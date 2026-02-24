package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Dragon Shadow.
 *
 * Card reference:
 * - Dragon Shadow ({1}{B}): Enchantment — Aura
 *   "Enchant creature"
 *   "Enchanted creature gets +1/+0 and has fear."
 *   "When a creature with mana value 6 or greater enters, you may return
 *    Dragon Shadow from your graveyard to the battlefield attached to that creature."
 */
class DragonShadowScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Dragon Shadow aura") {
            test("casting Dragon Shadow grants +1/+0 and fear to enchanted creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Dragon Shadow")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Cast Dragon Shadow targeting Grizzly Bears
                val castResult = game.castSpell(1, "Dragon Shadow", bearsId)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // Dragon Shadow should be on the battlefield attached to Grizzly Bears
                withClue("Dragon Shadow should be on the battlefield") {
                    game.isOnBattlefield("Dragon Shadow") shouldBe true
                }

                val shadowId = game.findPermanent("Dragon Shadow")!!
                val attached = game.state.getEntity(shadowId)!!.get<AttachedToComponent>()
                withClue("Dragon Shadow should be attached to Grizzly Bears") {
                    attached shouldNotBe null
                    attached!!.targetId shouldBe bearsId
                }

                // Check +1/+0 and fear in projected state
                val projected = stateProjector.project(game.state)
                withClue("Grizzly Bears should have power 3 (2 base + 1 from Dragon Shadow)") {
                    projected.getPower(bearsId) shouldBe 3
                }
                withClue("Grizzly Bears should have toughness 2 (unchanged)") {
                    projected.getToughness(bearsId) shouldBe 2
                }
                withClue("Grizzly Bears should have fear") {
                    projected.hasKeyword(bearsId, Keyword.FEAR) shouldBe true
                }
            }

            test("Dragon Shadow returns from graveyard when creature with MV 6+ enters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInGraveyard(1, "Dragon Shadow")
                    .withCardInHand(1, "Elvish Aberration") // MV 6 creature
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Dragon Shadow should be in graveyard
                withClue("Dragon Shadow should be in graveyard") {
                    game.isInGraveyard(1, "Dragon Shadow") shouldBe true
                }

                // Cast Elvish Aberration (MV 6)
                val castResult = game.castSpell(1, "Elvish Aberration")
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                // Resolve Elvish Aberration entering the battlefield
                game.resolveStack()

                // Dragon Shadow's trigger fires - answer yes to return it
                withClue("Should have a yes/no decision for Dragon Shadow's trigger") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                // Resolve the triggered ability
                game.resolveStack()

                // Dragon Shadow should now be on the battlefield attached to Elvish Aberration
                withClue("Dragon Shadow should be on the battlefield") {
                    game.isOnBattlefield("Dragon Shadow") shouldBe true
                }

                val aberrationId = game.findPermanent("Elvish Aberration")!!
                val shadowId = game.findPermanent("Dragon Shadow")!!
                val attached = game.state.getEntity(shadowId)!!.get<AttachedToComponent>()
                withClue("Dragon Shadow should be attached to Elvish Aberration") {
                    attached shouldNotBe null
                    attached!!.targetId shouldBe aberrationId
                }

                // Check +1/+0 and fear on Elvish Aberration
                val projected = stateProjector.project(game.state)
                withClue("Elvish Aberration should have power 5 (4 base + 1 from Dragon Shadow)") {
                    projected.getPower(aberrationId) shouldBe 5
                }
                withClue("Elvish Aberration should have fear") {
                    projected.hasKeyword(aberrationId, Keyword.FEAR) shouldBe true
                }
            }

            test("Dragon Shadow does not trigger for creature with MV less than 6") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInGraveyard(1, "Dragon Shadow")
                    .withCardInHand(1, "Grizzly Bears") // MV 2 creature
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Grizzly Bears (MV 2)
                val castResult = game.castSpell(1, "Grizzly Bears")
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // Dragon Shadow should remain in graveyard (no trigger)
                withClue("Dragon Shadow should still be in graveyard") {
                    game.isInGraveyard(1, "Dragon Shadow") shouldBe true
                }
                withClue("Dragon Shadow should not be on the battlefield") {
                    game.isOnBattlefield("Dragon Shadow") shouldBe false
                }
            }

            test("Dragon Shadow graveyard trigger is optional - declining leaves it in graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInGraveyard(1, "Dragon Shadow")
                    .withCardInHand(1, "Elvish Aberration") // MV 6 creature
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Elvish Aberration
                game.castSpell(1, "Elvish Aberration")
                game.resolveStack()

                // Decline the trigger
                withClue("Should have a yes/no decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(false)

                game.resolveStack()

                // Dragon Shadow should still be in graveyard
                withClue("Dragon Shadow should still be in graveyard") {
                    game.isInGraveyard(1, "Dragon Shadow") shouldBe true
                }
                withClue("Dragon Shadow should not be on the battlefield") {
                    game.isOnBattlefield("Dragon Shadow") shouldBe false
                }
            }
        }
    }
}
