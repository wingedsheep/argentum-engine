package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AttackersDeclaredEvent
import com.wingedsheep.engine.state.components.combat.PlayerAttackersThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * The "attacking a battle" vocabulary: `AttackPredicate.DefenderIsBattle`,
 * `StatePredicate.IsAttackingABattle` and `StatePredicate.AttackedABattleThisTurn`. CR 508.1 fixes
 * each attacker's defender — a player, planeswalker, or battle — at declaration, so the engine
 * stamps the battle attackers on `AttackersDeclaredEvent` and on the controller's per-turn record.
 */
class AttackingABattleTest : ScenarioTestBase() {

    init {
        test("declaration stamps only the battle attacker, and the per-turn record clears at cleanup") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                .withCardOnBattlefield(1, "Craw Wurm", summoningSickness = false)
                .withCardOnBattlefield(1, "Invasion of Innistrad") // protected by the opponent
                .withCardOnBattlefield(2, "Jace Beleren")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.checkStateBasedActions()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val wurm = game.findPermanent("Craw Wurm")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            val result = game.declareAttackersWithPermanentTargets(
                playerAttackers = mapOf("Hill Giant" to 2),
                permanentAttackers = mapOf("Grizzly Bears" to "Invasion of Innistrad", "Craw Wurm" to "Jace Beleren")
            )
            result.error shouldBe null

            val declared = result.events.filterIsInstance<AttackersDeclaredEvent>().single()
            withClue("battle vs player vs planeswalker are told apart") {
                declared.attackersAgainstBattle shouldContainExactly setOf(bears)
                declared.attackersAgainstPlayer shouldContainExactly setOf(giant)
                (wurm in declared.attackersAgainstBattle) shouldBe false
            }

            fun record() = game.state.getEntity(game.player1Id)?.get<PlayerAttackersThisTurnComponent>()
            record()!!.battleAttackerIds shouldContainExactly setOf(bears)
            record()!!.attackerIds shouldContainExactly setOf(bears, giant, wurm)

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("cleared at end of turn") { record() shouldBe null }
        }
    }
}
