package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Three Tree City.
 *
 * Card reference:
 * - Three Tree City: Legendary Land
 *   As Three Tree City enters, choose a creature type.
 *   {T}: Add {C}.
 *   {2}, {T}: Choose a color. Add an amount of mana of that color equal to
 *   the number of creatures you control of the chosen type.
 */
class ThreeTreeCityScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseCreatureType(typeName: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val options = (decision as ChooseOptionDecision).options
        val index = options.indexOf(typeName)
        withClue("Creature type '$typeName' should be in options") {
            (index >= 0) shouldBe true
        }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    private fun TestGame.playLandByName(playerNumber: Int, landName: String): com.wingedsheep.engine.core.ExecutionResult {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        val hand = state.getHand(playerId)
        val cardId = hand.find { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name == landName
        } ?: error("Card '$landName' not found in player $playerNumber's hand")
        return execute(PlayLand(playerId, cardId))
    }

    init {
        context("Three Tree City - choose creature type on entry") {

            test("playing Three Tree City prompts for creature type choice") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Three Tree City")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val result = game.playLandByName(1, "Three Tree City")
                withClue("Play land should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                withClue("Should have pending creature type decision") {
                    game.hasPendingDecision() shouldBe true
                }

                game.chooseCreatureType("Elf")

                withClue("Three Tree City should be on battlefield") {
                    game.isOnBattlefield("Three Tree City") shouldBe true
                }

                val cityId = game.findPermanent("Three Tree City")!!
                val chosenType = game.state.getEntity(cityId)?.get<ChosenCreatureTypeComponent>()
                withClue("Should have chosen creature type stored") {
                    chosenType.shouldNotBeNull()
                    chosenType.creatureType shouldBe "Elf"
                }
            }
        }

        context("Three Tree City - mana abilities") {

            test("{2}, {T}: produces mana equal to creatures of chosen type") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Three Tree City")
                    .withCardOnBattlefield(1, "Elvish Warrior")  // Elf Warrior
                    .withCardOnBattlefield(1, "Wirewood Herald") // Elf
                    .withLandsOnBattlefield(1, "Forest", 2)      // To pay {2} for second ability
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Play the land
                game.playLandByName(1, "Three Tree City")
                game.chooseCreatureType("Elf")

                val cityId = game.findPermanent("Three Tree City")!!
                val cardDef = cardRegistry.getCard("Three Tree City")!!
                val manaAbility = cardDef.script.activatedAbilities[1] // second ability: {2}, {T}

                // Activate {2}, {T} mana ability choosing green
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = cityId,
                        abilityId = manaAbility.id,
                        manaColorChoice = Color.GREEN
                    )
                )

                withClue("Mana ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // Should produce 2 green mana (2 Elves on battlefield)
                val manaPool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Should have 2 green mana from 2 Elves") {
                    manaPool.shouldNotBeNull()
                    manaPool.green shouldBe 2
                }
            }

            test("{2}, {T}: produces zero mana when no creatures of chosen type") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Three Tree City")
                    .withCardOnBattlefield(1, "Glory Seeker")  // Human Soldier, not an Elf
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.playLandByName(1, "Three Tree City")
                game.chooseCreatureType("Elf")

                val cityId = game.findPermanent("Three Tree City")!!
                val cardDef = cardRegistry.getCard("Three Tree City")!!
                val manaAbility = cardDef.script.activatedAbilities[1]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = cityId,
                        abilityId = manaAbility.id,
                        manaColorChoice = Color.GREEN
                    )
                )

                withClue("Mana ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // Should produce 0 green mana (no Elves)
                val manaPool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                val greenMana = manaPool?.green ?: 0
                withClue("Should have 0 green mana when no Elves on battlefield") {
                    greenMana shouldBe 0
                }
            }
        }
    }
}
