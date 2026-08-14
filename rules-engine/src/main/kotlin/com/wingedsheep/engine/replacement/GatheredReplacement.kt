package com.wingedsheep.engine.replacement

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ReplacementEffect
import kotlinx.serialization.Serializable

/**
 * A replacement effect that has been matched to a [PendingGameEvent] and
 * gathered from the battlefield or granted effects.
 *
 * @property identity Unique identity for tracking (CR 614.5 — prevent double-application).
 *           Use [ReplacementEffectIdentity.BattlefieldIdentity] for battlefield permanents
 *           and [ReplacementEffectIdentity.FloatingIdentity] for floating-effects.
 * @property effect The replacement effect that matched
 * @property sourceControllerId The controller of the source permanent
 * @property description Human-readable description of why this replacement applies
 */
@Serializable
data class GatheredReplacement(
    val identity: ReplacementEffectIdentity,
    val effect: ReplacementEffect,
    val sourceControllerId: EntityId,
    val description: String
)

/**
 * Extract the source entity ID from a [GatheredReplacement], resolving it from
 * the identity type. Battlefield identities carry the source directly; floating
 * identities require looking up the floating effect's stored source ID.
 */
fun GatheredReplacement.sourceEntityId(state: GameState): EntityId? {
    return when (val id = identity) {
        is ReplacementEffectIdentity.BattlefieldIdentity -> id.sourceEntityId
        is ReplacementEffectIdentity.FloatingIdentity -> {
            state.floatingEffects.firstOrNull { it.id == id.floatingId }
                ?.effect?.modification
                ?.sourceEntityId
        }
        is ReplacementEffectIdentity.GrantedIdentity -> {
            state.grantedReplacementEffects.getOrNull(id.grantedIndex)?.entityId
        }
        is ReplacementEffectIdentity.SelfRedirectIdentity -> id.sourceEntityId
    }
}