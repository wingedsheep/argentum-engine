package com.wingedsheep.engine.event

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.ReplacementEffect
import kotlinx.serialization.Serializable

/**
 * A replacement effect that has been granted temporarily, anchored to an entity.
 *
 * Used for "this turn" replacement riders created by a resolving ability — e.g. Forgotten
 * Cellar ("if a card would be put into your graveyard from anywhere this turn, exile it
 * instead"), which grants a [com.wingedsheep.sdk.scripting.RedirectZoneChange] until end of
 * turn. Stored in [com.wingedsheep.engine.state.GameState.grantedReplacementEffects] and read
 * at the point of use — the zone-change redirect path consults granted
 * [com.wingedsheep.sdk.scripting.RedirectZoneChange] alongside permanents' printed replacement
 * effects.
 *
 * Mirrors [GrantedStaticAbility]: replacement effects are checked where they matter (zone
 * movement) rather than projected onto the entity via the layer system, so a GameState-keyed
 * record is the right channel.
 *
 * @property entityId The permanent the grant is anchored to (its controller scopes "you")
 * @property controllerId The player who controls the grant — used for owner/controller filters
 * @property replacement The replacement effect that was granted
 * @property duration How long the grant lasts. A [Duration.NextUse] grant is one-shot ("the next
 *   time one or more enchantment creatures you control enter this turn, …" — Storyweave): the read
 *   site that applies it marks it [spent], and [com.wingedsheep.engine.replacement.ActiveReplacements.purgeSpent]
 *   drops it once the event it modified is over; unspent, it expires in the cleanup step.
 * @property spent True once a [Duration.NextUse] grant has modified an event. It keeps applying to
 *   the rest of that same event (every object entering simultaneously gets the counters), and is
 *   removed at the next event boundary.
 */
@Serializable
data class GrantedReplacementEffect(
    val entityId: EntityId,
    val controllerId: EntityId,
    val replacement: ReplacementEffect,
    val duration: Duration,
    val spent: Boolean = false
)
