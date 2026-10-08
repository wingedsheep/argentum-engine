package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gixian Infiltrator (BRO #98) — {1}{B} Creature — Phyrexian Human, 2/1.
 *
 * "Whenever you sacrifice another permanent, put a +1/+1 counter on this creature."
 *
 * Sacrifices a Blood token (a noncreature permanent) to pay its own ability's cost.
 */
class GixianInfiltratorScenarioTest : ScenarioTestBase() {

    init {
        context("Gixian Infiltrator") {

            test("sacrificing another permanent puts a +1/+1 counter on it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Gixian Infiltrator", summoningSickness = false)
                    .withCardOnBattlefield(1, "Blood", isToken = true)
                    .withCardInHand(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val blood = game.findPermanent("Blood")!!
                val toDiscard = game.findCardsInHand(1, "Swamp").first()
                val bloodAbilityId = cardRegistry.getCard("Blood")!!.activatedAbilities.first().id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = blood,
                        abilityId = bloodAbilityId,
                        costPayment = AdditionalCostPayment(discardedCards = listOf(toDiscard))
                    )
                ).error shouldBe null
                game.resolveStack()

                val infiltrator = game.findPermanent("Gixian Infiltrator")!!
                withClue("the Blood token was sacrificed") {
                    game.findPermanents("Blood").size shouldBe 0
                }
                withClue("Gixian Infiltrator got a +1/+1 counter") {
                    game.state.getEntity(infiltrator)!!.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                    game.state.projectedState.getPower(infiltrator) shouldBe 3
                }
            }
        }
    }
}
