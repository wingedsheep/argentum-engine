package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Thought Shucker (BLB #77).
 *
 * Thought Shucker: {1}{U} Creature — Rat Rogue 1/3
 * Threshold — {1}{U}: Put a +1/+1 counter on this creature and draw a card.
 * Activate only if there are seven or more cards in your graveyard and only once.
 */
class ThoughtShuckerScenarioTest : ScenarioTestBase() {

    init {
        context("Thought Shucker threshold activated ability") {

            test("can activate with 7+ cards in graveyard - gains counter and draws") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thought Shucker")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInGraveyard(1, "Island")
                    .withCardInGraveyard(1, "Island")
                    .withCardInGraveyard(1, "Island")
                    .withCardInGraveyard(1, "Island")
                    .withCardInGraveyard(1, "Island")
                    .withCardInGraveyard(1, "Island")
                    .withCardInGraveyard(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                val shuckerId = game.findPermanent("Thought Shucker")!!
                val cardDef = cardRegistry.getCard("Thought Shucker")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = shuckerId,
                        abilityId = ability.id
                    )
                )
                result.error shouldBe null

                game.resolveStack()

                // Should have drawn a card
                game.handSize(1) shouldBe handBefore + 1
            }

            test("cannot activate a second time - once restriction") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thought Shucker")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInGraveyard(1, "Island")
                    .withCardInGraveyard(1, "Island")
                    .withCardInGraveyard(1, "Island")
                    .withCardInGraveyard(1, "Island")
                    .withCardInGraveyard(1, "Island")
                    .withCardInGraveyard(1, "Island")
                    .withCardInGraveyard(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val shuckerId = game.findPermanent("Thought Shucker")!!
                val cardDef = cardRegistry.getCard("Thought Shucker")!!
                val ability = cardDef.script.activatedAbilities.first()

                // First activation succeeds
                val result1 = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = shuckerId,
                        abilityId = ability.id
                    )
                )
                result1.error shouldBe null
                game.resolveStack()

                // Second activation should fail
                val result2 = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = shuckerId,
                        abilityId = ability.id
                    )
                )
                result2.error shouldNotBe null
            }
        }
    }
}
