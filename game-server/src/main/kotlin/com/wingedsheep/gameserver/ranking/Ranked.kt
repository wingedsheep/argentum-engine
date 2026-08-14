package com.wingedsheep.gameserver.ranking

import com.wingedsheep.gameserver.lobby.TournamentFormat
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.core.GameRules

/**
 * Derives the [RankedMode] a 1v1 game belongs to from its lobby's format. This is the single source of
 * truth for the LIMITED / CONSTRUCTED / COMMANDER split, computed once at game creation and carried on
 * the [GameSession][com.wingedsheep.gameserver.session.GameSession] so the game-over path doesn't have
 * to re-derive it (the lobby may already be gone by then for quick games).
 */
object Ranked {
    /**
     * Quick-game mode: Commander rules put the game in the COMMANDER queue whatever the deck came
     * from; any other constructed restriction (or Momir, which uses a fixed constructed-style pool)
     * is CONSTRUCTED; no restriction means a random sealed pool, i.e. LIMITED.
     */
    fun modeForQuickGame(rules: GameRules, format: DeckFormat?, momirBasic: Boolean): RankedMode = when {
        rules.usesCommanders -> RankedMode.COMMANDER
        format != null || momirBasic -> RankedMode.CONSTRUCTED
        else -> RankedMode.LIMITED
    }

    /**
     * Tournament mode: the queue follows the Rules axis first — a Commander game is a Commander game
     * whether its decks were drafted, sealed or brought. Failing that, brought decks are CONSTRUCTED
     * and pool-built tournaments (sealed/draft/winston/grid) are LIMITED.
     *
     * This used to need three branches to ask one question, because commander-ness lived in two
     * unrelated fields.
     */
    fun modeForTournament(rules: GameRules, format: TournamentFormat): RankedMode = when {
        rules.usesCommanders -> RankedMode.COMMANDER
        format == TournamentFormat.PREMADE_DECKS -> RankedMode.CONSTRUCTED
        else -> RankedMode.LIMITED
    }
}
