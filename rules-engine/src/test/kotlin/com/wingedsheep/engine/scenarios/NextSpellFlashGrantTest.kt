package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.FlashGrant
import com.wingedsheep.engine.state.components.player.FlashGrantsComponent
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The one-shot "the next sorcery spell you cast this turn can be cast as though it had flash" grant
 * — `Effects.GrantFlashToNextSpell`, a `GrantFlashToSpellsEffect` with `Duration.NextUse` (Quicken,
 * Savage Summoning).
 *
 * Pins the printed rulings (Quicken / Savage Summoning):
 *  - the grant applies to the next matching spell and is spent by it — even a spell cast at a time
 *    it could have been cast anyway;
 *  - a spell that doesn't match leaves the grant waiting;
 *  - several such grants are all spent by the very next matching spell;
 * plus: whole-turn grants beside it are never spent, and an unspent grant lapses at end of turn.
 */
class NextSpellFlashGrantTest : FunSpec({

    val testSorcery = CardDefinition.sorcery(
        name = "Test Sorcery",
        manaCost = ManaCost.parse("{R}"),
        oracleText = "Draw a card.",
        script = CardScript.spell(effect = Effects.DrawCards(1))
    )
    val testCreature = CardDefinition.creature(
        name = "Test Beast",
        manaCost = ManaCost.parse("{R}"),
        subtypes = setOf(Subtype("Beast")),
        power = 1,
        toughness = 1
    )
    val quickenAnalogue = CardDefinition.instant(
        name = "Test Quickener",
        manaCost = ManaCost.parse("{U}"),
        oracleText = "The next sorcery spell you cast this turn can be cast as though it had flash.",
        script = CardScript.spell(effect = Effects.GrantFlashToNextSpell(GameObjectFilter.Sorcery))
    )

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(testSorcery, testCreature, quickenAnalogue))
        it.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
    }

    fun GameTestDriver.cast(player: com.wingedsheep.sdk.model.EntityId, card: com.wingedsheep.sdk.model.EntityId) =
        submit(CastSpell(playerId = player, cardId = card, paymentStrategy = PaymentStrategy.FromPool))

    fun GameTestDriver.quicken(player: com.wingedsheep.sdk.model.EntityId) {
        val q = putCardInHand(player, "Test Quickener")
        giveMana(player, Color.BLUE, 1)
        submitSuccess(CastSpell(playerId = player, cardId = q, paymentStrategy = PaymentStrategy.FromPool))
        bothPass()
    }

    fun GameTestDriver.grants(player: com.wingedsheep.sdk.model.EntityId) =
        state.getEntity(player)?.get<FlashGrantsComponent>()?.grants ?: emptyList()

    test("resolving the effect records a one-shot end-of-turn grant") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.quicken(p1)
        d.grants(p1) shouldBe listOf(FlashGrant(GameObjectFilter.Sorcery, PlayerEffectRemoval.EndOfTurn, nextSpellOnly = true))
    }

    test("the next sorcery may be cast at instant speed, and only that one") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.END)
        d.quicken(p1)
        val first = d.putCardInHand(p1, "Test Sorcery")
        val second = d.putCardInHand(p1, "Test Sorcery")
        d.giveMana(p1, Color.RED, 2)

        d.cast(p1, first).outcome shouldBe Outcome.Done
        d.grants(p1) shouldBe emptyList()
        d.bothPass()
        d.cast(p1, second).outcome shouldNotBe Outcome.Done
    }

    test("a sorcery cast at sorcery speed still spends the grant (ruling)") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.quicken(p1)
        val mainPhase = d.putCardInHand(p1, "Test Sorcery")
        d.giveMana(p1, Color.RED, 1)
        d.cast(p1, mainPhase).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.getEntity(p1)?.get<FlashGrantsComponent>().shouldBeNull()

        val later = d.putCardInHand(p1, "Test Sorcery")
        d.passPriorityUntil(Step.END)
        d.giveMana(p1, Color.RED, 1)
        d.cast(p1, later).outcome shouldNotBe Outcome.Done
    }

    test("a non-matching spell leaves the grant waiting") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.quicken(p1)
        val beast = d.putCardInHand(p1, "Test Beast")
        d.giveMana(p1, Color.RED, 1)
        d.cast(p1, beast).outcome shouldBe Outcome.Done
        d.bothPass()
        d.grants(p1).size shouldBe 1

        val sorcery = d.putCardInHand(p1, "Test Sorcery")
        d.passPriorityUntil(Step.END)
        d.giveMana(p1, Color.RED, 1)
        d.cast(p1, sorcery).outcome shouldBe Outcome.Done
    }

    test("two one-shot grants are both spent by the very next sorcery (ruling)") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.END)
        d.quicken(p1)
        d.quicken(p1)
        d.grants(p1).size shouldBe 2
        val first = d.putCardInHand(p1, "Test Sorcery")
        val second = d.putCardInHand(p1, "Test Sorcery")
        d.giveMana(p1, Color.RED, 2)
        d.cast(p1, first).outcome shouldBe Outcome.Done
        d.bothPass()
        d.grants(p1) shouldBe emptyList()
        d.cast(p1, second).outcome shouldNotBe Outcome.Done
    }

    test("a whole-turn grant beside it is not spent") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.END)
        val lasting = FlashGrant(GameObjectFilter.Sorcery, PlayerEffectRemoval.EndOfTurn)
        d.replaceState(d.state.updateEntity(p1) { it.with(FlashGrantsComponent(listOf(lasting))) })
        d.quicken(p1)
        val first = d.putCardInHand(p1, "Test Sorcery")
        d.giveMana(p1, Color.RED, 1)
        d.cast(p1, first).outcome shouldBe Outcome.Done
        d.grants(p1) shouldBe listOf(lasting)
    }

    test("an opponent's sorcery neither gains flash nor spends the grant") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.player2
        d.passPriorityUntil(Step.END)
        d.quicken(p1)
        val theirs = d.putCardInHand(p2, "Test Sorcery")
        d.passPriority(p1)
        d.giveMana(p2, Color.RED, 1)
        d.cast(p2, theirs).outcome shouldNotBe Outcome.Done
        d.grants(p1).size shouldBe 1
    }

    test("an unspent grant lapses at end of turn") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.quicken(p1)
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldNotBe p1
        d.state.getEntity(p1)?.get<FlashGrantsComponent>().shouldBeNull()
    }

    test("the effect reads as the printed one-shot sentence") {
        Effects.GrantFlashToNextSpell(GameObjectFilter.Sorcery).description shouldBe
            "The next sorcery spell you cast this turn can be cast as though it had flash"
    }
})
