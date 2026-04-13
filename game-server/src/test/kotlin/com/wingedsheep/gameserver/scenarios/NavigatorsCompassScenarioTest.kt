package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Navigator's Compass.
 *
 * Card reference:
 * - Navigator's Compass ({1}): Artifact
 *   "When this artifact enters, you gain 3 life."
 *   "{T}: Until end of turn, target land you control becomes the basic land type
 *    of your choice in addition to its other types."
 */
class NavigatorsCompassScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseBasicLandType(typeName: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val options = decision.options
        val index = options.indexOf(typeName)
        withClue("Basic land type '$typeName' should be in options $options") {
            (index >= 0) shouldBe true
        }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    init {
        context("Navigator's Compass") {

            test("ETB trigger gains 3 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Navigator's Compass")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialLife = game.getLifeTotal(1)
                game.castSpell(1, "Navigator's Compass")
                game.resolveStack()

                game.getLifeTotal(1) shouldBe initialLife + 3
            }

            test("tap ability adds chosen basic land type to target land") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Navigator's Compass")
                    .withCardOnBattlefield(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val compassId = game.findPermanent("Navigator's Compass")!!
                val mountainId = game.findPermanent("Mountain")!!

                val cardDef = cardRegistry.getCard("Navigator's Compass")!!
                val tapAbility = cardDef.script.activatedAbilities[0]

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = compassId,
                        abilityId = tapAbility.id,
                        targets = listOf(ChosenTarget.Permanent(mountainId))
                    )
                )
                withClue("Activate should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                // Resolve the ability on the stack
                game.resolveStack()

                // Choose "Island" as the basic land type
                game.chooseBasicLandType("Island")

                // Mountain should now also have "Island" subtype
                val clientState = game.getClientState(1)
                val mountainCard = clientState.cards[mountainId]
                mountainCard.shouldNotBeNull()
                mountainCard.subtypes.shouldContain("Island")
                mountainCard.subtypes.shouldContain("Mountain")
            }

            test("basic land type options include all five types") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Navigator's Compass")
                    .withCardOnBattlefield(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val compassId = game.findPermanent("Navigator's Compass")!!
                val mountainId = game.findPermanent("Mountain")!!

                val cardDef = cardRegistry.getCard("Navigator's Compass")!!
                val tapAbility = cardDef.script.activatedAbilities[0]

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = compassId,
                        abilityId = tapAbility.id,
                        targets = listOf(ChosenTarget.Permanent(mountainId))
                    )
                )

                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldNotBeNull()
                decision.shouldBeInstanceOf<ChooseOptionDecision>()
                val options = decision.options

                options.shouldContain("Plains")
                options.shouldContain("Island")
                options.shouldContain("Swamp")
                options.shouldContain("Mountain")
                options.shouldContain("Forest")
            }
        }
    }
}
