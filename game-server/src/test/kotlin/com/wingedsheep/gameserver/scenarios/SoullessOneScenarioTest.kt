package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Soulless One.
 *
 * Card reference:
 * - Soulless One (3B): *|* Creature — Zombie Avatar
 *   Trample
 *   Soulless One's power and toughness are each equal to the number of Zombies on the battlefield
 *   plus the number of Zombie cards in all graveyards.
 */
class SoullessOneScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Soulless One CDA power/toughness") {
            test("counts Zombies on the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Soulless One")
                    .withCardOnBattlefield(1, "Festering Goblin") // Zombie
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val soullessOne = game.findPermanent("Soulless One")!!

                // 2 Zombies on battlefield (Soulless One + Festering Goblin)
                val projected = stateProjector.project(game.state)
                withClue("Soulless One P/T should be 2/2 with 2 Zombies on battlefield") {
                    projected.getPower(soullessOne) shouldBe 2
                    projected.getToughness(soullessOne) shouldBe 2
                }
            }

            test("counts Zombie cards in all graveyards") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Soulless One") // 1 Zombie on battlefield
                    .withCardInGraveyard(1, "Festering Goblin") // Zombie in player's graveyard
                    .withCardInGraveyard(2, "Nantuko Husk") // Zombie in opponent's graveyard
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val soullessOne = game.findPermanent("Soulless One")!!

                // 1 Zombie on battlefield + 2 Zombie cards in graveyards = 3
                val projected = stateProjector.project(game.state)
                withClue("Soulless One should count Zombies on battlefield and in all graveyards") {
                    projected.getPower(soullessOne) shouldBe 3
                    projected.getToughness(soullessOne) shouldBe 3
                }
            }

            test("counts opponent's battlefield Zombies too") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Soulless One")
                    .withCardOnBattlefield(2, "Gluttonous Zombie") // Opponent's Zombie
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val soullessOne = game.findPermanent("Soulless One")!!

                // 2 Zombies on battlefield: Soulless One + Gluttonous Zombie
                val projected = stateProjector.project(game.state)
                withClue("Soulless One should count opponent's Zombies on battlefield") {
                    projected.getPower(soullessOne) shouldBe 2
                    projected.getToughness(soullessOne) shouldBe 2
                }
            }
        }
    }
}
