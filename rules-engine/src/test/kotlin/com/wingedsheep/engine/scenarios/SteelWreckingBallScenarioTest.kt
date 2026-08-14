package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Steel Wrecking Ball (SPM #177) — {5} Artifact.
 *
 *   When this artifact enters, it deals 5 damage to target creature.
 *   {1}{R}, Discard this card: Destroy target artifact.
 *
 * Exercises the ETB damage (5 to a target creature) and the from-hand activated ability
 * ({1}{R}, discard this card: destroy a target artifact — [activateFromZone] = HAND,
 * cost [Costs.DiscardSelf]).
 */
class SteelWreckingBallScenarioTest : ScenarioTestBase() {

    init {
        context("Steel Wrecking Ball") {

            test("ETB deals 5 damage to target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Steel Wrecking Ball")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardOnBattlefield(2, "Vulture, Scheming Scavenger") // 4/6 opponent creature
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vulture = game.findPermanent("Vulture, Scheming Scavenger")!!

                game.castSpell(1, "Steel Wrecking Ball").error shouldBe null
                game.resolveStack() // artifact enters → ETB asks for a target creature

                val result = game.selectTargets(listOf(vulture))
                withClue("targeting the opponent's creature is legal: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("5 damage is marked on the 4/6 (survives, exactly 5 marked)") {
                    game.state.getEntity(vulture)?.get<DamageComponent>()?.amount shouldBe 5
                    game.isOnBattlefield("Vulture, Scheming Scavenger") shouldBe true
                }
                withClue("Steel Wrecking Ball is on the battlefield") {
                    game.isOnBattlefield("Steel Wrecking Ball") shouldBe true
                }
            }

            test("from-hand {1}{R}, Discard this card: Destroy target artifact") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Steel Wrecking Ball")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Steel Wrecking Ball") // target artifact to destroy
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targetArtifact = game.findPermanent("Steel Wrecking Ball")!!
                val handCard = game.findCardsInHand(1, "Steel Wrecking Ball").first()
                val abilityId = cardRegistry.getCard("Steel Wrecking Ball")!!
                    .activatedAbilities.first().id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = handCard,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(targetArtifact))
                    )
                )
                result.error shouldBe null
                game.resolveStack()

                withClue("the from-hand ability resolves: this card is discarded to the graveyard") {
                    game.isInGraveyard(1, "Steel Wrecking Ball") shouldBe true
                }
                withClue("the targeted artifact is destroyed") {
                    game.isOnBattlefield("Steel Wrecking Ball") shouldBe false
                    game.isInGraveyard(2, "Steel Wrecking Ball") shouldBe true
                }
            }
        }
    }
}
