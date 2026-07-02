package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Transform Effects
// =============================================================================

/**
 * Transform a double-faced permanent.
 * Toggles between front and back face.
 * "Transform this creature"
 */
@SerialName("Transform")
@Serializable
data class TransformEffect(
    val target: EffectTarget = EffectTarget.Self
) : Effect {
    override val description: String = "Transform ${target.description}"
}

/**
 * Which face a permanent re-enters on after an [ExileAndReturnTransformedEffect].
 *
 * - [TRANSFORMED] — the opposite of the face it had on the battlefield (oracle: "return it to
 *   the battlefield transformed"). A front-face creature comes back as its back face; a back-face
 *   Saga comes back as its front face.
 * - [FRONT] — its front face, regardless of the face it left on (oracle: "return it to the
 *   battlefield front face up"). Used by the eikon Saga backs on their final chapter.
 * - [BACK] — its back face, regardless of the face it left on.
 */
enum class ReturnFace { TRANSFORMED, FRONT, BACK }

/**
 * Exile a double-faced permanent, then return it to the battlefield as a **new object** on the
 * chosen face under its owner's control (FIN "Dominant" / eikon transform).
 *
 * Unlike [TransformEffect] (CR 701.27 — turning a permanent already on the battlefield over in
 * place, preserving counters/damage/attachments and firing transform triggers), this models the
 * "Exile [this], then return it to the battlefield transformed" templating: the permanent leaves
 * and a brand-new object enters. Counters/damage/auras do **not** carry over, leaves-the-battlefield
 * and enters-the-battlefield triggers fire (not transform triggers), and a Saga face re-enters with
 * a fresh lore counter (CR 714.2b). The exile and return happen atomically in one resolution — no
 * priority or state-based actions in between.
 *
 * Used by both directions of the cycle: the front face's activated/triggered ability returns it
 * [TRANSFORMED] (front → back Saga), and the back Saga's final chapter returns it [FRONT].
 */
@SerialName("ExileAndReturnTransformed")
@Serializable
data class ExileAndReturnTransformedEffect(
    val target: EffectTarget = EffectTarget.Self,
    val returnAs: ReturnFace = ReturnFace.TRANSFORMED
) : Effect {
    override val description: String = when (returnAs) {
        ReturnFace.TRANSFORMED -> "Exile ${target.description}, then return it to the battlefield transformed"
        ReturnFace.FRONT -> "Exile ${target.description}, then return it to the battlefield front face up"
        ReturnFace.BACK -> "Exile ${target.description}, then return it to the battlefield back face up"
    }
}

/**
 * Return the source of a Craft activated ability (CR 702.167a) from exile to the battlefield
 * transformed under its owner's control.
 *
 * The source has just been exiled by the Craft cost (paired with
 * [com.wingedsheep.sdk.scripting.AbilityCost.Craft]); this effect is what the activated
 * ability resolves to. It zone-moves the source from EXILE → BATTLEFIELD, sets its
 * [com.wingedsheep.engine.state.components.identity.DoubleFacedComponent] to its back face,
 * and re-attaches the `CraftedFromExiledComponent` recording the exiled materials so the
 * back face's "exiled cards used to craft it" CDA (CR 702.167c) can read them.
 *
 * Not a generic "return-and-transform" — only valid for the Craft pattern, where the source
 * being in exile and the materials being linked to it are guaranteed by the paired cost.
 */
@SerialName("ReturnSelfFromExileTransformed")
@Serializable
data object ReturnSelfFromExileTransformedEffect : Effect {
    override val description: String =
        "Return this card to the battlefield transformed under its owner's control"
}

/**
 * Return the source **card** from a non-battlefield zone to the battlefield with its back face
 * up — "Return this card from your graveyard to the battlefield transformed" (Garland, Knight
 * of Cornelia). Typically the resolution of an activated ability with
 * `activateFromZone = fromZone`.
 *
 * A double-faced card in a non-battlefield zone has only its front face's characteristics;
 * entering "transformed" means the new battlefield object has its back face up. As with the
 * other off-battlefield return-transformed effects (and unlike [TransformEffect]), no transform
 * triggers fire — the card was never turned over on the battlefield; enters-the-battlefield
 * triggers of the back face fire normally. Per the official ruling ("If you are instructed to
 * put a card that isn't a double-faced card onto the battlefield transformed, it will not enter
 * at all"), the executor no-ops when the source is not a double-faced card, and it also no-ops
 * if the source has already left [fromZone] by the time the ability resolves.
 */
@SerialName("ReturnSelfFromZoneTransformed")
@Serializable
data class ReturnSelfFromZoneTransformedEffect(
    val fromZone: Zone = Zone.GRAVEYARD,
) : Effect {
    override val description: String =
        "Return this card from your ${fromZone.displayName.lowercase()} to the battlefield transformed"
}
