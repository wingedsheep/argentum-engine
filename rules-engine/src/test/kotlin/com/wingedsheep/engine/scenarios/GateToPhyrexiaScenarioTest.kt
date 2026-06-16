package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Gate to Phyrexia (ATQ #16).
 *
 * {B}{B} Enchantment
 * "Sacrifice a creature: Destroy target artifact. Activate only during your upkeep and only
 *  once each turn."
 */
class GateToPhyrexiaScenarioTest : ScenarioTestBase() {

    private val abilityId by lazy {
        cardRegistry.getCard("Gate to Phyrexia")!!.script.activatedAbilities[0].id
    }

    init {
        context("Gate to Phyrexia") {

            test("during your upkeep, sacrificing a creature destroys a target artifact") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Gate to Phyrexia")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    // Opponent's artifact to destroy.
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()

                val gateId = game.findPermanent("Gate to Phyrexia")!!
                val creatureId = game.findPermanent("Grizzly Bears")!!
                val artifactId = game.findPermanent("Ornithopter")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = gateId,
                        abilityId = abilityId,
                        targets = listOf(entityIdToChosenTarget(game.state, artifactId)),
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(creatureId))
                    )
                )
                withClue("Activating Gate to Phyrexia during upkeep should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("The targeted artifact should be destroyed") {
                    game.isOnBattlefield("Ornithopter") shouldBe false
                }
                withClue("The sacrificed creature should be gone") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("the ability is NOT a legal action outside your upkeep (precombat main)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Gate to Phyrexia")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gateId = game.findPermanent("Gate to Phyrexia")!!

                val gateActions = game.getLegalActions(1).filter {
                    (it.action as? ActivateAbility)?.sourceId == gateId
                }
                withClue("Gate to Phyrexia must not be activatable outside the controller's upkeep") {
                    gateActions shouldBe emptyList()
                }
            }

            test("the once-per-turn restriction rejects a second activation in the same upkeep") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Gate to Phyrexia")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withCardOnBattlefield(2, "Bottle of Suleiman")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()

                val gateId = game.findPermanent("Gate to Phyrexia")!!
                val firstCreature = game.findPermanent("Grizzly Bears")!!
                val firstArtifact = game.findPermanent("Ornithopter")!!

                val first = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = gateId,
                        abilityId = abilityId,
                        targets = listOf(entityIdToChosenTarget(game.state, firstArtifact)),
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(firstCreature))
                    )
                )
                withClue("First activation should succeed: ${first.error}") {
                    first.error shouldBe null
                }
                game.resolveStack()

                val secondCreature = game.findPermanent("Hill Giant")!!
                val secondArtifact = game.findPermanent("Bottle of Suleiman")!!
                val second = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = gateId,
                        abilityId = abilityId,
                        targets = listOf(entityIdToChosenTarget(game.state, secondArtifact)),
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(secondCreature))
                    )
                )
                withClue("Second activation in the same turn must be rejected (once per turn)") {
                    (second.error != null) shouldBe true
                }
                withClue("The second creature must NOT have been sacrificed (cost not paid)") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
            }
        }
    }
}
