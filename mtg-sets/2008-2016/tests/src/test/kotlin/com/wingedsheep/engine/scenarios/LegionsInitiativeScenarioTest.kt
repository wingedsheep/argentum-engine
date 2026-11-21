package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dgm.cards.LegionsInitiative
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Legion's Initiative (Dragon's Maze).
 *
 * Oracle: "Red creatures you control get +1/+0. / White creatures you control get +0/+1. /
 * {R}{W}, Exile this enchantment: Exile all creatures you control. At the beginning of the next
 * combat, return those cards to the battlefield under their owner's control and those creatures
 * gain haste until end of turn."
 *
 * The interesting half is the delayed return: the enchantment is gone (exiled as the cost) before
 * the ability resolves, and the exiled pile comes back only at the next beginning of combat.
 */
class LegionsInitiativeScenarioTest : ScenarioTestBase() {

    private val abilityId = LegionsInitiative.activatedAbilities.first().id

    init {
        context("Legion's Initiative") {

            test("red creatures get +1/+0 and white creatures get +0/+1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Legion's Initiative")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val projected = game.state.projectedState
                val giant = game.findPermanent("Hill Giant")!!
                val lions = game.findPermanent("Savannah Lions")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                projected.getPower(giant) shouldBe 4
                projected.getToughness(giant) shouldBe 3
                projected.getPower(lions) shouldBe 1 // the test registry's Savannah Lions is a 1/1
                projected.getToughness(lions) shouldBe 2
                withClue("an opponent's creature is unaffected") {
                    projected.getPower(bears) shouldBe 2
                    projected.getToughness(bears) shouldBe 2
                }
            }

            test("exiles your creatures and returns them with haste at the beginning of the next combat") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Legion's Initiative")
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardOnBattlefield(1, "Savannah Lions", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initiative = game.findPermanent("Legion's Initiative")!!
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = initiative, abilityId = abilityId)
                ).error shouldBe null

                withClue("the enchantment was exiled as part of the cost") {
                    game.isOnBattlefield("Legion's Initiative") shouldBe false
                    game.isInExile(1, "Legion's Initiative") shouldBe true
                }

                game.resolveStack()

                withClue("all creatures you control are exiled; the opponent's stays") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isOnBattlefield("Savannah Lions") shouldBe false
                    game.isInExile(1, "Hill Giant") shouldBe true
                    game.isInExile(1, "Savannah Lions") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                val giant = game.findPermanent("Hill Giant")
                val lions = game.findPermanent("Savannah Lions")
                withClue("both creatures returned at the beginning of combat") {
                    (giant != null) shouldBe true
                    (lions != null) shouldBe true
                }
                val projected = game.state.projectedState
                withClue("the returned creatures have haste") {
                    projected.hasKeyword(giant!!, Keyword.HASTE) shouldBe true
                    projected.hasKeyword(lions!!, Keyword.HASTE) shouldBe true
                }
                withClue("the anthems are gone with the enchantment") {
                    projected.getPower(giant!!) shouldBe 3
                    projected.getToughness(lions!!) shouldBe 1
                }
            }

            test("activated during the beginning of combat step, the creatures wait for the next combat") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Legion's Initiative")
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .apply { repeat(3) { withCardInLibrary(1, "Mountain"); withCardInLibrary(2, "Mountain") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                    .build()

                val initiative = game.findPermanent("Legion's Initiative")!!
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = initiative, abilityId = abilityId)
                ).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("this turn's beginning of combat had already begun, so the giant stays exiled") {
                    game.isInExile(1, "Hill Giant") shouldBe true
                }

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()
                val giant = game.findPermanent("Hill Giant")
                withClue("the next combat (the opponent's turn) returns it under its owner's control, with haste") {
                    (giant != null) shouldBe true
                    game.state.projectedState.getController(giant!!) shouldBe game.player1Id
                    game.state.projectedState.hasKeyword(giant, Keyword.HASTE) shouldBe true
                }
            }
        }
    }
}
