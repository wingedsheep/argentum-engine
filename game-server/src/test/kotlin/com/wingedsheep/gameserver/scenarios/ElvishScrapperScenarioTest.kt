package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Elvish Scrapper's activated ability.
 *
 * Card reference:
 * - Elvish Scrapper (G): 1/1 Creature - Elf
 *   "{G}, {T}, Sacrifice Elvish Scrapper: Destroy target artifact."
 */
class ElvishScrapperScenarioTest : ScenarioTestBase() {

    // Simple artifact for testing — no abilities
    private val testArtifact = CardDefinition.artifact(
        name = "Test Artifact",
        manaCost = ManaCost.parse("{2}")
    )

    init {
        // Register the test artifact so scenario builder can find it
        cardRegistry.register(testArtifact)

        context("Elvish Scrapper sacrifice-to-destroy") {

            test("sacrifice self to destroy target artifact") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elvish Scrapper")
                    .withCardOnBattlefield(2, "Test Artifact")
                    .withLandsOnBattlefield(1, "Forest", 1)  // {G} for activation cost
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val scrapperId = game.findPermanent("Elvish Scrapper")!!
                val artifactId = game.findPermanent("Test Artifact")!!

                // Find Elvish Scrapper's activated ability
                val cardDef = cardRegistry.getCard("Elvish Scrapper")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate the ability targeting the artifact
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = scrapperId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(artifactId))
                    )
                )

                withClue("Ability should activate successfully: ${result.error}") {
                    result.error shouldBe null
                }

                // Elvish Scrapper should be sacrificed (in graveyard) as part of the cost
                withClue("Elvish Scrapper should be sacrificed to graveyard") {
                    game.isOnBattlefield("Elvish Scrapper") shouldBe false
                    game.isInGraveyard(1, "Elvish Scrapper") shouldBe true
                }

                // Resolve the ability
                game.resolveStack()

                // Artifact should be destroyed
                withClue("Test Artifact should be destroyed") {
                    game.isOnBattlefield("Test Artifact") shouldBe false
                    game.isInGraveyard(2, "Test Artifact") shouldBe true
                }
            }

            test("cannot activate without green mana") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elvish Scrapper")
                    .withCardOnBattlefield(2, "Test Artifact")
                    // No lands — can't pay {G}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val scrapperId = game.findPermanent("Elvish Scrapper")!!
                val artifactId = game.findPermanent("Test Artifact")!!

                val cardDef = cardRegistry.getCard("Elvish Scrapper")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = scrapperId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(artifactId))
                    )
                )

                withClue("Activation should fail without mana") {
                    result.error shouldNotBe null
                }
            }

            test("cannot target non-artifact permanent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elvish Scrapper")
                    .withCardOnBattlefield(2, "Grizzly Bears")  // Not an artifact
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val scrapperId = game.findPermanent("Elvish Scrapper")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!

                val cardDef = cardRegistry.getCard("Elvish Scrapper")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = scrapperId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(bearsId))
                    )
                )

                withClue("Targeting a non-artifact should fail") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
