package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Militia Rallier (VOW #24) — {2}{W} Creature — Human Soldier, 3/3.
 *
 *   This creature can't attack alone.
 *   Whenever this creature attacks, untap target creature.
 *
 * Exercises the "can't attack alone" restriction (illegal solo, legal with a co-attacker) and the
 * attack trigger that untaps a target creature.
 */
class MilitiaRallierScenarioTest : ScenarioTestBase() {

    init {
        context("Militia Rallier") {

            test("can't attack alone, but can attack alongside another creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Militia Rallier", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                withClue("Militia Rallier can't be the only attacker") {
                    game.declareAttackers(mapOf("Militia Rallier" to 2)).error shouldNotBe null
                }
                withClue("...but can attack alongside another attacker") {
                    game.declareAttackers(mapOf("Militia Rallier" to 2, "Grizzly Bears" to 2)).error shouldBe null
                }
            }

            test("whenever Militia Rallier attacks, untap target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Militia Rallier", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    // A separate, non-attacking tapped creature to serve as the untap target
                    // (an attacking creature must be untapped to be declared, barring vigilance).
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false, tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val rallier = game.findPermanent("Militia Rallier")!!
                val giant = game.findPermanent("Hill Giant")!!

                withClue("Hill Giant starts tapped") {
                    game.state.getEntity(giant)?.has<TappedComponent>() shouldBe true
                }

                game.declareAttackers(mapOf("Militia Rallier" to 2, "Grizzly Bears" to 2)).error shouldBe null
                game.resolveStack() // attack trigger goes on the stack and asks for a target

                game.selectTargets(listOf(giant))
                game.resolveStack()

                withClue("the target creature is untapped by the attack trigger") {
                    game.state.getEntity(giant)?.has<TappedComponent>() shouldBe false
                }
                withClue("Militia Rallier itself is still tapped from attacking") {
                    game.state.getEntity(rallier)?.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
