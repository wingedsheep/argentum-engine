package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * My Precious — "Equipped creature has hexproof and can't be blocked."
 *
 * Same bug as Cloak of Mists and Whispersilk Cloak: the evasion half was
 * `flags(AbilityFlag.CANT_BE_BLOCKED)`, which lands on the Equipment rather than on the creature it
 * equips, while the hexproof half was a proper `GrantKeyword` static.
 */
class MyPreciousScenarioTest : ScenarioTestBase() {

    init {
        context("the equipped creature cannot be blocked") {

            test("no creature may block the equipped attacker") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardAttachedTo(1, "My Precious", "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                    .build()
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("My Precious must make the creature it equips unblockable") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldNotBe null
                }
            }
        }
    }
}
