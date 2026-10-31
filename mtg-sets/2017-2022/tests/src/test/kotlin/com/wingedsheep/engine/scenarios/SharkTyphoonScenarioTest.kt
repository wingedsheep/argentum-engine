package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Shark Typhoon — an X/X flying Shark per noncreature spell (X = that spell's mana value, counting
 * a chosen {X}), and Cycling {X}{1}{U} whose cycle trigger makes an X/X Shark from the X announced
 * for the cycling cost.
 */
class SharkTyphoonScenarioTest : ScenarioTestBase() {

    init {
        test("a noncreature spell with X makes a Shark sized by its mana value; a creature spell makes none") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Shark Typhoon")
                .withCardInHand(1, "Mind Spring") // {X}{U}{U}: MV 4 with X = 2
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 4)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castXSpell(1, "Mind Spring", xValue = 2)
            withClue("Mind Spring for X=2: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            val sharks = game.findPermanents("Shark Token")
            withClue("One Shark for the noncreature spell") { sharks.size shouldBe 1 }
            val projected = game.state.projectedState
            withClue("X = Mind Spring's mana value with X = 2, i.e. 4") {
                projected.getPower(sharks.single()) shouldBe 4
                projected.getToughness(sharks.single()) shouldBe 4
                projected.hasKeyword(sharks.single(), Keyword.FLYING) shouldBe true
            }

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            withClue("A creature spell doesn't trigger it") { game.findPermanents("Shark Token").size shouldBe 1 }
        }

        test("cycling for X=3 makes a 3/3 flying Shark and draws") {
            val game = cyclingGame(islands = 5)
            val handBefore = game.handSize(1)

            val cycle = game.cycleCard(1, "Shark Typhoon", xValue = 3)
            withClue("Cycling for X=3: ${cycle.error}") { cycle.error shouldBe null }
            game.resolveStack()

            val sharks = game.findPermanents("Shark Token")
            withClue("One Shark from the cycle trigger") { sharks.size shouldBe 1 }
            val projected = game.state.projectedState
            withClue("X = 3 announced for the cycling cost") {
                projected.getPower(sharks.single()) shouldBe 3
                projected.getToughness(sharks.single()) shouldBe 3
                projected.hasKeyword(sharks.single(), Keyword.FLYING) shouldBe true
            }
            withClue("Cycling drew a card replacing the discarded Typhoon") {
                game.handSize(1) shouldBe handBefore
                game.isInGraveyard(1, "Shark Typhoon") shouldBe true
            }
        }

        test("cycling for X=0 makes a 0/0 Shark that dies to state-based actions") {
            val game = cyclingGame(islands = 2)

            val cycle = game.cycleCard(1, "Shark Typhoon", xValue = 0)
            withClue("Cycling for X=0 on two Islands: ${cycle.error}") { cycle.error shouldBe null }
            game.resolveStack()

            withClue("The 0/0 Shark is put into the graveyard (ruling 2020-04-17)") {
                game.findPermanents("Shark Token").size shouldBe 0
            }
        }
    }

    private fun cyclingGame(islands: Int): TestGame =
        scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Shark Typhoon")
            .withLandsOnBattlefield(1, "Island", islands)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
}
