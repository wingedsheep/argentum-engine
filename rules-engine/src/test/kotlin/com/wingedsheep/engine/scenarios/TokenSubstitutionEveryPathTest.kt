package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReplaceTokenCreationWithAttachedCopy
import com.wingedsheep.sdk.scripting.ReplaceTokenCreationWithToken
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A [ReplaceTokenCreationWithToken] replaces every token-creation event, whatever instruction
 * creates the tokens ("if one or more tokens would be created under your control, that many 3/3
 * colorless Illusion artifact creature tokens are created instead" — Holo-Engineer). Pins the
 * token paths that don't build their tokens from a `CreateTokenEffect`: token copies of the
 * source, of a chosen permanent and of the equipped creature, Aura tokens (Roles), and tokens
 * minted from a random card definition (Momir).
 *
 * A substitution that asks (Jinnie Fay) re-runs the creating effect after the answer; on the
 * chosen-permanent path the permanent already chosen is remembered, not asked for again.
 */
class TokenSubstitutionEveryPathTest : FunSpec({

    val illusions = card("Test Illusion Maker") {
        typeLine = "Enchantment"
        replacementEffect(
            ReplaceTokenCreationWithToken(
                tokens = listOf(Effects.CreateToken(3, 3, emptySet(), setOf("Illusion"), artifactToken = true)),
                appliesTo = EventPattern.TokenCreationEvent(controller = Player.You)
            )
        )
    }
    val catOrDog = card("Test Cat Or Dog") {
        typeLine = "Enchantment"
        replacementEffect(
            ReplaceTokenCreationWithToken(
                tokens = listOf(
                    Effects.CreateToken(2, 2, setOf(Color.GREEN), setOf("Cat"), setOf(Keyword.HASTE)),
                    Effects.CreateToken(3, 1, setOf(Color.GREEN), setOf("Dog"), setOf(Keyword.VIGILANCE))
                ),
                appliesTo = EventPattern.TokenCreationEvent(controller = Player.You),
                optional = true
            )
        )
    }
    val helm = card("Test Copy Helm") {
        typeLine = "Artifact — Equipment"
    }
    val crown = card("Test Copy Crown") {
        typeLine = "Artifact — Equipment"
        replacementEffect(ReplaceTokenCreationWithAttachedCopy(attachmentVerb = "equipped"))
    }

    fun driver(vararg permanents: String): GameTestDriver = GameTestDriver().also { d ->
        d.registerCards(TestCards.all + listOf(illusions, catOrDog, helm, crown, PredefinedTokens.MonsterRole, PredefinedTokens.Treasure))
        d.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        permanents.forEach { d.putPermanentOnBattlefield(d.player1, it) }
    }

    /** Resolve [effect] for player 1 from [sourceId] and adopt the resulting (possibly paused) state. */
    fun GameTestDriver.run(effect: Effect, sourceId: EntityId? = null) {
        val result = services.effectExecutorRegistry.execute(
            state, effect, EffectContext(sourceId = sourceId, controllerId = player1)
        )
        replaceState(result.state)
    }

    fun GameState.tokenNames(player: EntityId): List<String> = getBattlefield().mapNotNull { id ->
        val c = getEntity(id) ?: return@mapNotNull null
        if (!c.has<TokenComponent>() || c.get<ControllerComponent>()?.playerId != player) return@mapNotNull null
        // A copy keeps the copied card's name; a substitute is named for its creature type.
        c.get<CardComponent>()?.name?.takeIf { it == "Savannah Lions" }
            ?: listOf("Illusion", "Cat", "Dog").firstOrNull { projectedState.hasSubtype(id, it) }
    }

    test("a token copy of the source is replaced") {
        val d = driver(illusions.name)
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        d.run(Effects.CreateTokenCopyOfSelf(count = 2), sourceId = lions)
        d.state.tokenNames(d.player1) shouldBe listOf("Illusion", "Illusion")
    }

    test("a token copy of the equipped creature is replaced") {
        val d = driver(illusions.name)
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        val equipment = d.putPermanentOnBattlefield(d.player1, helm.name)
        d.replaceState(d.state.updateEntity(equipment) { it.with(AttachedToComponent(lions)) })
        d.run(Effects.CreateTokenCopyOfEquippedCreature(grantHaste = true), sourceId = equipment)
        val token = d.state.getBattlefield().single { d.state.getEntity(it)?.has<TokenComponent>() == true }
        d.state.tokenNames(d.player1) shouldBe listOf("Illusion")
        withClue("\"that token gains haste\" still reaches the substitute") {
            d.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe true
        }
    }

    test("a Role token is replaced and nothing is attached") {
        val d = driver(illusions.name)
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        d.run(Effects.CreateAuraToken("Monster Role", EffectTarget.Self), sourceId = lions)
        d.state.tokenNames(d.player1) shouldBe listOf("Illusion")
        d.state.getBattlefield().none { d.state.getEntity(it)?.get<AttachedToComponent>()?.targetId == lions } shouldBe true
    }

    test("a minted Momir token is replaced") {
        val d = driver(illusions.name)
        d.replaceState(d.state.copy(format = Format.MomirBasic(eligibleCreatureNames = listOf("Savannah Lions"))))
        d.run(Effects.CreateRandomCreatureTokenWithManaValue(DynamicAmount.Fixed(1)))
        d.state.tokenNames(d.player1) shouldBe listOf("Illusion")
    }

    test("a token copy of a chosen permanent is replaced") {
        val d = driver(illusions.name)
        d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        d.run(Effects.CreateTokenCopyOfChosenPermanent(GameObjectFilter.Creature))
        d.state.tokenNames(d.player1) shouldBe listOf("Illusion")
    }

    test("an asking substitution on a chosen copy doesn't ask for the permanent again") {
        val d = driver(catOrDog.name)
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.run(Effects.CreateTokenCopyOfChosenPermanent(GameObjectFilter.Creature))
        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(d.player1, listOf(lions)).error shouldBe null
        val ask = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        d.submitDecision(d.player1, OptionChosenResponse(ask.id, 0)).error shouldBe null
        withClue("the re-run after the answer remembers the chosen permanent (was ${pick.id})") {
            d.pendingDecision shouldBe null
        }
        d.state.tokenNames(d.player1) shouldBe listOf("Cat")
    }

    test("declining an asking substitution creates the copy of the chosen permanent") {
        val d = driver(catOrDog.name)
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        d.run(Effects.CreateTokenCopyOfChosenPermanent(GameObjectFilter.Creature))
        d.submitCardSelection(d.player1, listOf(lions)).error shouldBe null
        val ask = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        d.submitDecision(d.player1, OptionChosenResponse(ask.id, ask.options.lastIndex)).error shouldBe null
        d.pendingDecision shouldBe null
        d.state.tokenNames(d.player1) shouldBe listOf("Savannah Lions")
    }

    test("an opponent's substitution leaves your token copies alone") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player2, illusions.name)
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        d.run(Effects.CreateTokenCopyOfSelf(), sourceId = lions)
        d.state.tokenNames(d.player1) shouldBe listOf("Savannah Lions")
    }
    /** Put [equipment] on the battlefield attached to [creature]. */
    fun GameTestDriver.equip(equipment: String, creature: EntityId): EntityId =
        putPermanentOnBattlefield(player1, equipment).also { id ->
            replaceState(state.updateEntity(id) { it.with(AttachedToComponent(creature)) })
        }

    fun GameTestDriver.choose(option: Int) {
        val ask = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(player1, OptionChosenResponse(ask.id, option)).error shouldBe null
    }

    test("copies an accepted Mirrormind-style offer makes are themselves substituted") {
        val d = driver(illusions.name)
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        d.equip(crown.name, lions)
        d.run(Effects.CreateTreasure())
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(d.player1, true).error shouldBe null
        d.pendingDecision shouldBe null
        d.state.tokenNames(d.player1) shouldBe listOf("Illusion")
    }

    test("an asking substitution after an accepted offer re-runs without a second offer") {
        val d = driver(catOrDog.name)
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        d.equip(crown.name, lions)
        d.run(Effects.CreateTreasure())
        d.submitYesNo(d.player1, true).error shouldBe null
        d.choose(0)
        d.pendingDecision shouldBe null
        d.state.tokenNames(d.player1) shouldBe listOf("Cat")
    }

    test("declining an asking substitution after an accepted offer makes the copies") {
        val d = driver(catOrDog.name)
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        d.equip(crown.name, lions)
        d.run(Effects.CreateTreasure())
        d.submitYesNo(d.player1, true).error shouldBe null
        val ask = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        d.choose(ask.options.lastIndex)
        d.pendingDecision shouldBe null
        d.state.tokenNames(d.player1) shouldBe listOf("Savannah Lions")
    }

    test("an asking substitution on an equipped copy still grants haste to the substitute") {
        val d = driver(catOrDog.name)
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        val equipment = d.equip(helm.name, lions)
        d.run(Effects.CreateTokenCopyOfEquippedCreature(grantHaste = true), sourceId = equipment)
        d.choose(1)
        val token = d.state.getBattlefield().single { d.state.getEntity(it)?.has<TokenComponent>() == true }
        d.state.tokenNames(d.player1) shouldBe listOf("Dog")
        d.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe true
    }

    test("declining an asking substitution on a Momir mint keeps the creature first drawn") {
        val d = driver()
        val jinnie = d.putPermanentOnBattlefield(d.player1, catOrDog.name)
        d.replaceState(d.state.copy(format = Format.MomirBasic(eligibleCreatureNames = listOf("Goblin Guide", "Savannah Lions"))))
        val before = d.state
        fun minted(): String = d.state.getBattlefield().single { d.state.getEntity(it)?.has<TokenComponent>() == true }
            .let { d.state.getEntity(it)!!.get<CardComponent>()!!.name }

        d.run(Effects.CreateRandomCreatureTokenWithManaValue(DynamicAmount.Fixed(1)))
        val ask = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        d.choose(ask.options.lastIndex)
        val afterAnswer = minted()

        d.replaceState(before)
        d.moveToGraveyard(jinnie)
        d.run(Effects.CreateRandomCreatureTokenWithManaValue(DynamicAmount.Fixed(1)))
        withClue("the re-run after the answer draws from the seed the question paused on") {
            afterAnswer shouldBe minted()
        }
    }
})
