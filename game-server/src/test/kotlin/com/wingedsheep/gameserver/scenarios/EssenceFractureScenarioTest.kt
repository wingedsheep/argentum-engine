package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Essence Fracture.
 *
 * Card: Essence Fracture
 * {3}{U}{U}
 * Sorcery
 * Return two target creatures to their owners' hands.
 * Cycling {2}{U}
 */
class EssenceFractureScenarioTest : ScenarioTestBase() {

    init {
        context("Essence Fracture bounces two creatures") {
            test("returns two target creatures to their owners' hands") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Essence Fracture")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardOnBattlefield(1, "Grizzly Bears")    // 2/2 on caster's side
                    .withCardOnBattlefield(2, "Hill Giant")       // 3/3 on opponent's side
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                // Cast Essence Fracture targeting both creatures
                val castResult = game.execute(
                    CastSpell(
                        game.player1Id,
                        game.findCardsInHand(1, "Essence Fracture").first(),
                        listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant))
                    )
                )
                withClue("Essence Fracture should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // Both creatures should be gone from battlefield
                withClue("Grizzly Bears should not be on battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
                withClue("Hill Giant should not be on battlefield") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }

                // Each creature should be in its owner's hand
                withClue("Grizzly Bears should be in player 1's hand") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
                withClue("Hill Giant should be in player 2's hand") {
                    game.isInHand(2, "Hill Giant") shouldBe true
                }
            }

            test("returns two creatures controlled by the same player") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Essence Fracture")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                val castResult = game.execute(
                    CastSpell(
                        game.player1Id,
                        game.findCardsInHand(1, "Essence Fracture").first(),
                        listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant))
                    )
                )
                withClue("Should cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Grizzly Bears should not be on battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
                withClue("Hill Giant should not be on battlefield") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
                withClue("Both should be in opponent's hand") {
                    game.isInHand(2, "Grizzly Bears") shouldBe true
                    game.isInHand(2, "Hill Giant") shouldBe true
                }
            }

            test("cannot cast with only one target creature") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Essence Fracture")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                // Try to cast with only one target - should fail since it requires exactly two
                val castResult = game.execute(
                    CastSpell(
                        game.player1Id,
                        game.findCardsInHand(1, "Essence Fracture").first(),
                        listOf(ChosenTarget.Permanent(bears))
                    )
                )
                withClue("Should fail with only one target") {
                    castResult.error shouldNotBe null
                }
            }
        }
    }
}
