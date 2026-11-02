package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.ScenarioTestBase.TestGame
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * Vaevictis Asmadi, the Dire (M19 #225): "Whenever Vaevictis Asmadi attacks, for each player,
 * choose target permanent that player controls. Those players sacrifice those permanents. Each
 * player who sacrificed a permanent this way reveals the top card of their library, then puts it
 * onto the battlefield if it's a permanent card."
 *
 * Covers what the snapshot can't see: one target per player (yours included), the sacrifice by
 * each target's controller, the per-player reveal that only puts a *permanent* card onto the
 * battlefield, and the ruling that a player whose target became illegal neither sacrifices nor
 * reveals.
 */
class VaevictisAsmadiTheDireScenarioTest : ScenarioTestBase() {

    private fun board(p1Top: String, p2Top: String, withUnsummon: Boolean = false): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Vaevictis Asmadi, the Dire", summoningSickness = false)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Craw Wurm")
            .withCardInLibrary(1, p1Top)
            .withCardInLibrary(2, p2Top)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        val withSpell = if (withUnsummon) {
            builder.withCardInHand(1, "Unsummon").withLandsOnBattlefield(1, "Island", 1)
        } else builder
        return withSpell.build()
    }

    /** Attack with Vaevictis and choose [targets] for its trigger; returns the offered targets. */
    private fun TestGame.attackAndTarget(targets: List<EntityId>): List<EntityId> {
        advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Vaevictis Asmadi, the Dire" to 2)).error shouldBe null
        val decision = state.pendingDecision as? ChooseTargetsDecision
            ?: error("expected the attack trigger's target decision, got ${state.pendingDecision}")
        val offered = decision.legalTargets[0] ?: emptyList()
        selectTargets(targets).error shouldBe null
        return offered
    }

    init {
        test("each player sacrifices their target; only a permanent card off the top enters") {
            val game = board(p1Top = "Hill Giant", p2Top = "Lightning Bolt")
            val bears = game.findPermanent("Grizzly Bears")!!
            val wurm = game.findPermanent("Craw Wurm")!!
            val vaevictis = game.findPermanent("Vaevictis Asmadi, the Dire")!!

            val offered = game.attackAndTarget(listOf(bears, wurm))
            withClue("your own permanents are offered alongside the opponent's") {
                offered shouldContainAll listOf(bears, wurm, vaevictis)
            }
            game.resolveStack()

            withClue("each target's controller sacrificed it") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Craw Wurm") shouldBe true
            }
            withClue("the revealed permanent card enters under its owner's control") {
                val giant = game.findPermanent("Hill Giant") ?: error("Hill Giant should be on the battlefield")
                game.state.projectedState.getController(giant) shouldBe game.player1Id
            }
            withClue("a revealed nonpermanent card stays on top of its library") {
                game.isOnBattlefield("Lightning Bolt") shouldBe false
                game.findCardsInLibrary(2, "Lightning Bolt").size shouldBe 1
            }
            game.isOnBattlefield("Vaevictis Asmadi, the Dire") shouldBe true
        }

        test("ruling: a player whose target became illegal doesn't sacrifice or reveal") {
            val game = board(p1Top = "Raging Goblin", p2Top = "Hill Giant", withUnsummon = true)
            val bears = game.findPermanent("Grizzly Bears")!!
            val wurm = game.findPermanent("Craw Wurm")!!

            game.attackAndTarget(listOf(bears, wurm))
            withClue("bounce the opponent's target in response") {
                game.castSpell(1, "Unsummon", targetId = wurm).error shouldBe null
            }
            game.resolveStack()

            game.isInHand(2, "Craw Wurm") shouldBe true
            withClue("the player with a legal target still sacrifices and reveals") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Raging Goblin") shouldBe true
            }
            withClue("the player whose target left reveals nothing") {
                game.isOnBattlefield("Hill Giant") shouldBe false
                game.findCardsInLibrary(2, "Hill Giant").size shouldBe 1
            }
        }
    }
}
