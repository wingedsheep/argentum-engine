package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.speed.SpeedService
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Speed
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Outpace Oblivion (DFT #139).
 *
 * Outpace Oblivion {2}{R} — Enchantment
 * Start your engines!
 * When this enchantment enters, it deals 5 damage to up to one target creature or planeswalker.
 * {2}, Sacrifice this enchantment: It deals 2 damage to each player who doesn't have max speed.
 *
 * The load-bearing claim is that "each player who doesn't have max speed" is evaluated **per
 * player**: the `ForEachPlayer(Each)` loop rebinds the controller each iteration, so the max-speed
 * gate asks about that player rather than about the ability's controller. Getting that wrong is
 * invisible in the common case — it only shows up when the two players' speeds differ, which is
 * exactly what these tests set up. The sacrifice is a cost, so the damage also has to survive its
 * own source having already left the battlefield.
 */
class OutpaceOblivionScenarioTest : ScenarioTestBase() {

    private val sacrificeAbilityId
        get() = cardRegistry.getCard("Outpace Oblivion")!!.script.activatedAbilities.single().id

    init {
        context("Outpace Oblivion") {

            test("start your engines! puts its controller on the board at speed 1") {
                val game = oblivionGame()

                withClue("CR 704.5z sets speed to 1 as soon as you control the permanent") {
                    game.state.speed(game.player1Id) shouldBe Speed.STARTING
                }
                withClue("each player tracks speed separately — the opponent still has none") {
                    game.state.speed(game.player2Id) shouldBe Speed.NONE
                }
            }

            test("the sacrifice ability skips the player at max speed and hits the one who isn't") {
                val game = oblivionGame()
                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first

                val controllerLifeBefore = game.getLifeTotal(1)
                val opponentLifeBefore = game.getLifeTotal(2)

                game.activateSacrifice()

                withClue("the controller has max speed, so they take nothing") {
                    game.getLifeTotal(1) shouldBe controllerLifeBefore
                }
                withClue("the opponent has no speed (0 ≠ 4), so they take 2") {
                    game.getLifeTotal(2) shouldBe opponentLifeBefore - 2
                }
            }

            test("it hits its own controller too when they are below max speed") {
                val game = oblivionGame()
                game.state = SpeedService.set(game.state, game.player2Id, Speed.MAX, "test").first

                val controllerLifeBefore = game.getLifeTotal(1)
                val opponentLifeBefore = game.getLifeTotal(2)

                game.activateSacrifice()

                withClue("the controller sits at speed 1, so the symmetric damage includes them") {
                    game.getLifeTotal(1) shouldBe controllerLifeBefore - 2
                }
                withClue("the opponent is at max speed and is spared") {
                    game.getLifeTotal(2) shouldBe opponentLifeBefore
                }
            }

            test("with nobody at max speed it hits both players") {
                val game = oblivionGame()
                val controllerLifeBefore = game.getLifeTotal(1)
                val opponentLifeBefore = game.getLifeTotal(2)

                game.activateSacrifice()

                game.getLifeTotal(1) shouldBe controllerLifeBefore - 2
                game.getLifeTotal(2) shouldBe opponentLifeBefore - 2
            }
        }
    }

    private fun TestGame.activateSacrifice() {
        val oblivion = findPermanent("Outpace Oblivion")!!
        val result = execute(
            ActivateAbility(playerId = player1Id, sourceId = oblivion, abilityId = sacrificeAbilityId),
        )
        withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
        resolveStack()
        withClue("the enchantment was sacrificed to pay the cost") {
            isOnBattlefield("Outpace Oblivion") shouldBe false
        }
    }

    /** Outpace Oblivion already on the battlefield, with two Mountains for the {2} activation. */
    private fun oblivionGame(): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Outpace Oblivion")
            .withLandsOnBattlefield(1, "Mountain", 2)
        repeat(6) {
            builder.withCardInLibrary(1, "Grizzly Bears")
            builder.withCardInLibrary(2, "Grizzly Bears")
        }
        // Built in the upkeep and advanced, so the CR 704.5z start-your-engines state-based action
        // has actually run by the time a test reads anyone's speed.
        val game = builder
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .build()
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        return game
    }
}
