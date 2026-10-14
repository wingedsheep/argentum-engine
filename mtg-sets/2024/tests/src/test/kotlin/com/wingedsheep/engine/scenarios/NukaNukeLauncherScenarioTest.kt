package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class NukaNukeLauncherScenarioTest : ScenarioTestBase() {
    private val freeSpell = card("Nuka Test Free Spell") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.GainLife(1) }
    }

    private fun TestGame.rads(playerId: com.wingedsheep.sdk.model.EntityId): Int =
        state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.RAD) ?: 0

    private fun TestGame.castFree(player: Int) {
        castSpell(player, freeSpell.name).error shouldBe null
        resolveStack()
    }

    // Player 1's Grizzly Bears wears the Launcher; both libraries are all lands, so radiation's
    // precombat-main mill never removes a rad counter and the totals stay readable.
    private fun board(): TestGame {
        val b = scenario().withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardAttachedTo(1, "Nuka-Nuke Launcher", "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(4) { b.withCardInHand(1, freeSpell.name); b.withCardInHand(2, freeSpell.name) }
        repeat(20) { b.withCardInLibrary(1, "Forest"); b.withCardInLibrary(2, "Forest") }
        return b.build()
    }

    private fun TestGame.attackWithBears() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
        resolveStack()
    }

    init {
        cardRegistry.register(freeSpell)

        test("equipped creature gets +3/+0 and has intimidate") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(bears) shouldBe 5
            game.state.projectedState.hasKeyword(bears, Keyword.INTIMIDATE) shouldBe true
        }

        test("after the attack, the defending player gets two rad counters per spell they cast; the attacker doesn't") {
            val game = board()
            game.attackWithBears()
            game.state.delayedTriggers.size shouldBe 1

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.castFree(1)
            game.rads(game.player1Id) shouldBe 0
            game.rads(game.player2Id) shouldBe 0
            // The rest of the attack turn is inside the window too.
            game.passPriority()
            game.castFree(2)
            game.rads(game.player2Id) shouldBe 2

            // Defending player's next turn: each of their spells gives them two rad counters.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            game.castFree(2)
            game.rads(game.player2Id) shouldBe 4
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.castFree(2)
            game.rads(game.player2Id) shouldBe 6
        }

        test("the watcher ends with the cleanup of the defending player's next turn") {
            val game = board()
            game.attackWithBears()

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.state.activePlayerId shouldBe game.player1Id
            game.state.delayedTriggers.size shouldBe 1
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            game.state.delayedTriggers.size shouldBe 1

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.state.delayedTriggers.size shouldBe 1
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.state.delayedTriggers.shouldBeEmpty()
        }
    }
}
