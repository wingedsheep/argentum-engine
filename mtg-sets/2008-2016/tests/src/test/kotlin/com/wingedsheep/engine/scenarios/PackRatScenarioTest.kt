package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Pack Rat (RTR) — P/T each equal to the number of Rats you control. {2}{B}, Discard a card:
 * Create a token that's a copy of this creature.
 */
class PackRatScenarioTest : ScenarioTestBase() {
    init {
        val abilityId = cardRegistry.getCard("Pack Rat")!!.script.activatedAbilities.single().id

        test("counts Rats you control, and the copy ability grows every Pack Rat") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Pack Rat")
                .withCardOnBattlefield(1, "Pack Rat")
                .withCardOnBattlefield(2, "Pack Rat")
                .withCardInHand(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val myRats = game.state.getBattlefield(game.player1Id)
                .filter { game.state.getEntity(it)?.get<CardComponent>()?.name == "Pack Rat" }
            myRats shouldHaveSize 2

            withClue("two Rats you control -> each is 2/2 (the opponent's Rat doesn't count)") {
                myRats.forEach {
                    game.state.projectedState.getPower(it) shouldBe 2
                    game.state.projectedState.getToughness(it) shouldBe 2
                }
            }

            val discard = game.findCardsInHand(1, "Hill Giant").single()
            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = myRats.first(),
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(discardedCards = listOf(discard)),
                )
            )
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            val after = game.state.getBattlefield(game.player1Id)
                .filter { game.state.getEntity(it)?.get<CardComponent>()?.name == "Pack Rat" }
            withClue("a token copy joined, the discard went to the graveyard, and all three are 3/3") {
                after shouldHaveSize 3
                game.isInGraveyard(1, "Hill Giant") shouldBe true
                after.forEach {
                    game.state.projectedState.getPower(it) shouldBe 3
                    game.state.projectedState.getToughness(it) shouldBe 3
                }
            }
        }
    }
}
