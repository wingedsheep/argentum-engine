package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bloodthirsty Aerialist ({1}{B}{B}, 2/3 flier):
 * "Whenever you gain life, put a +1/+1 counter on this creature."
 */
class BloodthirstyAerialistScenarioTest : ScenarioTestBase() {

    // A {0} sorcery that makes its controller gain 3 life — a clean way to fire a life-gain event.
    private val gainThreeLife = card("Gain Three Life") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "You gain 3 life."
        spell {
            effect = Effects.GainLife(3)
        }
    }

    init {
        cardRegistry.register(gainThreeLife)

        context("Bloodthirsty Aerialist") {

            test("gaining life puts one +1/+1 counter on it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodthirsty Aerialist")
                    .withCardInHand(1, "Gain Three Life")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val aerialist = game.findPermanent("Bloodthirsty Aerialist")!!
                game.state.projectedState.getPower(aerialist) shouldBe 2

                game.castSpell(1, "Gain Three Life").error shouldBe null
                game.resolveStack()

                withClue("one life-gain event → one counter → 3/4") {
                    game.getLifeTotal(1) shouldBe 23
                    game.state.projectedState.getPower(aerialist) shouldBe 3
                    game.state.projectedState.getToughness(aerialist) shouldBe 4
                }
            }

            test("an opponent gaining life does not grow it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodthirsty Aerialist")
                    .withCardInHand(2, "Gain Three Life")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val aerialist = game.findPermanent("Bloodthirsty Aerialist")!!
                game.castSpell(2, "Gain Three Life").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 23
                game.state.projectedState.getPower(aerialist) shouldBe 2
            }
        }
    }
}
