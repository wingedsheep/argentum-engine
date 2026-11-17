package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Arashin War Beast (FRF #123) — {5}{G}{G} Creature — Beast 6/6.
 *
 *   Whenever this creature deals combat damage to one or more blocking creatures, manifest the top
 *   card of your library.
 */
class ArashinWarBeastScenarioTest : ScenarioTestBase() {

    private fun TestGame.faceDownCount(): Int =
        state.getBattlefield().count { state.getEntity(it)?.has<FaceDownComponent>() == true }

    init {
        test("blocked by two creatures that die to its damage, it manifests once") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Arashin War Beast", summoningSickness = false)
                .withCardOnBattlefield(2, "Llanowar Elves")
                .withCardOnBattlefield(2, "Llanowar Elves")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Arashin War Beast" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            val beast = game.findPermanent("Arashin War Beast")!!
            val elves = game.findPermanents("Llanowar Elves")
            game.execute(DeclareBlockers(game.player2Id, elves.associateWith { listOf(beast) })).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            if (game.state.pendingDecision != null) game.submitDefaultCombatDamage()
            game.resolveStack()

            withClue("both Elves died, and the War Beast manifested exactly one card") {
                game.findPermanents("Llanowar Elves") shouldHaveSize 0
                game.faceDownCount() shouldBe 1
            }
        }

        test("unblocked, it deals damage only to a player and doesn't manifest") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Arashin War Beast", summoningSickness = false)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Arashin War Beast" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.getLifeTotal(2) shouldBe 14
            game.faceDownCount() shouldBe 0
        }

        test("when it blocks, damage to the attacker isn't damage to a blocking creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Arashin War Beast")
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            val beast = game.findPermanent("Arashin War Beast")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(DeclareBlockers(game.player1Id, mapOf(beast to listOf(bears)))).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.findPermanents("Grizzly Bears") shouldHaveSize 0
            game.faceDownCount() shouldBe 0
        }
    }
}
