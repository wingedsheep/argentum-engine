package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Witch-king, Bringer of Ruin — "Whenever Witch-king attacks, defending player sacrifices a
 * creature with the least power among creatures they control." Exercises the new
 * `StatePredicate.HasLeastPower` filter: only the defending player's lowest-power creature is a
 * legal sacrifice, so with a unique minimum the edict auto-sacrifices it.
 */
class WitchKingBringerOfRuinScenarioTest : ScenarioTestBase() {

    init {
        test("on attack, defending player sacrifices their least-power creature") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Witch-king, Bringer of Ruin")  // 5/3 flying
                .withCardOnBattlefield(2, "Savannah Lions")                // 1/1 — least power
                .withCardOnBattlefield(2, "Centaur Courser")               // 2/3
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Witch-king, Bringer of Ruin" to 2))
            // The attack trigger goes on the stack; resolving it runs the edict. Only the 1/1 has
            // the least power, so it's the sole legal sacrifice and is taken automatically.
            game.resolveStack()

            game.isOnBattlefield("Savannah Lions") shouldBe false
            game.isOnBattlefield("Centaur Courser") shouldBe true
        }

        test("higher-power creature is not a legal sacrifice when least-power creatures tie") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Witch-king, Bringer of Ruin")  // 5/3 flying
                .withCardOnBattlefield(2, "Savannah Lions")                // 1/1 — least power
                .withCardOnBattlefield(2, "Mons's Goblin Raiders")         // 1/1 — also least power
                .withCardOnBattlefield(2, "Grizzly Bears")                 // 2/2 — higher power
                .withCardOnBattlefield(2, "Centaur Courser")               // 2/3 — higher power
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Witch-king, Bringer of Ruin" to 2))
            game.resolveStack()

            // Two creatures tie for least power (both 1/1), so the defending player must choose
            // between them. The higher-power creatures must NOT be offered as legal sacrifices.
            game.hasPendingDecision() shouldBe true
            val decision = game.getPendingDecision() as SelectCardsDecision
            val legalNames = decision.options.map { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name
            }.toSet()
            legalNames shouldBe setOf("Savannah Lions", "Mons's Goblin Raiders")

            val grizzly = game.findPermanent("Grizzly Bears")!!
            (grizzly in decision.options) shouldBe false

            // Sacrifice the Lions; the higher-power creatures and the other 1/1 survive.
            game.selectCards(listOf(game.findPermanent("Savannah Lions")!!))

            game.isOnBattlefield("Savannah Lions") shouldBe false
            game.isOnBattlefield("Mons's Goblin Raiders") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Centaur Courser") shouldBe true
        }
    }
}
