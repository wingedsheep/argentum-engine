package com.wingedsheep.engine.legalactions.support

import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.model.EntityId

/**
 * Test driver for legal action enumeration.
 *
 * Wraps [GameTestDriver] (for setting up game state) with direct access to
 * [LegalActionEnumerator] (for asking "what can this player do now?").
 *
 * ## Philosophy
 * Tests drive the public coordinator [LegalActionEnumerator.enumerate], not individual
 * enumerators. This exercises the real wiring (EnumerationContext, combat-step
 * short-circuit, mana solver caching) and survives refactors that move logic
 * between enumerators.
 *
 * ## Usage
 * ```kotlin
 * val driver = EnumerationTestDriver()
 * driver.registerCards(TestCards.all)
 * driver.initMirrorMatch(Deck.of("Forest" to 20, "Grizzly Bears" to 20))
 *
 * val actions = driver.enumerateFor(driver.player1)
 * actions shouldContainCastOf "Grizzly Bears"
 * ```
 */
class EnumerationTestDriver {
    val game: GameTestDriver = GameTestDriver()

    private val enumerator: LegalActionEnumerator by lazy {
        LegalActionEnumerator.create(game.cardRegistry)
    }

    // Forward common GameTestDriver setup methods so tests don't need to
    // reach through `.game` for the most common operations.
    fun registerCards(cards: Iterable<com.wingedsheep.sdk.model.CardDefinition>) =
        game.registerCards(cards)

    val player1: EntityId get() = game.player1
    val player2: EntityId get() = game.player2

    /**
     * Enumerate legal actions for the given player at the current state.
     *
     * Defaults to [EnumerationMode.FULL] so auto-tap previews are computed —
     * tests that care about the preview can assert on it; tests that don't
     * are unaffected.
     */
    fun enumerateFor(
        playerId: EntityId,
        mode: EnumerationMode = EnumerationMode.FULL
    ): LegalActionsView = LegalActionsView(
        actions = enumerator.enumerate(game.state, playerId, mode),
        state = game.state
    )

    /** Enumerate for the player who currently has priority. */
    fun enumerateForPriorityPlayer(mode: EnumerationMode = EnumerationMode.FULL): LegalActionsView {
        val pid = game.state.priorityPlayerId
            ?: error("No player has priority at step ${game.state.step}")
        return enumerateFor(pid, mode)
    }
}
