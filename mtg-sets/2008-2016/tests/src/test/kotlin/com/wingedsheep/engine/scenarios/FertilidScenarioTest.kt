package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Fertilid (MOR #122) — {2}{G} 0/0 Elemental.
 *
 *   This creature enters with two +1/+1 counters on it.
 *   {1}{G}, Remove a +1/+1 counter from this creature: Target player searches their library for a
 *   basic land card, puts it onto the battlefield tapped, then shuffles.
 *
 * The interesting part is the searcher: the *targeted* player searches their own library, from an
 * activated ability (the `ForEachPlayer` rebinding of `Player.You`).
 */
class FertilidScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Fertilid") {

            test("enters with two counters; targeted opponent searches their own library for a tapped basic") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Fertilid")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Hill Giant")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("cast should succeed") { game.castSpell(1, "Fertilid").error shouldBe null }
                game.resolveStack()

                val fertilid = game.findPermanent("Fertilid")!!
                game.plusOneCounters(fertilid) shouldBe 2
                game.state.projectedState.getPower(fertilid) shouldBe 2
                game.state.projectedState.getToughness(fertilid) shouldBe 2

                val ability = cardRegistry.getCard("Fertilid")!!.script.activatedAbilities[0]
                val bobForest = game.findCardsInLibrary(2, "Forest").single()
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = fertilid,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                withClue("a +1/+1 counter is removed as a cost") { game.plusOneCounters(fertilid) shouldBe 1 }

                game.resolveStack()
                withClue("the targeted player, not the controller, makes the search choice") {
                    game.getPendingDecision()!!.playerId shouldBe game.player2Id
                }
                game.selectCards(listOf(bobForest))
                game.resolveStack()

                withClue("the land enters tapped under Bob's control") {
                    game.state.getBattlefield().contains(bobForest) shouldBe true
                    game.state.getEntity(bobForest)!!.get<ControllerComponent>()!!.playerId shouldBe game.player2Id
                    game.state.getEntity(bobForest)!!.has<TappedComponent>() shouldBe true
                }
                withClue("Alice's library is untouched") { game.librarySize(1) shouldBe 1 }
                game.librarySize(2) shouldBe 1
            }
        }
    }
}
