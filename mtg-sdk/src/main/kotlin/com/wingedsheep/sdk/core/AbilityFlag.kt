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

    // ── Transform restriction flags ─────────────────────────────
    /**
     * "This permanent can't transform" (CR 701.27b — a permanent that can't transform simply
     * doesn't). Enforced in the single shared transform-in-place implementation
     * (`flipDfcInPlace`), so it blocks *every* cause: a `TransformEffect` one-shot, an
     * activated/triggered transform ability, and the daybound/nightbound day-change flips.
     * Granted to the enchanted creature by Bound by Moonsilver.
     */
    CANT_TRANSFORM("Can't transform"),

    // ── Designation restriction flags ───────────────────────────
    /**
     * "This creature can't become suspected" (CR 701.60 — Airtight Alibi). Enforced in the single
     * shared suspect implementation (`SuspectExecutor`), which is why suspect is **one** effect
     * rather than a composite of status + menace + can't-block: gating only the status half would
     * still land the menace and can't-block riders, leaving a creature that is not suspected but
     * carries suspect's downsides.
     *
     * Distinct from being un-suspected ([com.wingedsheep.sdk.scripting.effects.RemoveSuspectedEffect]):
     * this prevents the designation from ever attaching, so no "becomes suspected" trigger fires
     * either, where un-suspecting takes an existing designation away after the fact.
     */
    CANT_BECOME_SUSPECTED("Can't become suspected"),

    // ── Combat damage assignment flags ──────────────────────────
    ASSIGNS_COMBAT_DAMAGE_AS_TOUGHNESS("Assigns combat damage equal to its toughness rather than its power"),

    // ── Summoning-sickness flags ────────────────────────────────
    /**
     * "You may activate abilities of this creature as though it had haste" — the
     * Thousand-Year Elixir / Shang-Chi, Master of Kung Fu permission.
     *
     * CR 302.6 gates two separate things on the same condition: a creature can't activate an
     * activated ability whose cost includes `{T}` or `{Q}`, *and* a creature can't attack. Haste
     * (CR 702.10b/c) lifts both. This flag lifts **only the ability half** — a creature carrying it
     * still can't attack the turn it arrives, which is exactly what "as though those creatures had
     * haste" (limited to activating abilities) means.
     *
     * So it is deliberately *not* [Keyword.HASTE]: grant it with
     * `GrantKeyword(AbilityFlag.MAY_ACTIVATE_ABILITIES_AS_THOUGH_HASTY, <filter>)` and the layer
     * system carries it like any other granted keyword. It is read only by
     * `SummoningSicknessRules` (the shared `{T}`/`{Q}` gate); `AttackRestrictionRules` keeps its own
     * plain haste check, so the attack half is structurally unaffected.
     */
    MAY_ACTIVATE_ABILITIES_AS_THOUGH_HASTY("You may activate its abilities as though it had haste")
}
