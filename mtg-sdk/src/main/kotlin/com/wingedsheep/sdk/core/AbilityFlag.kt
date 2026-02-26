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
    MAY_NOT_UNTAP("You may choose not to untap")
}
