package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * The "one or more [creatures] you control deal combat damage to a player" batch trigger
 * (`OneOrMoreDealCombatDamageToPlayerEvent`) counts battles only when it says so: its `orBattle`
 * reading is Zurgo and Ojutai's (see that card's test); the plain reading must still ignore combat
 * damage dealt to a battle. Kastral, the Windcrested is the plain-reading witness.
 */
class CombatDamageBatchToBattleTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Kastral, the Windcrested", summoningSickness = false)
        .withCardOnBattlefield(1, "Invasion of Innistrad") // protected by the opponent
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
        .also { it.checkStateBasedActions() }

    init {
        test("\"to a player\" does not fire on combat damage to a battle") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackersWithPermanentTargets(
                permanentAttackers = mapOf("Kastral, the Windcrested" to "Invasion of Innistrad")
            ).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            withClue("Kastral hit the Siege and nothing triggered") {
                game.getLifeTotal(2) shouldBe 20
                game.state.stack.size shouldBe 0
                game.state.pendingDecision shouldBe null
            }
        }

        test("\"to a player\" still fires on combat damage to a player") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Kastral, the Windcrested" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.getLifeTotal(2) shouldBe 16
            withClue("Kastral's trigger is waiting on the stack or on its mode choice") {
                (game.state.stack.isNotEmpty() || game.state.pendingDecision != null) shouldBe true
            }
        }
    }
}
