package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Nature's Will (CHK #230) — {2}{G}{G} Enchantment.
 *
 * "Whenever one or more creatures you control deal combat damage to a player, tap all lands that
 * player controls and untap all lands you control."
 *
 * Batch trigger (ruling 2004-12-01): once per combat-damage event, not once per creature.
 */
class NaturesWillScenarioTest : ScenarioTestBase() {

    private fun TestGame.tapped(id: EntityId) = state.getEntity(id)?.has<TappedComponent>() == true

    init {
        test("unblocked attackers: tap the damaged player's lands, untap yours — one trigger") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Nature's Will")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                .withCardOnBattlefield(1, "Forest", tapped = true)
                .withCardOnBattlefield(1, "Forest", tapped = true)
                .withLandsOnBattlefield(2, "Mountain", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            if (game.state.pendingDecision != null) game.submitDefaultCombatDamage()

            withClue("two creatures connecting with one player make a single trigger") {
                game.state.stack.size shouldBe 1
            }
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 20 - 5
            game.findAllPermanents("Mountain").forEach { game.tapped(it) shouldBe true }
            game.findAllPermanents("Forest").forEach { game.tapped(it) shouldBe false }
        }

        test("no combat damage to a player means no trigger") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Nature's Will")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Forest", tapped = true)
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(2, "Mountain", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            if (game.state.pendingDecision != null) game.submitDefaultCombatDamage()
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 20
            game.findAllPermanents("Mountain").forEach { game.tapped(it) shouldBe false }
            game.findAllPermanents("Forest").forEach { game.tapped(it) shouldBe true }
        }
    }
}
