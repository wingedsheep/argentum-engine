package com.wingedsheep.sdk.core

import kotlinx.serialization.Serializable

/**
 * Non-keyword static ability flags.
 *
 * These are engine flags for abilities that are NOT true MTG keyword abilities
 * per the Comprehensive Rules (702.x), but instead represent static ability text
 * used as binary on/off flags by the engine.
 *
 * They are stored alongside keywords in the projected state's keyword set
 * (as strings) so the engine's hasKeyword() checks work uniformly.
 */
@Serializable
enum class AbilityFlag(val displayName: String) {
    // ── Evasion flags ───────────────────────────────────────────
    CANT_BE_BLOCKED("Can't be blocked"),
    CANT_BE_BLOCKED_BY_MORE_THAN_ONE("Can't be blocked by more than one creature"),

    // ── Untap restriction flags ─────────────────────────────────
    DOESNT_UNTAP("Doesn't untap during your untap step"),

    /**
     * "Can't become untapped." The stronger untap restriction (Blossombind, Kraken's
     * Eye of the Sea, …): unlike [DOESNT_UNTAP] — which only removes the permanent from
     * its controller's untap step (CR 502.3) — this blocks *every* source of untapping,
     * including explicit "untap target permanent" effects, provoke, and untap costs. It
     * is a continuous "can't" restriction, so it also subsumes the untap-step behavior.
     * Enforced universally in the shared untap atom (`untapOrConsumeStun`), which every
     * untap path routes through.
     */
    CANT_BECOME_UNTAPPED("Can't become untapped"),

    MAY_NOT_UNTAP("You may choose not to untap"),

    /**
     * "If this would untap during its controller's untap step, remove a +1/+1
     * counter from it instead. If you do, untap it." (CR 614 replacement applied
     * during the untap step, CR 502.) Granted to a permanent (e.g. the creature
     * enchanted by Bewitching Leechcraft). During the untap step the engine tries
     * to remove a +1/+1 counter; the permanent untaps only if a counter was removed,
     * otherwise it stays tapped. Applies only to the natural untap step — explicit
     * "untap target permanent" effects are unaffected.
     */
    REMOVE_COUNTER_TO_UNTAP("If this would untap during your untap step, remove a +1/+1 counter from it instead; untap it only if you do"),

    // ── Counter restriction flags ───────────────────────────────
    CANT_RECEIVE_COUNTERS("Can't have counters put on it"),

    // ── Sacrifice restriction flags ─────────────────────────────
    CANT_BE_SACRIFICED("Can't be sacrificed"),

    // ── Aura / control restriction flags ────────────────────────
    /**
     * Auras can't be put onto this permanent (CR 303.4). Enforced at Aura-cast target legality
     * in TargetValidator. Granted by effects like Guardian Beast.
     */
    CANT_BE_ENCHANTED("Can't be enchanted"),

    /**
     * Other players can't gain control of this permanent. Enforced in the control-change executors
     * (gain / exchange / by-most). Granted by effects like Guardian Beast.
     */
    CANT_GAIN_CONTROL("Can't be gained control of"),

    // ── Combat damage assignment flags ──────────────────────────
    ASSIGNS_COMBAT_DAMAGE_AS_TOUGHNESS("Assigns combat damage equal to its toughness rather than its power")
}
