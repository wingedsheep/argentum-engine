package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Ainok Strike Leader (TDC #11) — {1}{W} Creature — Dog Warrior 2/2.
 *
 * "Whenever you attack with this creature and/or your commander, for each opponent, create a 1/1
 * red Goblin creature token that's tapped and attacking that player."
 *
 * The attack trigger's attacker filter is "this creature, or a commander you own": attacking with
 * either fires it, attacking with neither doesn't.
 */
class AinokStrikeLeaderScenarioTest : ScenarioTestBase() {

    private fun setup(): TestGame = scenario()
        .withPlayers("Alice", "Bob")
        .withCardOnBattlefield(1, "Ainok Strike Leader", summoningSickness = false)
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        .build()

    private fun TestGame.assertGoblinAttackingBob() {
        val goblins = findPermanents("Goblin Token")
        goblins.size shouldBe 1
        val goblin = goblins.single()
        state.getEntity(goblin)?.has<TappedComponent>() shouldBe true
        state.getEntity(goblin)?.get<AttackingComponent>()?.defenderId shouldBe player2Id
        state.projectedState.getController(goblin) shouldBe player1Id
    }

    init {
        test("attacking with Ainok creates a Goblin tapped and attacking the opponent") {
            val game = setup()
            game.declareAttackers(mapOf("Ainok Strike Leader" to 2)).error shouldBe null
            game.resolveStack()

            game.assertGoblinAttackingBob()
        }

        test("attacking with your commander alone triggers it") {
            val game = setup()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(bears) { it.with(CommanderComponent(ownerId = game.player1Id)) }

            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            game.assertGoblinAttackingBob()
        }

        test("attacking with both still triggers only once") {
            val game = setup()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(bears) { it.with(CommanderComponent(ownerId = game.player1Id)) }

            game.declareAttackers(mapOf("Ainok Strike Leader" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            game.assertGoblinAttackingBob()
        }

        test("attacking with neither Ainok nor a commander doesn't trigger") {
            val game = setup()
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            game.findPermanents("Goblin Token").size shouldBe 0
        }
    }
}
