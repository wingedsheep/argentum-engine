package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SuspendCardFromHand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SuspendedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tsp.cards.AncestralVision
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ancestral Vision — Sorcery (no printed mana cost) — Rare (Time Spiral).
 *
 * "Suspend 4—{U} ... Target player draws three cards."
 *
 * Ancestral Vision is the primary coverage for the printed-Suspend *special action*
 * (CR 702.62a / CR 116.2f) — `SuspendCardFromHandHandler` / `SuspendEnumerator`. The exile-side
 * countdown and eventual free cast are the pre-existing, separately-tested
 * `Suspend.countdownAbility` machinery ([SuspendMechanicTest]); this file only needs to prove
 * the special action correctly hands off into that machinery, plus the CR 202.1b/118.6 fix that a
 * card with no mana cost can never be hard-cast.
 */
class AncestralVisionScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AncestralVision))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        return driver
    }

    fun timeCounters(driver: GameTestDriver, cardId: EntityId): Int =
        driver.state.getEntity(cardId)?.get<CountersComponent>()?.getCount(CounterType.TIME) ?: 0

    /** Advance to the owner's next upkeep and resolve the suspend countdown trigger there. */
    fun resolveNextOwnerUpkeep(driver: GameTestDriver, owner: EntityId) {
        do {
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            driver.passPriorityUntil(Step.UPKEEP)
        } while (driver.activePlayer != owner)
        driver.bothPass()
    }

    test("suspend: paying {U} exiles it with 4 time counters as a special action — no stack use") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(me, "Ancestral Vision")
        driver.giveMana(me, Color.BLUE, 1)

        driver.submitSuccess(SuspendCardFromHand(me, card))

        driver.getHand(me).contains(card) shouldBe false
        driver.getExile(me).contains(card) shouldBe true
        timeCounters(driver, card) shouldBe 4
        driver.state.getEntity(card)?.has<SuspendedComponent>() shouldBe true
        driver.state.stack.isEmpty() shouldBe true
        // Special action: priority stays with the player who took it.
        driver.state.priorityPlayerId shouldBe me
    }

    test("suspend: without {U} available, the special action is rejected") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(me, "Ancestral Vision")

        val result = driver.submit(SuspendCardFromHand(me, card))
        result.isSuccess shouldBe false
        driver.getHand(me).contains(card) shouldBe true
    }

    test("suspend: can only be taken at a time you could cast the card (sorcery speed here)") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(me, "Ancestral Vision")
        driver.giveMana(me, Color.BLUE, 1)

        // Ancestral Vision is a Sorcery with no flash — only legal at sorcery speed (own main
        // phase, empty stack). Put something else on the stack first: the caster keeps priority
        // right after casting, but the stack is no longer empty (CR 702.62c).
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(opponent)).isSuccess shouldBe true
        driver.state.stack.isEmpty() shouldBe false
        driver.state.priorityPlayerId shouldBe me

        val result = driver.submit(SuspendCardFromHand(me, card))
        result.isSuccess shouldBe false
        driver.getHand(me).contains(card) shouldBe true
    }

    test("Ancestral Vision has no mana cost and can never be cast normally (CR 202.1b/118.6)") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(me, "Ancestral Vision")

        // Never offered as a normal cast among the player's legal actions...
        val castOffered = driver.legalActions(me).any { action ->
            val a = action.action
            a is com.wingedsheep.engine.core.CastSpell && a.cardId == card
        }
        castOffered shouldBe false

        // ...and a direct engine-level cast attempt is rejected even if submitted anyway.
        driver.castSpell(me, card).isSuccess shouldBe false
    }

    test("full lifecycle: after 4 owner upkeeps, the owner may cast it for free and target player draws 3") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(me, "Ancestral Vision")
        driver.giveMana(me, Color.BLUE, 1)
        driver.submitSuccess(SuspendCardFromHand(me, card))
        timeCounters(driver, card) shouldBe 4

        repeat(3) {
            resolveNextOwnerUpkeep(driver, me)
        }
        timeCounters(driver, card) shouldBe 1
        driver.getExile(me).contains(card) shouldBe true

        val handSizeBefore = driver.getHand(me).size

        // Fourth owner upkeep: the last time counter is removed and the "may cast it for free"
        // trigger fires (CR 702.62a).
        resolveNextOwnerUpkeep(driver, me)
        driver.submitYesNo(me, true)
        // "Target player draws three cards" — the free cast still needs its target chosen.
        driver.submitTargetSelection(me, listOf(me))
        driver.bothPass()

        // The card was cast (not drawn) and resolved: three actual cards were drawn on top of
        // that, so hand size grew by exactly 3, and the card itself is gone (a sorcery, so it
        // went to the graveyard on resolution rather than staying in exile).
        driver.getExile(me).contains(card) shouldBe false
        driver.getHand(me).size shouldBe handSizeBefore + 3
        driver.state.getEntity(card)?.has<SuspendedComponent>() shouldBe false
    }

    test("the free cast can target the opponent instead of the caster") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(me, "Ancestral Vision")
        driver.giveMana(me, Color.BLUE, 1)
        driver.submitSuccess(SuspendCardFromHand(me, card))

        repeat(4) {
            resolveNextOwnerUpkeep(driver, me)
        }

        val myHandSizeBefore = driver.getHand(me).size
        val opponentHandSizeBefore = driver.getHand(opponent).size

        driver.submitYesNo(me, true)
        // "Target player draws three cards" is a real choice — the caster (who controls this
        // free cast, per CR 601.2c/2d — target selection is the caster's, not the target's) may
        // point it at either player. Choosing the opponent here proves the target isn't
        // hardcoded to self.
        driver.submitTargetSelection(me, listOf(opponent))
        driver.bothPass()

        driver.getHand(me).size shouldBe myHandSizeBefore
        driver.getHand(opponent).size shouldBe opponentHandSizeBefore + 3
    }

    test("declining the free cast at zero counters leaves it exiled and no longer suspended") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(me, "Ancestral Vision")
        driver.giveMana(me, Color.BLUE, 1)
        driver.submitSuccess(SuspendCardFromHand(me, card))

        repeat(4) {
            resolveNextOwnerUpkeep(driver, me)
        }
        timeCounters(driver, card) shouldBe 0

        // CR 702.62a: "you may play it" — declining is legal, and it remains exiled forever.
        driver.submitYesNo(me, false)
        driver.bothPass()

        driver.getExile(me).contains(card) shouldBe true
        // CR 702.62b: "suspended" requires >=1 time counter — with none left, it no longer
        // counts down or offers another free cast, even though it stays in exile permanently.
        timeCounters(driver, card) shouldBe 0
    }
})
