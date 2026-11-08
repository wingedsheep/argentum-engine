package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Cavern-Hoard Dragon (LTC #31) — "This spell costs {X} less to cast, where X is the greatest
 * number of artifacts an opponent controls." and "Whenever this creature deals combat damage to a
 * player, you create a Treasure token for each artifact that player controls."
 *
 * The cost reduction is the first card to pair a self-cast `ReduceGenericBy` with a
 * `GreatestAmongPlayers` amount, and the Treasure count reads the player the Dragon hit.
 */
class CavernHoardDragonScenarioTest : ScenarioTestBase() {

    private fun castingSetup(opponentArtifacts: Int, mountains: Int): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Cavern-Hoard Dragon")
            .withLandsOnBattlefield(1, "Mountain", mountains)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(opponentArtifacts) { builder = builder.withCardOnBattlefield(2, "Sol Ring") }
        return builder.build()
    }

    private fun combatSetup(opponentArtifacts: Int): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Cavern-Hoard Dragon")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(opponentArtifacts) { builder = builder.withCardOnBattlefield(2, "Sol Ring") }
        repeat(5) { builder = builder.withCardInLibrary(1, "Island").withCardInLibrary(2, "Island") }
        return builder.build()
    }

    private fun TestGame.hitOpponent() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Cavern-Hoard Dragon" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        resolveStack()
    }

    init {
        context("Cavern-Hoard Dragon") {
            test("costs {X} less, X being the artifacts the opponent controls: three artifacts leave {4}{R}{R}") {
                val game = castingSetup(opponentArtifacts = 3, mountains = 6)

                game.castSpell(1, "Cavern-Hoard Dragon").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Cavern-Hoard Dragon") shouldBe true
            }

            test("the reduction only eats generic mana: seven or more artifacts leave {R}{R}") {
                val game = castingSetup(opponentArtifacts = 9, mountains = 2)

                game.castSpell(1, "Cavern-Hoard Dragon").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Cavern-Hoard Dragon") shouldBe true
            }

            test("with fewer artifacts the full remainder must still be paid") {
                val game = castingSetup(opponentArtifacts = 2, mountains = 6)

                withClue("{5}{R}{R} needs seven mana") {
                    game.castSpell(1, "Cavern-Hoard Dragon").error shouldNotBe null
                }
            }

            test("creates a Treasure for each artifact the damaged player controls") {
                val game = combatSetup(opponentArtifacts = 3)

                game.hitOpponent()

                game.findPermanents("Treasure").size shouldBe 3
                game.getLifeTotal(2) shouldBe 14
            }

            test("creates nothing when the damaged player controls no artifacts") {
                val game = combatSetup(opponentArtifacts = 0)

                game.hitOpponent()

                game.findPermanents("Treasure").size shouldBe 0
                game.getLifeTotal(2) shouldBe 14
            }
        }
    }
}
