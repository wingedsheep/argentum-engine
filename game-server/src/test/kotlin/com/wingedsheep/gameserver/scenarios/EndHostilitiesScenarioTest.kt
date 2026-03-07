package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for End Hostilities.
 *
 * End Hostilities: {3}{W}{W} Sorcery
 * "Destroy all creatures and all permanents attached to creatures."
 */
class EndHostilitiesScenarioTest : ScenarioTestBase() {

    init {
        context("End Hostilities") {

            test("destroys all creatures on the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "End Hostilities")
                    .withCardOnBattlefield(1, "Devoted Hero")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "End Hostilities")
                game.resolveStack()

                withClue("Player's creature should be destroyed") {
                    game.isOnBattlefield("Devoted Hero") shouldBe false
                }
                withClue("Opponent's creature should be destroyed") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("destroys equipment attached to creatures") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "End Hostilities")
                    .withCardOnBattlefield(1, "Devoted Hero")
                    .withCardOnBattlefield(1, "Heart-Piercer Bow")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Equip Heart-Piercer Bow to Devoted Hero
                val bowId = game.findPermanent("Heart-Piercer Bow")!!
                val heroId = game.findPermanent("Devoted Hero")!!
                val cardDef = cardRegistry.getCard("Heart-Piercer Bow")!!
                val equipAbility = cardDef.script.activatedAbilities.first()

                val equipResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = bowId,
                        abilityId = equipAbility.id,
                        targets = listOf(ChosenTarget.Permanent(heroId))
                    )
                )
                withClue("Equip should succeed") { equipResult.error shouldBe null }
                game.resolveStack()

                // Verify equipment is attached
                val attached = game.state.getEntity(bowId)?.get<AttachedToComponent>()
                withClue("Bow should be attached to hero") {
                    attached?.targetId shouldBe heroId
                }

                // Cast End Hostilities
                game.castSpell(1, "End Hostilities")
                game.resolveStack()

                withClue("Creature should be destroyed") {
                    game.isOnBattlefield("Devoted Hero") shouldBe false
                }
                withClue("Equipment should also be destroyed") {
                    game.isOnBattlefield("Heart-Piercer Bow") shouldBe false
                }
            }

            test("does not destroy non-creature permanents that are not attached to creatures") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "End Hostilities")
                    .withCardOnBattlefield(1, "Devoted Hero")
                    .withCardOnBattlefield(1, "Heart-Piercer Bow") // unattached equipment
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "End Hostilities")
                game.resolveStack()

                withClue("Creature should be destroyed") {
                    game.isOnBattlefield("Devoted Hero") shouldBe false
                }
                withClue("Unattached equipment should survive") {
                    game.isOnBattlefield("Heart-Piercer Bow") shouldBe true
                }
            }
        }
    }
}
