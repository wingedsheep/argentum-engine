package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for King's Assassin's activated ability.
 *
 * King's Assassin: {1}{B}{B} 1/1 Creature - Human Assassin
 * "{T}: Destroy target tapped creature. Activate only during your turn,
 * before attackers are declared."
 */
class KingsAssassinScenarioTest : ScenarioTestBase() {

    init {
        context("King's Assassin activated ability") {

            test("can destroy opponent's tapped creature during precombat main phase") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "King's Assassin")  // 1/1
                    .withCardOnBattlefield(2, "Devoted Hero", tapped = true)  // Opponent's tapped creature
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val assassinId = game.findPermanent("King's Assassin")!!
                val heroId = game.findPermanent("Devoted Hero")!!

                // Verify Devoted Hero is on battlefield
                withClue("Devoted Hero should start on battlefield") {
                    game.isOnBattlefield("Devoted Hero") shouldBe true
                }

                // Find King's Assassin's activated ability
                val cardDef = cardRegistry.getCard("King's Assassin")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate the ability targeting the tapped Devoted Hero
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = assassinId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(heroId))
                    )
                )

                withClue("Activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // Verify ability is on the stack
                withClue("Stack should contain the ability") {
                    game.state.stack.size shouldBe 1
                }

                // King's Assassin should be tapped after activation
                val assassinContainer = game.state.getEntity(assassinId)!!
                withClue("King's Assassin should be tapped after activating tap ability") {
                    assassinContainer.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() shouldBe true
                }

                // Resolve the ability by both players passing priority
                val resolveResults = game.resolveStack()

                // Check for any errors during resolution
                for ((index, res) in resolveResults.withIndex()) {
                    withClue("Resolution step $index should succeed: ${res.error}") {
                        res.error shouldBe null
                    }
                }

                // Verify stack is empty after resolution
                withClue("Stack should be empty after resolution") {
                    game.state.stack.size shouldBe 0
                }

                // Verify Devoted Hero was destroyed (moved to graveyard)
                withClue("Devoted Hero should be destroyed and in opponent's graveyard") {
                    game.isOnBattlefield("Devoted Hero") shouldBe false
                    game.isInGraveyard(2, "Devoted Hero") shouldBe true
                }
            }

            test("activated ability appears on stack with targets in client state") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "King's Assassin")
                    .withCardOnBattlefield(2, "Devoted Hero", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val assassinId = game.findPermanent("King's Assassin")!!
                val heroId = game.findPermanent("Devoted Hero")!!

                val cardDef = cardRegistry.getCard("King's Assassin")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Activate the ability
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = assassinId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(heroId))
                    )
                )

                // Get the client state for player 1
                val clientState = game.getClientState(1)

                // Find the stack zone
                val stackZone = clientState.zones.find { it.zoneId.zoneType == com.wingedsheep.sdk.core.Zone.STACK }
                withClue("Stack zone should exist") {
                    stackZone shouldNotBe null
                }

                // Verify ability is on stack
                withClue("Stack should have one item") {
                    stackZone!!.cardIds.size shouldBe 1
                }

                // Get the ability from the cards map
                val abilityEntityId = stackZone!!.cardIds.first()
                val abilityCard = clientState.cards[abilityEntityId]

                withClue("Ability should be in cards map") {
                    abilityCard shouldNotBe null
                }

                withClue("Ability should be named 'King's Assassin ability'") {
                    abilityCard!!.name shouldBe "King's Assassin ability"
                }

                withClue("Ability should have targets") {
                    abilityCard!!.targets.size shouldBe 1
                }

                withClue("Target should be a Permanent targeting the hero") {
                    val target = abilityCard!!.targets.first()
                    target shouldBe com.wingedsheep.gameserver.dto.ClientChosenTarget.Permanent(heroId)
                }

                withClue("Ability should have the source card's image URI") {
                    abilityCard!!.imageUri shouldNotBe null
                    abilityCard.imageUri shouldBe "https://cards.scryfall.io/normal/front/6/7/670a3149-54fb-4fd2-bc66-3ee9bcc3519d.jpg"
                }
            }

            test("can destroy your own tapped creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "King's Assassin")  // 1/1
                    .withCardOnBattlefield(1, "Devoted Hero", tapped = true)  // Own tapped creature
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val assassinId = game.findPermanent("King's Assassin")!!
                val heroId = game.findPermanent("Devoted Hero")!!

                val cardDef = cardRegistry.getCard("King's Assassin")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = assassinId,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(heroId))
                    )
                )

                withClue("Activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                // Verify Devoted Hero was destroyed (moved to player 1's graveyard)
                withClue("Devoted Hero should be destroyed and in player's graveyard") {
                    game.isOnBattlefield("Devoted Hero") shouldBe false
                    game.isInGraveyard(1, "Devoted Hero") shouldBe true
                }
            }
        }
    }
}
