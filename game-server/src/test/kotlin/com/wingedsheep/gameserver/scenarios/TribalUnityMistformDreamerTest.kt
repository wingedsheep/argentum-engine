package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests interaction between Tribal Unity and Mistform Dreamer.
 *
 * Scenario: P2 activates Mistform Dreamer's ability to become a Cleric, then P1
 * casts Tribal Unity choosing Cleric. Mistform Dreamer should get +X/+X because
 * it is a Cleric when Tribal Unity's effect determines affected creatures.
 */
class TribalUnityMistformDreamerTest : ScenarioTestBase() {

    private fun TestGame.chooseCreatureType(typeName: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val options = (decision as ChooseOptionDecision).options
        val index = options.indexOf(typeName)
        withClue("Creature type '$typeName' should be in options $options") {
            (index >= 0) shouldBe true
        }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    init {
        context("Tribal Unity + Mistform Dreamer interaction") {

            test("Mistform Dreamer gets +X/+X when it has become the chosen creature type") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Tribal Unity")
                    .withLandsOnBattlefield(1, "Forest", 5) // {2}{G} + X=2 → 5 mana total
                    .withCardOnBattlefield(2, "Mistform Dreamer") // 2/1 Illusion, Flying
                    .withLandsOnBattlefield(2, "Island", 1) // {1} for Mistform Dreamer's ability
                    .withActivePlayer(1)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dreamerId = game.findPermanent("Mistform Dreamer")!!

                // P2 activates Mistform Dreamer's ability to become a Cleric
                val dreamerDef = cardRegistry.getCard("Mistform Dreamer")!!
                val ability = dreamerDef.script.activatedAbilities.first()

                val activateResult = game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = dreamerId,
                        abilityId = ability.id
                    )
                )
                withClue("Mistform Dreamer ability activation should succeed: ${activateResult.error}") {
                    activateResult.error shouldBe null
                }

                // Resolve Dreamer's ability — choose Cleric
                game.resolveStack()
                game.chooseCreatureType("Cleric")

                // Pass priority to P1 so they can cast
                game.passPriority()

                // P1 casts Tribal Unity with X=2 (auto-resolves since P2 has no mana left)
                game.castXSpell(1, "Tribal Unity", xValue = 2)

                // Choose Cleric during casting (creature type is chosen as part of casting)
                game.chooseCreatureType("Cleric")

                // Resolve the stack — Tribal Unity resolves and applies +X/+X to all Clerics
                game.resolveStack()

                // Mistform Dreamer (2/1) should now be 4/3 (+2/+2 from Tribal Unity)
                val clientState = game.getClientState(2)
                val dreamerInfo = clientState.cards[dreamerId]
                dreamerInfo shouldNotBe null
                withClue("Mistform Dreamer (2/1 now Cleric) should be 4/3 after +2/+2 from Tribal Unity, but was ${dreamerInfo!!.power}/${dreamerInfo.toughness}") {
                    dreamerInfo.power shouldBe 4
                    dreamerInfo.toughness shouldBe 3
                }
            }
        }
    }
}
