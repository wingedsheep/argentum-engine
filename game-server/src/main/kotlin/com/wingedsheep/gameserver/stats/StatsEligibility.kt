package com.wingedsheep.gameserver.stats

import com.wingedsheep.engine.core.BottomCards
import com.wingedsheep.engine.core.Concede
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.KeepHand
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.TakeMulligan

/**
 * Which recorded results count toward win/loss stats.
 *
 * A game only says something about a player when they actually played it. Two shapes don't:
 *  - a tournament no-show — the game started while the player was away and the disconnect timer
 *    conceded for them;
 *  - a player who kept (or mulliganed) and conceded before doing anything else.
 *
 * A seat **took an action** when its recorded inputs hold one play of its own past the opening hands:
 * a land, a spell, an ability, a real attack or block, a decision. Mulligan inputs, concessions, priority
 * passes and empty combat declarations don't count — the server submits the last three on an idle
 * seat's behalf (auto-pass), so they can't tell a present player from an absent one.
 *
 * A seat's result **counts** when it took an action and so did at least one other seat: the free win a
 * no-show hands their opponent counts for no one either, while in a multiplayer game the players who
 * kept playing still have a real result among themselves.
 */
object StatsEligibility {

    /** Whether [action] is a play the player made themselves — see the class doc for what isn't. */
    fun isOwnPlay(action: GameAction): Boolean = when (action) {
        is TakeMulligan, is KeepHand, is BottomCards, is Concede, is PassPriority -> false
        is DeclareAttackers -> action.attackers.isNotEmpty()
        is DeclareBlockers -> action.blockers.isNotEmpty()
        else -> true
    }

    /** Ids (as strings) of every player who made at least one [isOwnPlay] action in [actions]. */
    fun playersWhoActed(actions: List<GameAction>): Set<String> =
        actions.asSequence().filter(::isOwnPlay).map { it.playerId.value }.toSet()

    /**
     * Whether each seat's result counts, given whether each seat took an action (null = unknown, read
     * as "took an action" so games recorded before tracking keep counting). Keyed like [tookAction].
     */
    fun <K> countsInStats(tookAction: Map<K, Boolean?>): Map<K, Boolean> {
        val acted = tookAction.mapValues { (_, v) -> v != false }
        val actedCount = acted.values.count { it }
        return acted.mapValues { (_, a) -> a && actedCount >= 2 }
    }
}
