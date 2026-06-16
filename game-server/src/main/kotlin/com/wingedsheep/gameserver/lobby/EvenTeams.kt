package com.wingedsheep.gameserver.lobby

import com.wingedsheep.sdk.model.EntityId
import kotlin.random.Random

/**
 * Splits a pod of seats into [teamCount] even teams — the shared partition used by both
 * Two-Headed Giant (CR 810: two teams of two) and the general Team vs. Team variant (CR 808: two
 * teams of N, e.g. 2v2 / 3v3 / 4v4).
 *
 * Teams are expressed as seat indices into [orderedPlayerIds] — the exact order in which the caller
 * will seat the players — so the result drops straight into
 * [com.wingedsheep.gameserver.session.GameSession.teams]. The engine's GameInitializer later reorders
 * seats so teammates sit adjacently (CR 805.1 / 808.2), so the groups here need not be adjacent
 * indices.
 *
 * Two modes:
 *  - [randomTeams] = true (the lobby default): the seats are shuffled and split into even teams.
 *    Re-rolled on every call, so a "play again" pod gets fresh teams each game.
 *  - [randomTeams] = false: each player's team comes from [manualAssignment] (playerId -> team
 *    index). Players with no entry are slotted into whichever team still has room, so a partial
 *    assignment is usable. An assignment that can't yield even teams (e.g. three players forced onto
 *    one team) falls back to seat-order grouping so a malformed manual setup can never wedge the
 *    start of a game.
 *
 * The pod must divide evenly: `orderedPlayerIds.size % teamCount == 0` (the lobby enforces this
 * before starting — exactly four players for 2HG, an even count ≥ 4 for Team vs. Team).
 */
object EvenTeams {
    /** Both team variants currently shipped use two teams (CR 810.1 for 2HG; the default for Team vs. Team). */
    const val DEFAULT_TEAM_COUNT = 2

    fun partition(
        orderedPlayerIds: List<EntityId>,
        randomTeams: Boolean,
        manualAssignment: Map<EntityId, Int>,
        teamCount: Int = DEFAULT_TEAM_COUNT,
        random: Random = Random.Default,
    ): List<List<Int>> {
        val seats = orderedPlayerIds.indices.toList()
        val teamSize = seats.size / teamCount

        if (randomTeams) {
            return seats.shuffled(random).chunked(teamSize).take(teamCount).map { it.sorted() }
        }

        val teams = List(teamCount) { mutableListOf<Int>() }
        for (seat in seats) {
            val team = manualAssignment[orderedPlayerIds[seat]] ?: continue
            if (team in 0 until teamCount) teams[team].add(seat)
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
            // Malformed manual assignment (e.g. 3 vs 1): fall back to deterministic seat-order grouping.
            seats.chunked(teamSize).take(teamCount).map { it.sorted() }
        }
    }
}
