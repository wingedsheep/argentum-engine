package com.wingedsheep.gameserver.lobby

import com.wingedsheep.sdk.model.EntityId
import kotlin.random.Random

/**
 * Computes the Two-Headed Giant team partition (CR 810) for a four-seat pod.
 *
 * Teams are expressed as seat indices into [orderedPlayerIds] — the exact order in which the caller
 * will seat the players — so the result drops straight into
 * [com.wingedsheep.gameserver.session.GameSession.teams]. The engine's [GameInitializer] later
 * reorders seats so teammates sit adjacently (CR 805.1), so the pairs here need not be adjacent
 * indices.
 *
 * Two modes:
 *  - [randomTeams] = true (the lobby default): the seats are shuffled and split into even teams.
 *    Re-rolled on every call, so a "play again" pod gets fresh teams each game.
 *  - [randomTeams] = false: each player's team comes from [manualAssignment] (playerId -> team
 *    index). Players with no entry are slotted into whichever team still has room, so a partial
 *    assignment is usable. An assignment that can't yield even teams (e.g. three players forced onto
 *    one team) falls back to seat-order pairing so a malformed manual setup can never wedge the
 *    start of a game.
 */
object TwoHeadedGiantTeams {
    /** Two-Headed Giant is always two teams (CR 810.1). */
    const val TEAM_COUNT = 2

    fun partition(
        orderedPlayerIds: List<EntityId>,
        randomTeams: Boolean,
        manualAssignment: Map<EntityId, Int>,
        random: Random = Random.Default,
    ): List<List<Int>> {
        val seats = orderedPlayerIds.indices.toList()
        val teamSize = seats.size / TEAM_COUNT

        if (randomTeams) {
            return seats.shuffled(random).chunked(teamSize).take(TEAM_COUNT).map { it.sorted() }
        }

        val teams = List(TEAM_COUNT) { mutableListOf<Int>() }
        for (seat in seats) {
            val team = manualAssignment[orderedPlayerIds[seat]] ?: continue
            if (team in 0 until TEAM_COUNT) teams[team].add(seat)
        }
        // Slot any unassigned seats into a team that still has room (keeps partial setups usable).
        val assigned = teams.flatten().toSet()
        for (seat in seats) {
            if (seat in assigned) continue
            (teams.firstOrNull { it.size < teamSize } ?: teams[0]).add(seat)
        }
        return if (teams.all { it.size == teamSize }) {
            teams.map { it.sorted() }
        } else {
            // Malformed manual assignment (e.g. 3 vs 1): fall back to deterministic seat-order pairing.
            seats.chunked(teamSize).take(TEAM_COUNT).map { it.sorted() }
        }
    }
}
