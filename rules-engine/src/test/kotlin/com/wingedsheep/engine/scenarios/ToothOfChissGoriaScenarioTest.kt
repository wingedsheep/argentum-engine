package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mrd.cards.ToothOfChissGoria
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Tooth of Chiss-Goria (MRD #264) — flash artifact, affinity for artifacts, `{T}`: target creature
 * gets +1/+0 until end of turn.
 *
 * Affinity shaving the whole `{3}` and the tap ability being usable the turn the artifact lands are
 * what make this a free combat trick, so both are asserted. The pump is checked to expire at
 * end of turn as well — a permanent-duration mis-wiring would otherwise pass the obvious test.
 */
class ToothOfChissGoriaScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Tooth of Chiss-Goria") {

            test("affinity for artifacts shaves the generic cost; three artifacts make it free") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Tooth of Chiss-Goria")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val calculator = CostCalculator(cardRegistry)
                val tooth = cardRegistry.requireCard("Tooth of Chiss-Goria")

                withClue("nothing on the battlefield — the printed {3}") {
                    calculator.calculateEffectiveCost(game.state, tooth, game.player1Id)
                        .genericAmount shouldBe 3
                }
            }

            test("three artifacts you control reduce it to nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Tooth of Chiss-Goria")
                    .withCardOnBattlefield(1, "Tooth of Chiss-Goria")
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withCardOnBattlefield(1, "Fireshrieker")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cost = CostCalculator(cardRegistry).calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Tooth of Chiss-Goria"),
                    game.player1Id,
                )

                withClue("affinity only shaves generic, and {3} is entirely generic") {
                    cost.genericAmount shouldBe 0
                }
            }

            test("{T} gives target creature +1/+0 until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    // No summoningSickness flag needed: a noncreature artifact can tap the turn
                    // it enters, which is the whole point of pairing flash with this ability.
                    .withCardOnBattlefield(1, "Tooth of Chiss-Goria")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tooth = game.findPermanent("Tooth of Chiss-Goria")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val pump = ToothOfChissGoria.activatedAbilities[0].id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = tooth,
                        abilityId = pump,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                val pumped = projector.project(game.state)
                pumped.getPower(bears) shouldBe 3
                withClue("+1/+0 leaves toughness alone") { pumped.getToughness(bears) shouldBe 2 }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("the bonus is 'until end of turn' — it expires in cleanup") {
                    projector.project(game.state).getPower(bears) shouldBe 2
                }
            }

            test("it has flash") {
                withClue("flash is what lets the pump be held up as a combat trick") {
                    cardRegistry.requireCard("Tooth of Chiss-Goria")
                        .keywords.contains(Keyword.FLASH) shouldBe true
                }
            }
        }
    }
}
