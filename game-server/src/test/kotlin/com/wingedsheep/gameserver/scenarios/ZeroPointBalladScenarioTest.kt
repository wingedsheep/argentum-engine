package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Zero Point Ballad — exercises the new
 * `CardPredicate.ToughnessAtMostX` primitive (resolves X at filter time).
 *
 * Card reference:
 * - Zero Point Ballad ({X}{B}): Sorcery
 *   Destroy all creatures with toughness X or less. You lose X life.
 *   If X is 6 or more, return a creature card put into a graveyard this way to the battlefield under your control.
 */
class ZeroPointBalladScenarioTest : ScenarioTestBase() {

    init {
        context("Zero Point Ballad") {

            test("X=2 destroys creatures with toughness 2 or less, leaves larger ones alone") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Zero Point Ballad")
                    .withLandsOnBattlefield(1, "Swamp", 3) // X=2 plus {B}
                    .withCardOnBattlefield(2, "Glory Seeker")     // 2/2
                    .withCardOnBattlefield(2, "Gustcloak Runner") // 1/1
                    .withCardOnBattlefield(2, "Hill Giant")       // 3/3
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(1)

                val castResult = game.castXSpell(1, "Zero Point Ballad", xValue = 2)
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("2/2 creature should die (toughness 2 ≤ X=2)") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }
                withClue("1/1 creature should die (toughness 1 ≤ X=2)") {
                    game.isOnBattlefield("Gustcloak Runner") shouldBe false
                }
                withClue("3/3 creature should survive (toughness 3 > X=2)") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
                withClue("Caster should lose X=2 life") {
                    game.getLifeTotal(1) shouldBe startingLife - 2
                }
            }

            test("X<6 destroys creatures and loses life but does NOT reanimate") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Zero Point Ballad")
                    .withLandsOnBattlefield(1, "Swamp", 6) // X=5 plus {B}
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3 (toughness 3)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(1)

                val castResult = game.castXSpell(1, "Zero Point Ballad", xValue = 5)
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Hill Giant should die (toughness 3 ≤ X=5)") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
                withClue("Caster should lose X=5 life") {
                    game.getLifeTotal(1) shouldBe startingLife - 5
                }
                withClue("Hill Giant should remain in opponent's graveyard (not reanimated, X<6)") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
                withClue("No pending decision should be queued — reanimation clause is gated by X≥6") {
                    game.state.pendingDecision shouldBe null
                }
            }

            test("X=6 reanimates a destroyed creature under caster's control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Zero Point Ballad")
                    .withLandsOnBattlefield(1, "Swamp", 7) // X=6 plus {B}
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3 (opponent's)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castXSpell(1, "Zero Point Ballad", xValue = 6)
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Reanimation clause: SelectFromCollection auto-selects when only one creature
                // is eligible. Hill Giant should return under player 1's control.
                withClue("Hill Giant should be reanimated to the battlefield") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
                val hillGiantId = game.findPermanent("Hill Giant")!!
                val controllerId = game.state.getEntity(hillGiantId)
                    ?.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()
                    ?.playerId
                withClue("Hill Giant should be controlled by the spell's caster") {
                    controllerId shouldBe game.player1Id
                }
            }
        }
    }
}
