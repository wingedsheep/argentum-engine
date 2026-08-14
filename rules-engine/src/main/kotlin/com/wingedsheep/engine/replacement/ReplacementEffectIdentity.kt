package com.wingedsheep.engine.replacement

import com.wingedsheep.engine.state.components.identity.SelfZoneRedirectComponent
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Uniquely identifies a specific replacement effect instance.
 *
 * Replacement effects are tracked by this identity to ensure each effect
 * only applies once per event chain (CR 614.5 — a replacement effect doesn't
 * invoke itself repeatedly). This is particularly important when an entity
 * has two copies of the same ability (e.g., two Doubling Seasons), each of
 * which should apply exactly once.
 *
 * The sealed hierarchy distinguishes between battlefield-originated effects
 * and floating-effect shields (e.g., Words cycle Duration.NextUse shields)
 * with no magic-index hacks.
 */
@Serializable
sealed interface ReplacementEffectIdentity {

    /**
     * A replacement effect originating from a permanent on the battlefield.
     *
     * @property sourceEntityId The permanent that grants this replacement effect
     * @property effectIndex Which replacement effect within that ability (0-based)
     */
    @SerialName("BattlefieldIdentity")
    @Serializable
    data class BattlefieldIdentity(
        val sourceEntityId: EntityId,
        val effectIndex: Int = 0
    ) : ReplacementEffectIdentity

    /**
     * A replacement effect originating from a floating-effect.
     *
     * @property floatingId Index into [GameState.floatingEffects]
     */
    @SerialName("FloatingIdentity")
    @Serializable
    data class FloatingIdentity(
        val floatingId: EntityId
    ) : ReplacementEffectIdentity

    /**
     * A replacement effect that was granted temporarily (stored in
     * [GameState.grantedReplacementEffects]) — e.g. Malicious Eclipse's
     * "if a creature would die this turn, exile it instead".
     *
     * @property grantedIndex Index into [GameState.grantedReplacementEffects]
     */
    @SerialName("GrantedIdentity")
    @Serializable
    data class GrantedIdentity(
        val grantedIndex: Int
    ) : ReplacementEffectIdentity

    /**
     * A self-redirect replacement effect carried on a card entity via
     * [SelfZoneRedirectComponent].
     *
     * @property sourceEntityId The card entity carrying the self-redirect
     * @property effectIndex Which redirect within the component (0-based)
     */
    @SerialName("SelfRedirectIdentity")
    @Serializable
    data class SelfRedirectIdentity(
        val sourceEntityId: EntityId,
        val effectIndex: Int = 0
    ) : ReplacementEffectIdentity
}