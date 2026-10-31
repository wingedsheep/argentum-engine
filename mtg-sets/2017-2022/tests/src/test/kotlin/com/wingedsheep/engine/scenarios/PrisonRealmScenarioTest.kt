package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OrderTriggersDecision
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TriggerOrderResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Prison Realm (WAR #26) — {2}{W} Enchantment.
 *
 *   When this enchantment enters, exile target creature or planeswalker an opponent controls
 *   until this enchantment leaves the battlefield.
 *   When this enchantment enters, scry 1.
 */
class PrisonRealmScenarioTest : ScenarioTestBase() {

    /** Resolve everything, aiming the exile trigger at [target]; returns the legal exile targets offered. */
    private fun TestGame.drive(target: EntityId): Set<EntityId> {
        var offered = emptySet<EntityId>()
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 30) {
            when (val d = state.pendingDecision) {
                null -> passPriority()
                is ChooseTargetsDecision -> {
                    offered = d.legalTargets.values.flatten().toSet()
                    selectTargets(listOf(target))
                }
                is OrderTriggersDecision -> submitDecision(TriggerOrderResponse(d.id, d.triggers.indices.toList()))
                is SelectCardsDecision -> skipSelection()
                is ReorderLibraryDecision -> keepLibraryOrder()
                else -> error("Unexpected decision ${d::class.simpleName}")
            }
        }
        return offered
    }

    init {
        test("exiles an opponent's creature until it leaves, and scries 1") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Prison Realm")
                .withCardInHand(1, "Naturalize")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Prison Realm").error shouldBe null
            val offered = game.drive(giant)

            offered shouldBe setOf(giant)
            game.isOnBattlefield("Hill Giant") shouldBe false
            game.isInExile(2, "Hill Giant") shouldBe true
            game.librarySize(1) shouldBe 2

            val realm = game.findPermanent("Prison Realm")!!
            game.castSpell(1, "Naturalize", realm).error shouldBe null
            game.drive(giant)

            game.isInGraveyard(1, "Prison Realm") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
        }
    }
}
