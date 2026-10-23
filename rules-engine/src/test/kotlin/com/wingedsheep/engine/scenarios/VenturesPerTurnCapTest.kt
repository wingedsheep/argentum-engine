package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.dungeon.DungeonService
import com.wingedsheep.engine.state.components.player.VenturesThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dungeons.Dungeons
import com.wingedsheep.sdk.scripting.RestrictVenturesPerTurn
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * [RestrictVenturesPerTurn] — "each opponent can't venture into the dungeon more than once each
 * turn" (Keen-Eared Sentry; venture is CR 701.49).
 *
 * | Rule / ruling | Covered by |
 * |---|---|
 * | a forbidden venture doesn't happen: no marker move, no room trigger | "a second venture in one turn doesn't happen" |
 * | `affected` is relative to the cap's controller | "the cap's controller is unaffected" |
 * | ventures earlier in the turn count, even before the cap existed | "ventures before the cap entered count" |
 * | the smallest applicable cap wins | "the smallest cap wins" |
 * | the initiative's venture into Undercity is a venture (CR 726.2, 701.49d) | "the initiative's venture is capped too" |
 * | the tally resets every turn | "the allowance resets each turn" |
 */
class VenturesPerTurnCapTest : ScenarioTestBase() {

    private val opponentsCap = card("Test Opponents Venture Cap") {
        typeLine = "Artifact"
        staticAbility { ability = RestrictVenturesPerTurn(Player.EachOpponent, maxPerTurn = 1) }
    }
    private val selfCapTwo = card("Test Self Venture Cap Two") {
        typeLine = "Artifact"
        staticAbility { ability = RestrictVenturesPerTurn(Player.You, maxPerTurn = 2) }
    }
    private val selfCapOne = card("Test Self Venture Cap One") {
        manaCost = "{0}"
        typeLine = "Artifact"
        staticAbility { ability = RestrictVenturesPerTurn(Player.You, maxPerTurn = 1) }
    }
    private val everyoneCap = card("Test Everyone Venture Cap") {
        typeLine = "Artifact"
        staticAbility { ability = RestrictVenturesPerTurn(Player.Each, maxPerTurn = 1) }
    }
    private val ventureOnce = card("Test Venture Once") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.VentureIntoTheDungeon(into = Dungeons.DungeonOfTheMadMage.name) }
    }
    private val ventureTwice = card("Test Venture Twice") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val venture = Effects.VentureIntoTheDungeon(into = Dungeons.DungeonOfTheMadMage.name)
            effect = venture then venture
        }
    }
    private val ventureThrice = card("Test Venture Thrice") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val venture = Effects.VentureIntoTheDungeon(into = Dungeons.DungeonOfTheMadMage.name)
            effect = venture then venture then venture
        }
    }
    private val takeIt = card("Test Venture Cap Initiative") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.TakeTheInitiative() }
    }

    init {
        listOf(opponentsCap, selfCapTwo, selfCapOne, everyoneCap, ventureOnce, ventureTwice, ventureThrice, takeIt)
            .forEach { cardRegistry.register(it) }

        test("a second venture in one turn doesn't happen") {
            val game = game { withCardOnBattlefield(2, opponentsCap.name); withCardInHand(1, ventureThrice.name) }
            game.cast(ventureThrice.name)

            withClue("Only the first venture entered Yawning Portal (gain 1 life)") {
                DungeonService.marker(game.state, game.player1Id)!!.room shouldBe 0
                game.getLifeTotal(1) shouldBe 21
                game.ventures(1) shouldBe 1
            }
        }

        test("the cap's controller is unaffected") {
            val game = game { withCardOnBattlefield(1, opponentsCap.name); withCardInHand(1, ventureTwice.name) }
            game.cast(ventureTwice.name)

            withClue("Yawning Portal → Dungeon Level (its scry may still be pending)") {
                DungeonService.marker(game.state, game.player1Id)!!.room shouldBe 1
                game.ventures(1) shouldBe 2
            }
        }

        test("ventures before the cap entered count") {
            val game = game {
                withCardInHand(1, ventureOnce.name)
                withCardInHand(1, selfCapOne.name)
                withCardInHand(1, ventureThrice.name)
            }
            game.cast(ventureOnce.name)
            game.cast(selfCapOne.name)

            game.cast(ventureThrice.name)

            DungeonService.marker(game.state, game.player1Id)!!.room shouldBe 0
            game.getLifeTotal(1) shouldBe 21
        }

        test("the smallest cap wins") {
            val game = game {
                withCardOnBattlefield(1, selfCapTwo.name)
                withCardOnBattlefield(2, opponentsCap.name)
                withCardInHand(1, ventureThrice.name)
            }
            game.cast(ventureThrice.name)

            DungeonService.marker(game.state, game.player1Id)!!.room shouldBe 0
        }

        test("the initiative's venture is capped too") {
            val game = game {
                withCardOnBattlefield(2, opponentsCap.name)
                withCardInHand(1, ventureOnce.name)
                withCardInHand(1, takeIt.name)
            }
            game.cast(ventureOnce.name)
            game.cast(takeIt.name)

            withClue("Player 1 takes the initiative but doesn't venture into Undercity") {
                game.state.initiative shouldBe game.player1Id
                DungeonService.marker(game.state, game.player1Id)!!.dungeon shouldBe Dungeons.DungeonOfTheMadMage.name
                DungeonService.marker(game.state, game.player1Id)!!.room shouldBe 0
            }
        }

        test("the allowance resets each turn") {
            val game = game { withCardOnBattlefield(1, everyoneCap.name); withCardInHand(1, ventureOnce.name) }
            game.cast(ventureOnce.name)
            game.ventures(1) shouldBe 1

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            withClue("The turn change cleared player 1's tally") { game.ventures(1) shouldBe 0 }
        }
    }

    private fun TestGame.ventures(player: Int): Int =
        state.getEntity(if (player == 1) player1Id else player2Id)?.get<VenturesThisTurnComponent>()?.count ?: 0

    /** Cast [name] and resolve the stack, stopping at the first decision. */
    private fun TestGame.cast(name: String) {
        castSpell(1, name).error shouldBe null
        resolveStack()
    }

    private fun game(extra: ScenarioBuilder.() -> Unit): TestGame {
        val builder = scenario().withPlayers("Player", "Opponent")
        repeat(5) {
            builder.withCardInLibrary(1, "Plains")
            builder.withCardInLibrary(2, "Plains")
        }
        builder.extra()
        return builder.withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
    }
}
