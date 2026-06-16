package com.wingedsheep.engine.view

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.Concede
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `ClientPlayer.hasLost` must flag a player the moment the engine marks them lost — in a
 * multiplayer pod the game continues after an elimination (CR 800.4a), and the opponent-rail
 * tombstone keys off this flag. Before Phase 4 it was only derived at game end, so a mid-game
 * concede in a 4-player game never rendered as eliminated.
 */
class MidGameEliminationVisibilityTest : FunSpec({

    val bear = CardDefinition.creature(
        name = "Elimination Test Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    fun registry(): CardRegistry = CardRegistry().also { it.register(bear) }

    fun initGame(registry: CardRegistry, playerCount: Int): Pair<GameState, List<EntityId>> {
        val deck = Deck(cards = List(40) { "Elimination Test Bear" })
        val result = GameInitializer(registry).initializeGame(
            GameConfig(
                players = (1..playerCount).map { PlayerConfig("Player $it", deck, 20) },
                skipMulligans = true,
                startingPlayerIndex = 0
            )
        )
        return result.state to result.playerIds
    }

    test("mid-game elimination in a 3-player pod sets hasLost while the game continues") {
        val registry = registry()
        val (state, players) = initGame(registry, 3)
        val processor = ActionProcessor(registry)

        val afterConcede = processor.process(state, Concede(players[1])).result.state
        afterConcede.gameOver shouldBe false

        val transformer = ClientStateTransformer(registry)
        val view = transformer.transform(afterConcede, players[0])
        view.players.first { it.playerId == players[1] }.hasLost shouldBe true
        view.players.first { it.playerId == players[0] }.hasLost shouldBe false
        view.players.first { it.playerId == players[2] }.hasLost shouldBe false
    }

    test("two-player degenerate case: conceding ends the game and flags only the loser") {
        val registry = registry()
        val (state, players) = initGame(registry, 2)
        val processor = ActionProcessor(registry)

        val afterConcede = processor.process(state, Concede(players[1])).result.state
        afterConcede.gameOver shouldBe true
        afterConcede.winnerId shouldBe players[0]

        val transformer = ClientStateTransformer(registry)
        val view = transformer.transform(afterConcede, players[0])
        view.players.first { it.playerId == players[1] }.hasLost shouldBe true
        view.players.first { it.playerId == players[0] }.hasLost shouldBe false
    }
})
