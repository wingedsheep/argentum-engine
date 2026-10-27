package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreatureOrPlaneswalker
import com.wingedsheep.sdk.scripting.targets.TargetCreatureOrPlayer
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.targets.TargetOpponentOrPlaneswalker
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.targets.TargetPermanentOrPlayer
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.targets.TargetPlayerOrPlaneswalker
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent

/**
 * The zones a target requirement looks for objects in — what a targeting question declares as
 * [com.wingedsheep.engine.core.DecisionContext.candidateZones].
 *
 * Players are not a zone: a requirement that can only name players has the empty set. The `when`
 * is exhaustive on purpose, so a new requirement shape has to say where its candidates live.
 */
object TargetCandidateZones {

    fun of(requirement: TargetRequirement): Set<Zone> = when (requirement) {
        is TargetPlayer, is TargetOpponent -> emptySet()
        is AnyTarget,
        is TargetCreatureOrPlayer,
        is TargetPermanentOrPlayer,
        is TargetOpponentOrPlaneswalker,
        is TargetPlayerOrPlaneswalker,
        is TargetCreatureOrPlaneswalker -> setOf(Zone.BATTLEFIELD)
        is TargetSpellOrPermanent -> setOf(Zone.BATTLEFIELD, Zone.STACK)
        is TargetObject -> requirement.filter.clauses().mapTo(mutableSetOf()) { it.zone }
        is TargetOther -> of(requirement.baseRequirement)
    }

    /** The union over every requirement a single question asks for. */
    fun of(requirements: List<TargetRequirement>): Set<Zone> =
        requirements.flatMapTo(mutableSetOf()) { of(it) }
}
