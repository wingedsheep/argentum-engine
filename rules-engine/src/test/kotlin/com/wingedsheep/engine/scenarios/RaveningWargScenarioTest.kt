package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ravening Warg (HOB) — {1}{B} Creature — Wolf 2/2.
 *
 * "Deathtouch
 *  Ferocious — Whenever this creature attacks while you control a creature with power 4 or
 *  greater, you gain 2 life."
 *
 * The ferocious intervening-if clause is the whole card: attacking is not enough, and the
 * power-4 creature has to be one *you* control.
 */
class RaveningWargScenarioTest : ScenarioTestBase() {

    init {
        context("Ravening Warg") {

            test("it has deathtouch") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ravening Warg")
                    .build()

                val warg = game.findPermanent("Ravening Warg")!!
                game.state.projectedState.hasKeyword(warg, Keyword.DEATHTOUCH) shouldBe true
            }

            test("attacking with a power-4 creature on your side gains 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ravening Warg")
                    // Force of Nature is a 5/5 — satisfies "power 4 or greater".
                    .withCardOnBattlefield(1, "Force of Nature")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.getLifeTotal(1) shouldBe 20
                game.declareAttackers(mapOf("Ravening Warg" to 2)).error shouldBe null
                game.resolveStack()

                withClue("ferocious is satisfied, so the attack trigger gained 2 life") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }

            test("attacking without a power-4 creature gains nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ravening Warg")
                    // Centaur Courser is a 3/3 — below the ferocious threshold.
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Ravening Warg" to 2)).error shouldBe null
                game.resolveStack()

                withClue("power 3 does not meet 'power 4 or greater'") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("an opponent's power-4 creature does not satisfy ferocious") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ravening Warg")
                    .withCardOnBattlefield(2, "Force of Nature")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Ravening Warg" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the condition reads 'you control'") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
