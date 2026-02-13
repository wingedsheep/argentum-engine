package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Sea's Claim.
 *
 * Card reference:
 * - Sea's Claim (U): Enchantment — Aura
 *   "Enchant land"
 *   "Enchanted land is an Island."
 */
class SeasClaimScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()
    private val manaSolver = ManaSolver(cardRegistry)

    init {
        context("Sea's Claim changes enchanted land's type to Island") {
            test("casting Sea's Claim on a Mountain makes it an Island") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sea's Claim")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentMountain = game.findPermanent("Mountain")!!

                // Cast Sea's Claim targeting opponent's Mountain
                val castResult = game.castSpell(1, "Sea's Claim", opponentMountain)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // Sea's Claim should be on the battlefield attached to the Mountain
                withClue("Sea's Claim should be on the battlefield") {
                    game.isOnBattlefield("Sea's Claim") shouldBe true
                }

                val seasClaimId = game.findPermanent("Sea's Claim")!!
                val attachedTo = game.state.getEntity(seasClaimId)!!.get<AttachedToComponent>()
                withClue("Sea's Claim should be attached to the Mountain") {
                    attachedTo shouldNotBe null
                    attachedTo!!.targetId shouldBe opponentMountain
                }

                // The projected state should show the land as an Island, not a Mountain
                val projected = stateProjector.project(game.state)
                withClue("Enchanted land should have Island subtype") {
                    projected.hasSubtype(opponentMountain, "Island") shouldBe true
                }
                withClue("Enchanted land should no longer have Mountain subtype") {
                    projected.hasSubtype(opponentMountain, "Mountain") shouldBe false
                }
            }

            test("Sea's Claim on a Forest replaces Forest with Island") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sea's Claim")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentForest = game.findPermanent("Forest")!!

                game.castSpell(1, "Sea's Claim", opponentForest)
                game.resolveStack()

                val projected = stateProjector.project(game.state)
                withClue("Enchanted land should be an Island") {
                    projected.hasSubtype(opponentForest, "Island") shouldBe true
                }
                withClue("Enchanted land should no longer be a Forest") {
                    projected.hasSubtype(opponentForest, "Forest") shouldBe false
                }
                // Should still be a Land type
                withClue("Enchanted land should still be a Land") {
                    projected.hasType(opponentForest, "LAND") shouldBe true
                }
            }

            test("mana solver detects changed land type for mana production") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sea's Claim")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Sea's Claim", game.findPermanent("Mountain")!!)
                game.resolveStack()

                // The enchanted Mountain (now Island) should produce blue mana
                // Opponent should be able to pay {U} with their enchanted land
                withClue("Opponent should be able to pay {U} from enchanted land (now Island)") {
                    manaSolver.canPay(game.state, game.player2Id, ManaCost.parse("{U}")) shouldBe true
                }
                // Opponent should NOT be able to pay {R} (no longer a Mountain)
                withClue("Opponent should not be able to pay {R} (no longer a Mountain)") {
                    manaSolver.canPay(game.state, game.player2Id, ManaCost.parse("{R}")) shouldBe false
                }
            }

            test("removing Sea's Claim restores original land type") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sea's Claim")
                    .withCardInHand(2, "Naturalize")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentMountain = game.findPermanent("Mountain")!!

                // Cast Sea's Claim
                game.castSpell(1, "Sea's Claim", opponentMountain)
                game.resolveStack()

                // Verify it's now an Island
                var projected = stateProjector.project(game.state)
                withClue("Should be an Island after Sea's Claim") {
                    projected.hasSubtype(opponentMountain, "Island") shouldBe true
                }

                // Pass to opponent's turn
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passPriority()
                game.passPriority()

                // Opponent casts Naturalize to destroy Sea's Claim
                val seasClaimId = game.findPermanent("Sea's Claim")!!
                game.castSpell(2, "Naturalize", seasClaimId)
                game.resolveStack()

                // Sea's Claim should be gone
                withClue("Sea's Claim should be destroyed") {
                    game.isOnBattlefield("Sea's Claim") shouldBe false
                }

                // Mountain should be a Mountain again
                projected = stateProjector.project(game.state)
                withClue("Land should be a Mountain again after Sea's Claim is destroyed") {
                    projected.hasSubtype(opponentMountain, "Mountain") shouldBe true
                }
                withClue("Land should no longer be an Island") {
                    projected.hasSubtype(opponentMountain, "Island") shouldBe false
                }
            }
        }
    }
}
