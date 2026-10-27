package com.wingedsheep.engine.multiplayer

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.combat.MustAttackPlayerThisTurnComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.encore
import com.wingedsheep.sdk.dsl.encoreAbility
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantAttackYou
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Encore (CR 702.141a): "[Cost], Exile this card from your graveyard: For each opponent, create a
 * token that's a copy of this card that attacks that opponent this turn if able. The tokens gain
 * haste. Sacrifice them at the beginning of the next end step. Activate only as a sorcery." — and
 * the player-directed attack requirement it is built on (`MarkMustAttackEffect.defender`).
 */
class EncoreTest : FunSpec({

    val encoreBear = card("Test Encore Bear") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        encore("{2}{R}")
    }

    val wall = card("Test Pacifism Field") {
        manaCost = "{W}"
        typeLine = "Enchantment"
        staticAbility { ability = CantAttackYou(GroupFilter.AllCreatures) }
    }

    val ability = encoreAbility(ManaCost.parse("{2}{R}"))

    fun driver(players: Int): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(encoreBear, wall))
        val ids = d.initMultiplayer(List(players) { Deck.of("Mountain" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d to ids
    }

    fun GameTestDriver.tokensOf(player: EntityId): List<EntityId> =
        getCreatures(player).filter { state.getEntity(it)?.has<TokenComponent>() == true }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (pendingDecision == null && stackSize > 0 && guard++ < 50) passPriority(priorityPlayer!!)
    }

    /** Put the card in [player]'s graveyard, pay for encore, activate it and resolve. */
    fun GameTestDriver.encore(player: EntityId): EntityId {
        val cardId = putCardInGraveyard(player, "Test Encore Bear")
        giveMana(player, Color.RED, 3)
        submit(ActivateAbility(player, cardId, ability.id)).error shouldBe null
        resolveStack()
        return cardId
    }

    /** Which opponent each token is required to attack. */
    fun GameTestDriver.requiredDefender(token: EntityId): Set<EntityId> =
        state.getEntity(token)?.get<MustAttackPlayerThisTurnComponent>()?.defenderIds.orEmpty()

    test("one hasty token copy per opponent under the activator's control; the card is exiled") {
        val (d, p) = driver(4)
        val (p1, p2, p3, p4) = p
        val cardId = d.encore(p1)

        withClue("exiling the card is part of the cost") { d.getExile(p1) shouldBe listOf(cardId) }
        val tokens = d.tokensOf(p1)
        tokens shouldHaveSize 3
        tokens.forEach { token ->
            d.getCardName(token) shouldBe "Test Encore Bear"
            d.state.getEntity(token)!!.get<ControllerComponent>()!!.playerId shouldBe p1
            withClue("the tokens gain haste") { d.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe true }
        }
        tokens.map { d.requiredDefender(it).single() } shouldContainExactlyInAnyOrder listOf(p2, p3, p4)
    }

    test("each token must attack its own opponent: omitting it or attacking another player is illegal") {
        val (d, p) = driver(3)
        val (p1, p2, p3) = p
        d.encore(p1)
        val tokens = d.tokensOf(p1)
        val toP2 = tokens.single { d.requiredDefender(it) == setOf(p2) }
        val toP3 = tokens.single { d.requiredDefender(it) == setOf(p3) }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        withClue("not attacking at all breaks the requirement") {
            d.declareAttackers(p1, emptyMap()).error shouldNotBe null
        }
        withClue("swapping the opponents breaks both requirements") {
            d.declareAttackers(p1, mapOf(toP2 to p3, toP3 to p2)).error shouldNotBe null
        }
        d.declareAttackers(p1, mapOf(toP2 to p2, toP3 to p3)).error shouldBe null
    }

    test("a token that can't attack its opponent is under no requirement (CR 508.1d)") {
        val (d, p) = driver(3)
        val (p1, p2, p3) = p
        d.putPermanentOnBattlefield(p2, "Test Pacifism Field")
        d.encore(p1)
        val toP2 = d.tokensOf(p1).single { d.requiredDefender(it) == setOf(p2) }
        val toP3 = d.tokensOf(p1).single { d.requiredDefender(it) == setOf(p3) }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        withClue("the token bound for p2 may stay home or attack p3") {
            d.declareAttackers(p1, mapOf(toP2 to p3, toP3 to p3)).error shouldBe null
        }
    }

    test("the tokens are sacrificed at the beginning of the next end step") {
        val (d, p) = driver(3)
        val (p1, _, _) = p
        d.encore(p1)
        val tokens = d.tokensOf(p1)
        tokens shouldHaveSize 2

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, tokens.associateWith { d.requiredDefender(it).single() }).error shouldBe null
        d.passPriorityUntil(Step.END)
        d.resolveStack()
        d.tokensOf(p1).shouldBeEmpty()
    }

    test("activate only as a sorcery: not offered outside the main phase") {
        val (d, p) = driver(2)
        val (p1, _) = p
        val cardId = d.putCardInGraveyard(p1, "Test Encore Bear")
        d.giveMana(p1, Color.RED, 3)
        val offered = { d.legalActions(p1).any { (it.action as? ActivateAbility)?.sourceId == cardId } }
        offered() shouldBe true

        d.passPriorityUntil(Step.BEGIN_COMBAT)
        d.giveMana(p1, Color.RED, 3)
        offered() shouldBe false
    }

    test("the directed mark ends in the cleanup step") {
        val (d, p) = driver(2)
        val (p1, p2) = p
        val bear = d.putCreatureOnBattlefield(p1, "Test Encore Bear")
        val effect = Effects.MarkMustAttackThisTurn(EffectTarget.Self, defender = EffectTarget.Controller)
        val result = d.services.effectExecutorRegistry.execute(
            d.state, effect, EffectContext(sourceId = bear, controllerId = p2)
        )
        d.replaceState(result.state)
        d.requiredDefender(bear) shouldBe setOf(p2)

        d.passPriorityUntil(Step.UPKEEP, p2)
        d.requiredDefender(bear) shouldBe emptySet()
    }
})
