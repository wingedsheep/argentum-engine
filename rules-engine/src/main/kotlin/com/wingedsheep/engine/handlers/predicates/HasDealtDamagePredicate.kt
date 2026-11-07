package com.wingedsheep.engine.handlers.predicates

import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.battlefield.HasDealtDamageComponent
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Whether the permanent held by [container] satisfies [StatePredicate.HasDealtDamage] — "has dealt
 * damage", in the window the predicate names.
 *
 * The single read behind every dispatch site (target/gather filtering, group-static projection,
 * trigger gating) and behind `Conditions.SourceHasDealtDamage`, which is `SourceMatches` over the
 * same predicate. It answers purely from the per-permanent [HasDealtDamageComponent] marker plus
 * [currentTurn] — no projection, no source context — so every site gives the same answer.
 *
 * The marker's presence is the lifetime window (since the permanent entered the battlefield as its
 * current object, CR 400.7); its recorded turn matched against [currentTurn] is the per-turn one.
 * Because both windows read one marker, no damage path can satisfy one without satisfying the other,
 * and the per-turn window needs no end-of-turn cleanup — the stamp simply stops matching.
 *
 * @param currentTurn `state.turnNumber`. Only consulted for the per-turn window.
 */
fun hasDealtDamage(
    container: ComponentContainer,
    currentTurn: Int,
    predicate: StatePredicate.HasDealtDamage
): Boolean {
    val marker = container.get<HasDealtDamageComponent>() ?: return false
    // recipient narrows the kind of object damaged: its stamp pair is absent if this object never
    // damaged that kind. combatOnly narrows the damage type: read the combat stamp, absent if this
    // object has only ever dealt noncombat damage (to that kind).
    val stamp = marker.stampFor(predicate.recipient) ?: return false
    val lastTurn = if (predicate.combatOnly) {
        stamp.lastCombatTurn ?: return false
    } else {
        stamp.lastTurn
    }
    if (!predicate.thisTurnOnly) return true
    return lastTurn == currentTurn
}
