package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.state.components.combat.MustAttackNextCombatComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sizzling Soloist (SNC #123) — {3}{R} Creature — Human Citizen 3/2
 *
 *   Alliance — Whenever another creature you control enters, target creature an opponent controls
 *   can't block this turn. If this is the second time this ability has resolved this turn, that
 *   creature attacks during its controller's next combat phase if able.
 */
class SizzlingSoloistScenarioTest : ScenarioTestBase() {

    init {
        context("Sizzling Soloist") {

            test("the second resolution this turn makes the target attack in its controller's next combat") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Sizzling Soloist")
                    .withCardInHand(1, "Raging Goblin")
                    .withCardInHand(1, "Raging Goblin")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val giant = game.findPermanent("Hill Giant")!!

                fun castGoblinAndResolve() {
                    game.castSpell(1, "Raging Goblin").error shouldBe null
                    game.resolveStack()
                    if (game.hasPendingDecision()) game.selectTargets(listOf(giant))
                    game.resolveStack()
                }

                castGoblinAndResolve()
                withClue("the first resolution only stops it blocking") {
                    game.state.getEntity(giant)?.get<MustAttackNextCombatComponent>() shouldBe null
                }
                castGoblinAndResolve()
                game.state.getEntity(giant)?.get<MustAttackNextCombatComponent>() shouldNotBe null

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.state.activePlayerId shouldBe game.player2Id
                withClue("the Giant must attack on its controller's turn") {
                    game.execute(DeclareAttackers(game.player2Id, emptyMap())).error shouldNotBe null
                }
                game.execute(DeclareAttackers(game.player2Id, mapOf(giant to game.player1Id))).error shouldBe null
            }
        }
    }
}
