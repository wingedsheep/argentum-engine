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
 * Bloodbriar (Eldritch Moon #151) — {2}{G} Creature — Plant Elemental, 2/3.
 *
 * "Whenever you sacrifice another permanent, put a +1/+1 counter on this creature."
 *
 * Sacrifices a Blood token (a noncreature permanent) to pay its own ability's cost.
 */
class BloodbriarScenarioTest : ScenarioTestBase() {

    init {
        context("Bloodbriar") {

            test("sacrificing another permanent puts a +1/+1 counter on it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodbriar", summoningSickness = false)
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

                val bloodbriar = game.findPermanent("Bloodbriar")!!
                withClue("the Blood token was sacrificed") {
                    game.findPermanents("Blood").size shouldBe 0
                }
                withClue("Bloodbriar got a +1/+1 counter") {
                    game.state.getEntity(bloodbriar)!!.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                    game.state.projectedState.getPower(bloodbriar) shouldBe 3
                    game.state.projectedState.getToughness(bloodbriar) shouldBe 4
                }
            }
        }
    }
}
