package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CrewSaddleContributorsComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * The crew ability a Vehicle can activate right now, if any — the one place `CrewEnumerator` and
 * `CrewVehicleHandler` resolve "Crew N" from, so the action the client is offered and the one the
 * engine accepts can't disagree.
 *
 * A permanent can have several crew abilities, and each is a separate activated ability
 * (CR 702.122a): its printed `Crew N` and any granted ones ("Vehicles you control have crew 1",
 * Astor, Bearer of Blades — the projected `CREW_<n>` keyword). Crew abilities differ only in N
 * (and a printed "Activate only once each turn"), so the cheapest one still available is the one
 * worth activating; that's the requirement returned.
 *
 * The printed instance only counts while the projected bare `CREW` keyword is still there, so a
 * Vehicle that has lost all its abilities has no printed crew — though a crew ability granted after
 * that (a later timestamp, CR 613.7) still survives in the projection.
 */
internal object CrewRequirement {

    /** One crew ability: its N and whether it is the printed "Activate only once each turn" kind. */
    data class Crew(val n: Int, val onceEachTurn: Boolean)

    /** The cheapest crew ability [vehicleId] can activate now, or null when it has none left. */
    fun available(
        state: GameState,
        projected: ProjectedState,
        cardRegistry: CardRegistry,
        vehicleId: EntityId
    ): Crew? {
        val crewedThisTurn = (state.getEntity(vehicleId)
            ?.get<CrewSaddleContributorsComponent>()?.crewActivations ?: 0) >= 1
        // Cheapest first; on a tie prefer the unlimited instance so a once-each-turn one is kept
        // for later in the turn.
        return all(state, projected, cardRegistry, vehicleId)
            .filterNot { it.onceEachTurn && crewedThisTurn }
            .minWithOrNull(compareBy<Crew>({ it.n }, { it.onceEachTurn }))
    }

    /** Whether [vehicleId] has any crew ability at all, used or not. */
    fun hasAny(state: GameState, projected: ProjectedState, cardRegistry: CardRegistry, vehicleId: EntityId): Boolean =
        all(state, projected, cardRegistry, vehicleId).isNotEmpty()

    private fun all(
        state: GameState,
        projected: ProjectedState,
        cardRegistry: CardRegistry,
        vehicleId: EntityId
    ): List<Crew> {
        val keywords = projected.getKeywords(vehicleId)
        // By definition id, not name, so a renamed copy of a Vehicle (CR 707.9) keeps its crew.
        val printed = if (Keyword.CREW.name in keywords) {
            state.getEntity(vehicleId)
                ?.get<CardComponent>()
                ?.let { cardRegistry.getCard(it.cardDefinitionId) }
                ?.keywordAbilities
                ?.filterIsInstance<KeywordAbility.Numeric>()
                ?.filter { it.keyword == Keyword.CREW }
                ?.map { Crew(it.n, it.onceEachTurn) }
                .orEmpty()
        } else {
            emptyList()
        }
        val granted = keywords.mapNotNull { keyword ->
            if (!keyword.startsWith(GRANTED_PREFIX)) return@mapNotNull null
            keyword.substring(GRANTED_PREFIX.length).toIntOrNull()?.let { Crew(it, onceEachTurn = false) }
        }
        return printed + granted
    }

    private val GRANTED_PREFIX = "${Keyword.CREW.name}_"
}
