package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario coverage for Agatha's dynamic, projected-power activated-ability cost reduction. */
class AgathaOfTheVileCauldronScenarioTest : ScenarioTestBase() {

    private val cauldronAssistant = card("Cauldron Assistant") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Human Warlock"
        power = 2
        toughness = 2
        activatedAbility {
            cost = Costs.Mana("{4}")
            effect = Effects.DrawCards(1)
            description = "Draw a card."
        }
    }

    init {
        cardRegistry.register(cauldronAssistant)

        fun abilityAffordable(lands: Int, agathaCounters: Int = 0): Boolean {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Agatha of the Vile Cauldron")
                .withCardOnBattlefield(1, "Cauldron Assistant")
                .withLandsOnBattlefield(1, "Forest", lands)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            if (agathaCounters > 0) {
                val agatha = game.findPermanent("Agatha of the Vile Cauldron")!!
                game.state = game.state.updateEntity(agatha) {
                    it.with(
                        CountersComponent(
                            mapOf(CounterType.PLUS_ONE_PLUS_ONE to agathaCounters)
                        )
                    )
                }
            }

            val assistant = game.findPermanent("Cauldron Assistant")!!
            return game.getLegalActions(1).firstOrNull {
                val action = it.action
                action is ActivateAbility && action.sourceId == assistant
            }?.isAffordable == true
        }

        context("Agatha of the Vile Cauldron") {
            test("reduces creature activated abilities by her current power") {
                withClue("base 1 power reduces {4} to {3}") {
                    abilityAffordable(lands = 3) shouldBe true
                }
                withClue("two +1/+1 counters make Agatha 3 power and reduce {4} to {1}") {
                    abilityAffordable(lands = 1, agathaCounters = 2) shouldBe true
                }
            }

            test("cannot reduce the mana in an activated ability below one") {
                withClue("even a 3-power Agatha leaves the {4} ability costing one mana") {
                    abilityAffordable(lands = 0, agathaCounters = 2) shouldBe false
                }
            }
        }
    }
}
