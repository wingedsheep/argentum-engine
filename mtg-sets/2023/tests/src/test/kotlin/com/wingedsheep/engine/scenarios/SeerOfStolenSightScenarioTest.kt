package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Seer of Stolen Sight — "Whenever one or more artifacts and/or creatures you control are put into
 * a graveyard from the battlefield, surveil 1."
 *
 * Every surveil is answered by milling the looked-at card, so the number of cards that left the
 * library is the number of times the trigger resolved.
 */
class SeerOfStolenSightScenarioTest : ScenarioTestBase() {

    /** Resolve everything, answering each surveil by putting the card into the graveyard. */
    private fun TestGame.resolveMillingEachSurveil(): Int {
        var surveils = 0
        var guard = 0
        while (guard++ < 20) {
            val d = getPendingDecision()
            if (d is SelectCardsDecision) {
                surveils++
                selectCards(d.options).error shouldBe null
                continue
            }
            if (state.stack.isEmpty()) break
            resolveStack()
        }
        return surveils
    }

    private fun base() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Seer of Stolen Sight")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("a noncreature artifact you control put into a graveyard surveils 1") {
            val game = base()
                .withCardOnBattlefield(1, "Mind Stone")
                .withCardInHand(1, "Shatter")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .build()

            game.castSpell(1, "Shatter", game.findPermanent("Mind Stone")).error shouldBe null
            game.resolveMillingEachSurveil() shouldBe 1
            game.librarySize(1) shouldBe 2
        }

        test("a noncreature artifact token counts even though it ceases to exist") {
            val game = base()
                .withCardOnBattlefield(1, "Mind Stone", isToken = true)
                .withCardInHand(1, "Shatter")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .build()

            game.castSpell(1, "Shatter", game.findPermanent("Mind Stone")).error shouldBe null
            game.resolveMillingEachSurveil() shouldBe 1
        }

        test("a board wipe fires it once, and Seer sees the batch it dies in") {
            val game = base()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardInHand(1, "Wrath of God")
                .withLandsOnBattlefield(1, "Plains", 4)
                .build()

            game.castSpell(1, "Wrath of God").error shouldBe null
            withClue("three of your creatures died together — one batch, one surveil") {
                game.resolveMillingEachSurveil() shouldBe 1
            }
            game.isInGraveyard(1, "Seer of Stolen Sight") shouldBe true
        }

        test("an opponent's artifact doesn't count") {
            val game = base()
                .withCardOnBattlefield(2, "Mind Stone")
                .withCardInHand(1, "Shatter")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .build()

            game.castSpell(1, "Shatter", game.findPermanent("Mind Stone")).error shouldBe null
            game.resolveMillingEachSurveil() shouldBe 0
            game.librarySize(1) shouldBe 3
        }

        test("a land you control put into a graveyard doesn't count") {
            val game = base()
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInHand(1, "Stone Rain")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .build()

            game.castSpell(1, "Stone Rain", game.findPermanent("Forest")).error shouldBe null
            game.resolveMillingEachSurveil() shouldBe 0
        }
    }
}
