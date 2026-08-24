package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Air Bladder — "Enchanted creature has flying. Enchanted creature can block only creatures with
 * flying."
 *
 * The block restriction used to take `CanOnlyBlockCreaturesWith`'s default `filter`, which is
 * `GroupFilter.source()` — the *Aura*. `GrantKeyword` and `ModifyStats` beside it default to
 * `attachedCreature()`, so the generated card looked symmetric and restricted the wrong permanent.
 */
class AirBladderScenarioTest : ScenarioTestBase() {

    init {
        context("the enchanted creature can block only flyers") {

            test("it cannot block a ground attacker") {
                val game = combatScenario()

                withClue("a creature enchanted by Air Bladder must not block a creature without flying") {
                    game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldNotBe null
                }
            }

            test("it can block a flyer") {
                val game = combatScenario(attacker = "Storm Crow")

                withClue("a creature enchanted by Air Bladder must still block a flyer") {
                    game.declareBlockers(mapOf("Hill Giant" to listOf("Storm Crow"))).error shouldBe null
                }
            }
        }
    }

    private fun combatScenario(attacker: String = "Grizzly Bears") = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, attacker, summoningSickness = false)
        .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
        .withCardAttachedTo(2, "Air Bladder", "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
        .build()
        .also { game ->
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf(attacker to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        }
}
