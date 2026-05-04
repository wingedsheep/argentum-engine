package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class TeferiTemporalPilgrimScenarioTest : ScenarioTestBase() {

    private fun TestGame.addLoyalty(cardName: String, loyalty: Int) {
        val id = findPermanent(cardName)!!
        state = state.updateEntity(id) { c ->
            val counters = c.get<CountersComponent>() ?: CountersComponent()
            c.with(counters.withAdded(CounterType.LOYALTY, loyalty))
        }
    }

    private fun TestGame.activateLoyaltyAbility(
        playerNumber: Int,
        cardName: String,
        abilityIndex: Int,
        targetIds: List<EntityId> = emptyList()
    ) {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        val permanentId = findPermanent(cardName)!!
        val cardDef = cardRegistry.getCard(cardName)!!
        val ability = cardDef.script.activatedAbilities[abilityIndex]
        val targets = targetIds.map { ChosenTarget.Permanent(it) }
        val result = execute(
            ActivateAbility(
                playerId = playerId,
                sourceId = permanentId,
                abilityId = ability.id,
                targets = targets
            )
        )
        withClue("Loyalty ability activation should succeed: ${result.error}") {
            result.error shouldBe null
        }
    }

    init {
        context("Teferi draw trigger") {
            test("drawing a card puts a loyalty counter on Teferi") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Teferi, Temporal Pilgrim")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.addLoyalty("Teferi, Temporal Pilgrim", 4)

                // 0: Draw a card. (first activated ability, index 0)
                game.activateLoyaltyAbility(1, "Teferi, Temporal Pilgrim", 0)
                game.resolveStack()
                // Resolve the resulting draw trigger that adds loyalty
                game.resolveStack()

                val teferi = game.findPermanent("Teferi, Temporal Pilgrim")!!
                val counters = game.state.getEntity(teferi)?.get<CountersComponent>()
                withClue("Teferi should have 4 base loyalty + 1 from draw trigger - 0 (from 0-cost) = 5") {
                    counters shouldNotBe null
                    counters!!.getCount(CounterType.LOYALTY) shouldBe 5
                }
            }
        }

        context("Teferi draw trigger fires per draw") {
            test("Touch of Brilliance (draw 2) puts two loyalty counters on Teferi") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Teferi, Temporal Pilgrim")
                    .withCardInHand(1, "Touch of Brilliance")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.addLoyalty("Teferi, Temporal Pilgrim", 4)

                game.castSpell(1, "Touch of Brilliance")
                // Resolve the spell (draws 2 cards, queueing 2 loyalty triggers)
                game.resolveStack()
                // Resolve each draw trigger
                game.resolveStack()
                game.resolveStack()

                val teferi = game.findPermanent("Teferi, Temporal Pilgrim")!!
                val counters = game.state.getEntity(teferi)?.get<CountersComponent>()
                withClue("Teferi should gain 2 loyalty counters (one per draw): 4 base + 2 = 6") {
                    counters shouldNotBe null
                    counters!!.getCount(CounterType.LOYALTY) shouldBe 6
                }
            }
        }

        context("Teferi -2: create Spirit token") {
            test("creates a 2/2 blue Spirit token with vigilance") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Teferi, Temporal Pilgrim")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.addLoyalty("Teferi, Temporal Pilgrim", 4)

                // -2 is the second loyalty ability (index 1)
                game.activateLoyaltyAbility(1, "Teferi, Temporal Pilgrim", 1)
                game.resolveStack()

                val spirit = game.findPermanent("Spirit Token")
                withClue("Should have created a Spirit token") {
                    spirit shouldNotBe null
                }
                val projected = game.state.projectedState
                withClue("Spirit token should be 2/2") {
                    projected.getPower(spirit!!) shouldBe 2
                    projected.getToughness(spirit) shouldBe 2
                }
            }
        }

    }
}
