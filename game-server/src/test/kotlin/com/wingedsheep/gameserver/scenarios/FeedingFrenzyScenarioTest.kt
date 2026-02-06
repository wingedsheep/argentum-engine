package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Feeding Frenzy.
 *
 * Card reference:
 * - Feeding Frenzy (2B): Instant
 *   "Target creature gets -X/-X until end of turn, where X is the number of Zombies on the battlefield."
 */
class FeedingFrenzyScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Feeding Frenzy dynamic -X/-X") {
            test("gives -X/-X based on zombie count") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Feeding Frenzy")
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3 target
                    .withCardOnBattlefield(1, "Severed Legion") // Zombie
                    .withCardOnBattlefield(1, "Gluttonous Zombie") // Zombie
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giantId = game.findPermanent("Hill Giant")!!

                // Cast Feeding Frenzy targeting Hill Giant
                val castResult = game.castSpell(1, "Feeding Frenzy", giantId)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // 2 Zombies on the battlefield -> -2/-2
                // Hill Giant is 3/3, so after -2/-2 it should be 1/1
                val projected = stateProjector.project(game.state)
                withClue("Hill Giant should have power 1 (3 base - 2 from 2 Zombies)") {
                    projected.getPower(giantId) shouldBe 1
                }
                withClue("Hill Giant should have toughness 1 (3 base - 2 from 2 Zombies)") {
                    projected.getToughness(giantId) shouldBe 1
                }
            }

            test("kills creature when zombie count equals or exceeds toughness") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Feeding Frenzy")
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2 target
                    .withCardOnBattlefield(1, "Severed Legion") // Zombie
                    .withCardOnBattlefield(1, "Gluttonous Zombie") // Zombie
                    .withCardOnBattlefield(2, "Festering Goblin") // Zombie (opponent's)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!

                val castResult = game.castSpell(1, "Feeding Frenzy", bearsId)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // 3 Zombies -> -3/-3 on a 2/2 = dead
                withClue("Grizzly Bears should be dead (0 or less toughness)") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("gives -0/-0 when no zombies on battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Feeding Frenzy")
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3, no zombies
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giantId = game.findPermanent("Hill Giant")!!

                val castResult = game.castSpell(1, "Feeding Frenzy", giantId)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // 0 Zombies -> -0/-0, Hill Giant stays at 3/3
                val projected = stateProjector.project(game.state)
                withClue("Hill Giant should still be 3/3 with no Zombies") {
                    projected.getPower(giantId) shouldBe 3
                    projected.getToughness(giantId) shouldBe 3
                }
            }
        }
    }
}
