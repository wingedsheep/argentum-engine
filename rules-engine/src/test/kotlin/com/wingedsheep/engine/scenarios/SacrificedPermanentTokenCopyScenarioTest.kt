package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * `CreateTokenCopyOfTarget(EffectTarget.SacrificedAsCost(…))` — a token copy of a permanent that was
 * sacrificed, made from the copiable values frozen as it was sacrificed (CR 608.2h, CR 707.2):
 *
 *  - in a reflexive trigger (CR 603.12): the action sacrifices, and the separate "when you do"
 *    ability still names the sacrificed object after its stack round-trip;
 *  - a sacrificed **token** has ceased to exist (CR 704.5d) and is still copied;
 *  - an Aura copy attached to a creature it can't legally enchant isn't created (CR 303.4i), while
 *    the non-targeting attachment ignores shroud;
 *  - a permanent sacrificed to pay an activated ability's cost is copied the same way.
 */
class SacrificedPermanentTokenCopyScenarioTest : ScenarioTestBase() {

    private val echoBard = card("Sacrifice Echo Bard") {
        manaCost = "{2}"
        typeLine = "Creature — Human Bard"
        power = 1
        toughness = 5
        triggeredAbility {
            trigger = Triggers.you.attacks()
            effect = Effects.ReflexiveTrigger(
                action = Effects.SacrificeOwn(GameObjectFilter.Enchantment.withSubtype("Aura").attachedToSource()),
                reflexiveEffect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.youControl().attacking(), excludeSelf = true),
                    Effects.CreateTokenCopyOfTarget(EffectTarget.SacrificedAsCost(0), attachedTo = EffectTarget.IterationEntity)
                ),
                optional = true
            )
        }
    }

    private val wardedKnight = card("Enchantment-Warded Knight") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human Knight"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.Protection(ProtectionScope.CardType("Enchantment")))
    }

    private val echoSmith = card("Sacrifice Echo Smith") {
        manaCost = "{2}"
        typeLine = "Creature — Human Artificer"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Sacrifice(GameObjectFilter.Creature)
            effect = Effects.CreateTokenCopyOfTarget(EffectTarget.SacrificedAsCost(0))
        }
    }

    init {
        cardRegistry.register(listOf(echoBard, wardedKnight, echoSmith))
    }

    private fun TestGame.named(name: String): List<EntityId> =
        state.getBattlefield().filter { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.hostOf(id: EntityId): EntityId? = state.getEntity(id)?.get<AttachedToComponent>()?.targetId

    /** Answer every prompt (yes to the may, first option for a choice) until the stack is empty. */
    private fun TestGame.resolveSayingYes() {
        var guard = 0
        while (guard++ < 30 && (state.stack.isNotEmpty() || state.pendingDecision != null)) {
            when (val decision = state.pendingDecision) {
                null -> resolveStack()
                is YesNoDecision -> submitDecision(YesNoResponse(decision.id, true)).error shouldBe null
                is SelectCardsDecision -> selectCards(decision.options.take(decision.minSelections.coerceAtLeast(1))).error shouldBe null
                is SelectManaSourcesDecision ->
                    submitDecision(ManaSourcesSelectedResponse(decision.id, emptyList(), true)).error shouldBe null
                else -> error("unexpected decision $decision")
            }
        }
    }

    private fun attackWith(game: TestGame, vararg names: String) {
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(names.associateWith { 2 }).error shouldBe null
        game.resolveSayingYes()
    }

    init {
        test("a sacrificed Aura is copied onto each other attacker by the reflexive trigger") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sacrifice Echo Bard")
                .withCardAttachedTo(1, "Holy Strength", "Sacrifice Echo Bard")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            attackWith(game, "Sacrifice Echo Bard", "Grizzly Bears", "Hill Giant")

            val copies = game.named("Holy Strength")
            withClue("one token per other attacker, none on the bard") {
                copies.map { game.hostOf(it) } shouldContainExactlyInAnyOrder
                    listOf(game.findPermanent("Grizzly Bears"), game.findPermanent("Hill Giant"))
                copies.all { game.state.getEntity(it)!!.has<TokenComponent>() } shouldBe true
            }
            withClue("the original was sacrificed") { game.isInGraveyard(1, "Holy Strength") shouldBe true }
            game.state.projectedState.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 3
        }

        test("a sacrificed Aura token has ceased to exist and is still copied (CR 704.5d, CR 608.2h)") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sacrifice Echo Bard")
                .withCardAttachedTo(1, "Holy Strength", "Sacrifice Echo Bard", isToken = true)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val original = game.named("Holy Strength").single()

            attackWith(game, "Sacrifice Echo Bard", "Grizzly Bears")

            withClue("the sacrificed token is gone") { game.state.getEntity(original) shouldBe null }
            val copy = game.named("Holy Strength").single()
            game.hostOf(copy) shouldBe game.findPermanent("Grizzly Bears")
        }

        for (chosenIsToken in listOf(false, true)) {
            test("with two Auras the paused sacrifice choice reaches the reflexive trigger (token=$chosenIsToken)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sacrifice Echo Bard")
                    .withCardAttachedTo(1, "Holy Strength", "Sacrifice Echo Bard")
                    .withCardAttachedTo(1, "Unholy Strength", "Sacrifice Echo Bard", isToken = chosenIsToken)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val unholy = game.named("Unholy Strength").single()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Sacrifice Echo Bard" to 2, "Grizzly Bears" to 2)).error shouldBe null
                var chose = false
                var guard = 0
                while (guard++ < 30 && (game.state.stack.isNotEmpty() || game.state.pendingDecision != null)) {
                    when (val decision = game.state.pendingDecision) {
                        null -> game.resolveStack()
                        is YesNoDecision -> game.submitDecision(YesNoResponse(decision.id, true)).error shouldBe null
                        is SelectCardsDecision -> {
                            chose = true
                            game.selectCards(listOf(unholy)).error shouldBe null
                        }
                        else -> error("unexpected decision $decision")
                    }
                }

                withClue("two candidates, so the sacrifice paused for a choice") { chose shouldBe true }
                val bears = game.findPermanent("Grizzly Bears")
                withClue("the copy is of the chosen Aura, on the other attacker") {
                    game.named("Unholy Strength").filter { it != unholy }.map { game.hostOf(it) } shouldBe listOf(bears)
                    game.named("Holy Strength").map { game.hostOf(it) } shouldBe listOf(game.findPermanent("Sacrifice Echo Bard"))
                }
            }
        }

        test("no token for a creature with protection from enchantments; shroud doesn't stop one (CR 303.4i)") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sacrifice Echo Bard")
                .withCardAttachedTo(1, "Holy Strength", "Sacrifice Echo Bard")
                .withCardOnBattlefield(1, "Enchantment-Warded Knight")
                .withCardOnBattlefield(1, "Blurred Mongoose")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            attackWith(game, "Sacrifice Echo Bard", "Enchantment-Warded Knight", "Blurred Mongoose")

            val copies = game.named("Holy Strength")
            copies.map { game.hostOf(it) } shouldBe listOf(game.findPermanent("Blurred Mongoose"))
            withClue("the knight never got one") {
                game.state.getBattlefield().filter { game.hostOf(it) == game.findPermanent("Enchantment-Warded Knight") }
                    .shouldBeEmpty()
            }
        }

        test("a creature token sacrificed to pay an activation cost is copied from its frozen values") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Sacrifice Echo Smith")
                .withCardOnBattlefield(1, "Hill Giant", isToken = true)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val smith = game.findPermanent("Sacrifice Echo Smith")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = smith,
                    abilityId = echoSmith.activatedAbilities.first().id,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(giant)),
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("the sacrificed token ceased to exist") { game.state.getEntity(giant) shouldBe null }
            val copy = game.named("Hill Giant").single()
            game.state.projectedState.getPower(copy) shouldBe 3
        }
    }
}
