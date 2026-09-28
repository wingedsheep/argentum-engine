package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.player.SkipCombatPhasesComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class YuanShaosInfantryScenarioTest : ScenarioTestBase() {
    init {
        context("Yuan Shao's Infantry") {
            test("attacking alone makes it unblockable this combat") {
                val game = scenario()
                    .withPlayers("A", "B")
                    .withCardOnBattlefield(1, "Yuan Shao's Infantry")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Yuan Shao's Infantry" to 2)).error shouldBe null
                game.resolveStack()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                val r = game.declareBlockers(mapOf("Grizzly Bears" to listOf("Yuan Shao's Infantry")))
                withClue("blocking an unblockable attacker must be rejected") { r.error shouldNotBe null }
            }

            test("attacking with another creature leaves it blockable") {
                val game = scenario()
                    .withPlayers("A", "B")
                    .withCardOnBattlefield(1, "Yuan Shao's Infantry")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Yuan Shao's Infantry" to 2, "Hill Giant" to 2)).error shouldBe null
                game.resolveStack()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Yuan Shao's Infantry"))).error shouldBe null
            }
        }
    }
}
