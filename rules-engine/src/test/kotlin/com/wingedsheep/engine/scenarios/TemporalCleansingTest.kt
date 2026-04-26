package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.TemporalCleansing
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Temporal Cleansing.
 *
 * {3}{U} Sorcery (Convoke)
 * The owner of target nonland permanent puts it into their library second from
 * the top or on the bottom.
 *
 * Exercises:
 *  - the second-from-top branch of [com.wingedsheep.sdk.scripting.effects.PutOnLibraryPositionOfChoiceEffect]
 *  - reveal-tracking: the bounced card lands at a publicly known position, so it
 *    should be marked [RevealedToComponent] for every player so the library viewer
 *    shows it face-up.
 */
class TemporalCleansingTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + TemporalCleansing)
        return driver
    }

    test("owner chooses second from top — card lands at index 1 and is revealed to both players") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val cleansing = driver.putCardInHand(p1, "Temporal Cleansing")
        val courser = driver.putCreatureOnBattlefield(p2, "Centaur Courser")

        // Snapshot p2's pre-cast top card so we can assert it stays on top.
        val p2LibraryBefore = driver.state.getZone(ZoneKey(p2, Zone.LIBRARY))
        val originalTop = p2LibraryBefore.first()

        driver.giveMana(p1, Color.BLUE, 1)
        driver.giveColorlessMana(p1, 3)

        driver.submit(
            CastSpell(
                playerId = p1,
                cardId = cleansing,
                targets = listOf(ChosenTarget.Permanent(courser)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        val decision = driver.pendingDecision
        (decision is ChooseOptionDecision) shouldBe true
        decision as ChooseOptionDecision
        decision.playerId shouldBe p2
        decision.options shouldBe listOf("Second from top of library", "Bottom of library")

        driver.submit(SubmitDecision(p2, OptionChosenResponse(decision.id, 0))) // second from top

        driver.isPaused shouldBe false
        driver.findPermanent(p2, "Centaur Courser") shouldBe null

        val p2Library = driver.state.getZone(ZoneKey(p2, Zone.LIBRARY))
        p2Library.first() shouldBe originalTop
        p2Library[1] shouldBe courser

        // The placement was public (battlefield → known library position), so both players
        // should now see Centaur Courser face-up in p2's library.
        val revealed = driver.state.getEntity(courser)?.get<RevealedToComponent>()
        revealed shouldBe RevealedToComponent(setOf(p1, p2))
    }

    test("owner chooses bottom — card lands at the back of the library") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val cleansing = driver.putCardInHand(p1, "Temporal Cleansing")
        val courser = driver.putCreatureOnBattlefield(p2, "Centaur Courser")

        driver.giveMana(p1, Color.BLUE, 1)
        driver.giveColorlessMana(p1, 3)

        driver.submit(
            CastSpell(
                playerId = p1,
                cardId = cleansing,
                targets = listOf(ChosenTarget.Permanent(courser)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        val decision = driver.pendingDecision as ChooseOptionDecision
        driver.submit(SubmitDecision(p2, OptionChosenResponse(decision.id, 1))) // bottom

        val p2Library = driver.state.getZone(ZoneKey(p2, Zone.LIBRARY))
        p2Library.last() shouldBe courser

        val revealed = driver.state.getEntity(courser)?.get<RevealedToComponent>()
        revealed shouldBe RevealedToComponent(setOf(p1, p2))
    }
})
