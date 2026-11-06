package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Reads a Vehicle's printed body: the creature it becomes once crewed (CR 702.122, Crew N).
 *
 * An uncrewed Vehicle is a non-creature artifact, so the board evaluator used to price it as an
 * unreadable permanent (0.5) however big the creature behind its crew was, and the draft heuristic
 * rated a 7/11 like a blank artifact. The body is real but conditional: it only fights when enough
 * creature power is tapped for it, and the crew creatures don't attack or block that turn. So the
 * reading is a **share** of the creature-body value, between [UNCREWABLE_SHARE] (nothing on board
 * can crew it yet) and [CREWABLE_SHARE] (the controller's other creatures carry at least N power),
 * linear in between. A crewed Vehicle is a creature in the projection and is priced as one; this
 * reading only covers the turns it sits uncrewed.
 *
 * Mounts (Saddle N) are creatures already, so they need none of this.
 */
internal object VehicleBody {
    /** Share of the body an uncrewed Vehicle is worth when its controller has no crew power at all. */
    const val UNCREWABLE_SHARE = 0.2

    /** Share of the body an uncrewed Vehicle is worth when its controller's other creatures can crew it. */
    const val CREWABLE_SHARE = 0.5

    /** Draft-heuristic discount per point of crew: the creatures tapped to crew sit out the fight. */
    const val DRAFT_CREW_PENALTY = 0.15

    /** The cheapest printed Crew N on [card], or null when it has none. */
    fun printedCrew(card: CardDefinition): Int? = card.keywordAbilities
        .filterIsInstance<KeywordAbility.Numeric>()
        .filter { it.keyword == Keyword.CREW }
        .minOfOrNull { it.n }

    /**
     * The cheapest crew a permanent has right now from its projected keyword names: the printed
     * [printed] N while the bare `CREW` keyword survives (lost-all-abilities strips it), plus any
     * granted `CREW_<n>` ("Vehicles you control have crew 1").
     */
    fun crewInForce(projectedKeywords: Set<String>, printed: Int?): Int? {
        val fromPrinted = printed?.takeIf { Keyword.CREW.name in projectedKeywords }
        val granted = projectedKeywords.mapNotNull {
            if (it.startsWith(GRANTED_PREFIX)) it.substring(GRANTED_PREFIX.length).toIntOrNull() else null
        }
        return (granted + listOfNotNull(fromPrinted)).minOrNull()
    }

    /** The share of the body worth counting when [crewPower] is what the controller could tap for crew [crew]. */
    fun share(crew: Int, crewPower: Int): Double {
        val ready = if (crew <= 0) 1.0 else (crewPower.toDouble() / crew).coerceIn(0.0, 1.0)
        return UNCREWABLE_SHARE + (CREWABLE_SHARE - UNCREWABLE_SHARE) * ready
    }

    private val GRANTED_PREFIX = "${Keyword.CREW.name}_"
}
