package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ChosenColorComponent
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CharacteristicValue
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Riptide Replicator.
 *
 * Card reference:
 * - Riptide Replicator ({X}{4}): Artifact
 *   "As Riptide Replicator enters the battlefield, choose a color and a creature type.
 *    Riptide Replicator enters the battlefield with X charge counters on it.
 *    {4}, {T}: Create an X/X creature token of the chosen color and type, where X is
 *    the number of charge counters on Riptide Replicator."
 */
class RiptideReplicatorScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseColor(color: Color) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseColorDecision>()
        submitDecision(ColorChosenResponse(decision.id, color))
    }

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

    init {
        context("Riptide Replicator - ETB with color and creature type choice") {

            test("cast with X=3, choose Red and Goblin, verify 3 charge counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Riptide Replicator")
                    .withLandsOnBattlefield(1, "Mountain", 7)  // 4 + X=3
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Riptide Replicator with X=3
                val castResult = game.castXSpell(1, "Riptide Replicator", 3)
                withClue("Riptide Replicator should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell - should pause for color choice first
                game.resolveStack()

                withClue("Should have pending color decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Choose Red
                game.chooseColor(Color.RED)

                withClue("Should have pending creature type decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Choose Goblin
                game.chooseCreatureType("Goblin")

                // Riptide Replicator should now be on the battlefield
                withClue("Riptide Replicator should be on battlefield") {
                    game.isOnBattlefield("Riptide Replicator") shouldBe true
                }

                // Verify 3 charge counters
                val replicatorId = game.findPermanent("Riptide Replicator")!!
                val counters = game.state.getEntity(replicatorId)?.get<CountersComponent>()
                withClue("Should have 3 charge counters") {
                    counters.shouldNotBeNull()
                    counters.getCount(CounterType.CHARGE) shouldBe 3
                }

                // Verify chosen color
                val chosenColor = game.state.getEntity(replicatorId)?.get<ChosenColorComponent>()
                withClue("Should have chosen Red") {
                    chosenColor.shouldNotBeNull()
                    chosenColor.color shouldBe Color.RED
                }

                // Verify chosen creature type
                val chosenType = game.state.getEntity(replicatorId)?.get<ChosenCreatureTypeComponent>()
                withClue("Should have chosen Goblin") {
                    chosenType.shouldNotBeNull()
                    chosenType.creatureType shouldBe "Goblin"
                }
            }

            test("activate ability - create token with correct color, type, and P/T") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Riptide Replicator")
                    .withLandsOnBattlefield(1, "Mountain", 11)  // 4 + X=3 + 4 for activation
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Riptide Replicator with X=3
                game.castXSpell(1, "Riptide Replicator", 3)
                game.resolveStack()
                game.chooseColor(Color.RED)
                game.chooseCreatureType("Goblin")

                // Now activate the ability: {4}, {T}: Create an X/X token
                val replicatorId = game.findPermanent("Riptide Replicator")!!
                val cardDef = cardRegistry.getCard("Riptide Replicator")!!
                val ability = cardDef.script.activatedAbilities.first()

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = replicatorId,
                        abilityId = ability.id,
                        targets = emptyList()
                    )
                )

                withClue("Ability should activate successfully: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                // Should have a 3/3 red Goblin token on the battlefield
                val tokenId = game.findPermanent("Goblin Token")
                withClue("Should have Goblin Token on battlefield") {
                    tokenId.shouldNotBeNull()
                }

                val tokenCard = game.state.getEntity(tokenId!!)?.get<CardComponent>()
                withClue("Token should be 3/3") {
                    tokenCard.shouldNotBeNull()
                    (tokenCard.baseStats?.power as CharacteristicValue.Fixed).value shouldBe 3
                    (tokenCard.baseStats?.toughness as CharacteristicValue.Fixed).value shouldBe 3
                }

                withClue("Token should be red") {
                    tokenCard!!.colors shouldBe setOf(Color.RED)
                }

                withClue("Token should be a Goblin creature") {
                    tokenCard!!.typeLine.hasSubtype(Subtype("Goblin")) shouldBe true
                }
            }

            test("X=0 - activate ability creates 0/0 token that dies to SBAs") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Riptide Replicator")
                    .withLandsOnBattlefield(1, "Mountain", 8)  // 4 + X=0 + 4 for activation
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Riptide Replicator with X=0
                game.castXSpell(1, "Riptide Replicator", 0)
                game.resolveStack()
                game.chooseColor(Color.BLUE)
                game.chooseCreatureType("Wizard")

                // Verify 0 charge counters
                val replicatorId = game.findPermanent("Riptide Replicator")!!
                val counters = game.state.getEntity(replicatorId)?.get<CountersComponent>()
                withClue("Should have no charge counters (or no CountersComponent)") {
                    if (counters != null) {
                        counters.getCount(CounterType.CHARGE) shouldBe 0
                    }
                }

                // Activate the ability
                val cardDef = cardRegistry.getCard("Riptide Replicator")!!
                val ability = cardDef.script.activatedAbilities.first()

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = replicatorId,
                        abilityId = ability.id,
                        targets = emptyList()
                    )
                )

                withClue("Ability should activate: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                game.resolveStack()

                // 0/0 token should have died to state-based actions
                withClue("0/0 token should not survive (SBAs)") {
                    game.findPermanent("Wizard Token") shouldBe null
                }
            }
        }
    }
}
