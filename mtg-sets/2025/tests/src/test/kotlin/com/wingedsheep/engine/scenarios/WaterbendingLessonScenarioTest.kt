package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.support.answerForcedPayments
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.cards.WaterbendingLesson
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Waterbending Lesson ({3}{U}, Sorcery — Lesson):
 *   "Draw three cards. Then discard a card unless you waterbend {2}."
 *
 * The "unless you waterbend {2}" is an in-resolution waterbend payment gate
 * ([com.wingedsheep.sdk.dsl.Effects.UnlessYouWaterbend]): after drawing, the caster may pay a
 * waterbend {2} — mana and/or by tapping their untapped artifacts and creatures (each {1}, the
 * shared Ward—Waterbend tap-to-help path) — and if they decline or cannot pay, they discard a card.
 *
 * Pins:
 *  a) paying the waterbend {2} by tapping a creature and an artifact → no discard (hand +3), taps.
 *  b) paying the waterbend {2} with mana (auto-pay) → no discard (hand +3).
 *  c) can't pay (no mana, no tappable permanents) → discard a card (hand +2), no waterbend prompt.
 *  d) can pay but declines the waterbend prompt → discard a card (hand +2).
 */
class WaterbendingLessonScenarioTest : FunSpec({

    // A pure (non-creature) artifact, to prove an artifact can be tapped to help pay the waterbend.
    val trinket = card("Waterbending Test Trinket") {
        manaCost = "{1}"
        colorIdentity = ""
        typeLine = "Artifact"
        oracleText = ""
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WaterbendingLesson, trinket))
        return driver
    }

    test("paying the waterbend {2} by tapping a creature and an artifact avoids the discard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creature = driver.putCreatureOnBattlefield(caster, "Centaur Courser")
        val artifact = driver.putPermanentOnBattlefield(caster, "Waterbending Test Trinket")
        val taps = setOf(creature, artifact)

        // {3}{U} for the cast comes from the pool; the waterbend {2} is paid by the two taps.
        driver.giveMana(caster, Color.BLUE, 4)
        val lesson = driver.putCardInHand(caster, "Waterbending Lesson")
        driver.castSpell(caster, lesson)

        val handAtCast = driver.getHand(caster).size
        driver.bothPass() // resolve: draw three, then pause for the waterbend gate

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        // The gate offers every untapped artifact/creature the caster controls as a tap-to-help.
        decision.waterbendPermanents.map { it.entityId }.toSet() shouldBe taps

        driver.submitDecision(
            caster,
            ManaSourcesSelectedResponse(
                decisionId = decision.id,
                selectedSources = emptyList(),
                autoPay = false,
                waterbendPermanents = taps
            )
        )
        drain(driver)

        // Drew three, waterbend paid → no discard.
        driver.getHand(caster).size shouldBe handAtCast + 3
        taps.all { driver.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
    }

    test("paying the waterbend {2} with mana avoids the discard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Two untapped Islands on the battlefield cover the resolution-time waterbend {2}.
        val land1 = driver.putLandOnBattlefield(caster, "Island")
        val land2 = driver.putLandOnBattlefield(caster, "Island")

        driver.giveMana(caster, Color.BLUE, 4) // pool pays the {3}{U} cast
        val lesson = driver.putCardInHand(caster, "Waterbending Lesson")
        driver.castSpell(caster, lesson)

        val handAtCast = driver.getHand(caster).size
        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        driver.submitManaAutoPayOrDecline(caster, autoPay = true)
        drain(driver)

        driver.getHand(caster).size shouldBe handAtCast + 3
        setOf(land1, land2).all { driver.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
    }

    test("being unable to pay the waterbend {2} forces a discard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // No untapped mana sources and no tappable artifacts/creatures → the waterbend can't be
        // paid. The hand could hold a Spirit Guide, so the mana window still opens, with refusing
        // as its only answer, and the gate goes on to the discard.
        driver.giveMana(caster, Color.BLUE, 4)
        val lesson = driver.putCardInHand(caster, "Waterbending Lesson")
        driver.castSpell(caster, lesson)

        val handAtCast = driver.getHand(caster).size
        driver.bothPass()
        driver.answerForcedPayments()

        val discard = driver.pendingDecision
        discard.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(caster, listOf(discard.options.first()))
        drain(driver)

        // Drew three, discarded one → net +2.
        driver.getHand(caster).size shouldBe handAtCast + 2
    }

    test("declining the waterbend {2} forces a discard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // The caster CAN pay (two untapped Islands) but chooses to decline — still a discard.
        driver.putLandOnBattlefield(caster, "Island")
        driver.putLandOnBattlefield(caster, "Island")

        driver.giveMana(caster, Color.BLUE, 4)
        val lesson = driver.putCardInHand(caster, "Waterbending Lesson")
        driver.castSpell(caster, lesson)

        val handAtCast = driver.getHand(caster).size
        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        driver.submitManaAutoPayOrDecline(caster, autoPay = false) // decline

        val discard = driver.pendingDecision
        discard.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(caster, listOf(discard.options.first()))
        drain(driver)

        driver.getHand(caster).size shouldBe handAtCast + 2
    }
})

/** Drain any leftover stack/priority once all resolution decisions have been answered. */
private fun drain(driver: GameTestDriver) {
    var guard = 0
    while (driver.pendingDecision == null && driver.state.stack.isNotEmpty() && guard++ < 20) {
        driver.bothPass()
    }
}
