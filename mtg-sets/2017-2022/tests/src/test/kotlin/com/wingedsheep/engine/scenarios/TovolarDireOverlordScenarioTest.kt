package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.DayNight
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Tovolar, Dire Overlord // Tovolar, the Midnight Scourge (MID #246).
 *
 * The upkeep trigger is an intervening "if" (three or more Wolves and/or Werewolves) that makes it
 * night and then lets you transform any number of Human Werewolves. Becoming night flips Tovolar
 * itself through daybound first, so the only choice left is an older werewolf (Villagers of Estwald).
 *
 * Player 2 casts a spell on their turn so the untap-step day/night check keeps it day and Villagers'
 * own "no spells last turn" transform trigger stays quiet — the night and the flip come from Tovolar.
 */
class TovolarDireOverlordScenarioTest : ScenarioTestBase() {

    private fun setUp(withWolf: Boolean): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Tovolar, Dire Overlord")
            .withCardOnBattlefield(1, "Villagers of Estwald")
            .withCardInHand(2, "Grizzly Bears")
            .withLandsOnBattlefield(2, "Forest", 2)
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (withWolf) builder.withCardOnBattlefield(1, "Bounding Wolf")
        val game = builder.build()
        game.state = game.state.copy(dayNight = DayNight.DAY)

        game.castSpell(2, "Grizzly Bears").error shouldBe null
        game.resolveStack()
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.state.activePlayerId shouldBe game.player1Id
        return game
    }

    init {
        context("Tovolar, Dire Overlord — upkeep trigger") {

            test("with three Wolves/Werewolves it becomes night and an older Human Werewolf can be transformed") {
                val game = setUp(withWolf = true)
                game.resolveStack()

                withClue("it became night and Tovolar flipped through daybound") {
                    game.state.dayNight shouldBe DayNight.NIGHT
                    game.findPermanent("Tovolar, the Midnight Scourge") shouldNotBe null
                }

                val villagers = game.findPermanent("Villagers of Estwald")!!
                val decision = game.getPendingDecision() as SelectCardsDecision
                withClue("only the still-Human Werewolf is offered") {
                    decision.options shouldBe listOf(villagers)
                }
                game.selectCards(listOf(villagers)).error shouldBe null
                game.resolveStack()

                withClue("Villagers of Estwald transformed into Howlpack of Estwald") {
                    game.findPermanent("Howlpack of Estwald") shouldNotBe null
                    game.findPermanent("Villagers of Estwald") shouldBe null
                }
            }

            test("choosing none leaves the older werewolf untransformed") {
                val game = setUp(withWolf = true)
                game.resolveStack()

                game.state.dayNight shouldBe DayNight.NIGHT
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                game.findPermanent("Villagers of Estwald") shouldNotBe null
            }

            test("with only two Wolves/Werewolves the ability doesn't trigger") {
                val game = setUp(withWolf = false)
                game.resolveStack()

                withClue("still day, nothing transformed") {
                    game.state.dayNight shouldBe DayNight.DAY
                    game.getPendingDecision() shouldBe null
                    game.findPermanent("Tovolar, Dire Overlord") shouldNotBe null
                    game.findPermanent("Villagers of Estwald") shouldNotBe null
                }
            }
        }
    }
}
