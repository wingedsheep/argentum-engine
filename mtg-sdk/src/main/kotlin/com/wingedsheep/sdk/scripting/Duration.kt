package com.wingedsheep.sdk.scripting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the duration of a temporary effect.
 *
 * Magic has many different durations for effects:
 * - "Until end of turn"
 * - "Until your next turn"
 * - "For as long as you control this permanent"
 * - "Until end of combat"
 *
 * Usage:
 * ```kotlin
 * ModifyStatsEffect(
 *     powerModifier = 3,
 *     toughnessModifier = 3,
 *     target = EffectTarget.ContextTarget(0),
 *     duration = Duration.EndOfTurn
 * )
 * ```
 */
@Serializable
sealed interface Duration {
    val description: String

    /**
     * Effect lasts until end of turn.
     * Example: Giant Growth
     */
    @SerialName("EndOfTurn")
    @Serializable
    data object EndOfTurn : Duration {
        override val description = "until end of turn"
    }

    /**
     * Effect lasts until the beginning of your next turn.
     * Example: Teferi's Protection
     */
    @SerialName("UntilYourNextTurn")
    @Serializable
    data object UntilYourNextTurn : Duration {
        override val description = "until your next turn"
    }

    /**
     * Effect lasts until the beginning of your next upkeep.
     * Example: Various older cards
     */
    @SerialName("UntilYourNextUpkeep")
    @Serializable
    data object UntilYourNextUpkeep : Duration {
        override val description = "until the beginning of your next upkeep"
    }

    /**
     * Effect lasts until the beginning of the next end step (the *next* one — never the end step
     * it was created in). Distinct from [EndOfTurn], which ends during the cleanup step: this ends
     * one step earlier, at the start of the end step, the same moment as a delayed
     * "at the beginning of the next end step" trigger fires. So "exile … return it at the beginning
     * of the next end step" and "… becomes a copy of it until the next end step" line up exactly.
     *
     * Not player-keyed — it's "the next end step" of any player's turn, not "your next end step".
     * Physically expired by `CleanupPhaseManager.performNextEndStepExpiry`, invoked on entry to the
     * end step (`TurnManager`). Example: Niko, Light of Hope — "Shards you control become copies of
     * it until the next end step."
     */
    @SerialName("UntilNextEndStep")
    @Serializable
    data object UntilNextEndStep : Duration {
        override val description = "until the next end step"
    }

    /**
     * Effect lasts until end of combat.
     * Example: Fog effects, combat tricks
     */
    @SerialName("EndOfCombat")
    @Serializable
    data object EndOfCombat : Duration {
        override val description = "until end of combat"
    }

    /**
     * Effect is permanent (static abilities, auras while attached).
     * No duration text is displayed.
     */
    @SerialName("Permanent")
    @Serializable
    data object Permanent : Duration {
        override val description = ""
    }

    /**
     * Effect lasts while the source permanent is on the battlefield.
     * Example: Anthem effects, equipment bonuses
     */
    @SerialName("WhileSourceOnBattlefield")
    @Serializable
    data class WhileSourceOnBattlefield(
        val sourceDescription: String = "this permanent"
    ) : Duration {
        override val description = "for as long as $sourceDescription remains on the battlefield"
    }

    /**
     * Effect lasts for as long as the effect's source (an Aura/Equipment) remains attached to the
     * affected permanent (CR 611.2b "for as long as …"). The effect drops the instant the source
     * leaves the battlefield, becomes unattached, or re-attaches to a different permanent. The
     * latch is one-way per affected entity: once the source stops being attached to it, the effect
     * never re-applies to that entity even if the source re-attaches.
     *
     * Used by Eriette, the Beguiler ("gain control of that permanent for as long as that Aura is
     * attached to it") and Assimilation Aegis ("for as long as this Equipment remains attached to
     * it, that creature becomes a copy …"). The source is the Aura/Equipment, not the card whose
     * ability created the effect.
     */
    @SerialName("WhileSourceAttachedToAffected")
    @Serializable
    data object WhileSourceAttachedToAffected : Duration {
        override val description = "for as long as it remains attached"
    }

    /**
     * Effect lasts for as long as each affected permanent has at least one counter of
     * [counterType] on it (CR 611.2b "for as long as …"). This is the *affected-object*
     * analogue of the source-keyed `While*` durations: the gate watches a counter on the
     * permanent the effect is modifying, not on the effect's source, so the effect persists
     * independently of the source permanent and ends the instant the counter leaves that
     * affected permanent.
     *
     * Per-affected and one-way (like [WhileSourceAttachedToAffected]): each affected entity is
     * dropped as soon as it stops carrying the counter, and `EndedDurationExpiryCheck` latches
     * that drop off so re-adding the counter does not resurrect the effect for that entity
     * (a fresh application would create a new effect). `StateProjector` supplies the
     * instantaneous per-frame gate.
     *
     * Used by Ultima, Origin of Oblivion — "put a blight counter on target land. For as long as
     * that land has a blight counter on it, it loses all land types and abilities and has
     * '{T}: Add {C}.'" — where the transform is created by a resolving triggered ability and must
     * outlive Ultima leaving the battlefield.
     *
     * @property counterType The counter kind that must remain present (matches
     *   [com.wingedsheep.sdk.core.CounterType] names / the `Counters.*` string constants,
     *   e.g. `Counters.BLIGHT`).
     */
    @SerialName("WhileAffectedHasCounter")
    @Serializable
    data class WhileAffectedHasCounter(val counterType: String) : Duration {
        override val description = "for as long as it has a $counterType counter on it"
    }

    /**
     * Effect lasts until a specific phase.
     * Example: "Until your next end step"
     */
    @SerialName("UntilPhase")
    @Serializable
    data class UntilPhase(val phase: String) : Duration {
        override val description = "until the $phase"
    }

    /**
     * Effect lasts until a condition is met.
     * Example: "Until that creature leaves the battlefield"
     */
    @SerialName("UntilCondition")
    @Serializable
    data class UntilCondition(val conditionDescription: String) : Duration {
        override val description = "until $conditionDescription"
    }

    /**
     * Effect lasts through the affected entity's controller's next untap step, then expires.
     * Used for "doesn't untap during its controller's next untap step" effects.
     * Unlike UntilYourNextTurn (which tracks the caster), this tracks the affected creature's controller.
     * Example: Crippling Chill, Mercurial Kite
     */
    @SerialName("UntilAfterAffectedControllersNextUntap")
    @Serializable
    data object UntilAfterAffectedControllersNextUntap : Duration {
        override val description = "doesn't untap during its controller's next untap step"
    }

    /**
     * Effect lasts while the source permanent remains tapped. One-way (CR 611.2b): once the
     * source untaps or leaves the battlefield the effect ends for good — `EndedDurationExpiryCheck`
     * removes it, so a later re-tap does not restart it.
     * Example: Everglove Courier "for as long as Everglove Courier remains tapped"
     */
    @SerialName("WhileSourceTapped")
    @Serializable
    data class WhileSourceTapped(
        val sourceDescription: String = "this creature"
    ) : Duration {
        override val description = "for as long as $sourceDescription remains tapped"
    }

    /**
     * Effect lasts for as long as each *affected* permanent remains tapped (CR 611.2b
     * "for as long as it remains tapped") — the affected-object mirror of
     * [WhileSourceTapped]: the gate watches the permanent the effect is modifying, not the
     * effect's source, so the effect persists independently of the source and ends the
     * instant that permanent becomes untapped (or leaves the battlefield — a returning
     * permanent is a new object).
     *
     * Per-affected and one-way (CR 611.2b): once an affected permanent untaps, the effect
     * ends for that permanent for good — `EndedDurationExpiryCheck` physically removes it
     * (from floating effects and from duration-keyed grants alike), so tapping it again
     * later does NOT restart the effect. `StateProjector` (floating effects) and the
     * granted-ability read sites supply the instantaneous per-frame gate.
     *
     * Used by Braided Net — "Tap another target nonland permanent. Its activated abilities
     * can't be activated for as long as it remains tapped." — as the duration of a
     * [com.wingedsheep.sdk.scripting.effects.GrantStaticAbilityEffect] carrying
     * [com.wingedsheep.sdk.scripting.PreventActivatedAbilities].
     */
    @SerialName("WhileAffectedTapped")
    @Serializable
    data object WhileAffectedTapped : Duration {
        override val description = "for as long as it remains tapped"
    }

    /**
     * Effect lasts while the source permanent remains tapped AND each affected entity's
     * projected power stays less than or equal to the source's projected power. Gated
     * per-frame by `StateProjector`: the source-tapped half is enforced when the floating
     * effect is collected; the affected-power half is a post-Layer-7 fix-up that compares
     * each affected entity's final projected power to the source's final projected power
     * and reverts the controller for any entity that's stronger. The fix-up runs after
     * Layer 7, so it picks up every pump source — base printed power, +1/+1 / -1/-1
     * counters, Layer-7 floating pumps (Giant Growth, Aggressive Urge), and lord-style
     * anthems.
     *
     * Like every "for as long as …" duration this is one-way (CR 611.2b): once the source
     * untaps or an affected entity's power exceeds the source's, `EndedDurationExpiryCheck`
     * (a state-based action) physically removes the effect, so a pump that later wears off —
     * or a re-tap — does NOT restart it. The projection gate is the instantaneous view; the
     * SBA makes the end permanent.
     *
     * Example: Old Man of the Sea — "for as long as Old Man of the Sea remains tapped
     * and that creature's power remains less than or equal to Old Man of the Sea's power".
     */
    @SerialName("WhileSourceTappedAndAffectedPowerAtMostSource")
    @Serializable
    data class WhileSourceTappedAndAffectedPowerAtMostSource(
        val sourceDescription: String = "this creature"
    ) : Duration {
        override val description =
            "for as long as $sourceDescription remains tapped and that creature's power remains less than or equal to $sourceDescription's power"
    }

    /**
     * Effect lasts for as long as the effect's controller controls the affected object —
     * it ends the moment that object's controller becomes a different player ("for as long
     * as you control it"). Evaluated against the *projected* controller, so it responds to
     * every kind of control-changing effect (one-shot steals, Threaten, and static control
     * Auras alike). One-way (CR 611.2b): the instant the controller loses the object the effect
     * ends for good — `EndedDurationExpiryCheck` removes it, so regaining control does not
     * restart it.
     *
     * Example: Suspend (CR 702.62g) — a creature played via suspend "gains haste until you
     * lose control of the spell or the permanent it becomes."
     */
    @SerialName("WhileControlledByController")
    @Serializable
    data object WhileControlledByController : Duration {
        override val description = "for as long as you control it"
    }

    /**
     * Effect lasts for as long as the effect's controller controls the *source* permanent —
     * mirror of [WhileControlledByController], but the gate watches the source's controller
     * rather than the affected object's controller. Used by abilities phrased "for as long
     * as you control this [permanent]" where "this" refers to the source itself.
     *
     * Distinct from [WhileSourceOnBattlefield]: that one ends only when the source leaves
     * the battlefield, so a Threaten-style steal of the source would let the effect linger
     * under the wrong player until the source dies. `WhileYouControlSource` ends the
     * moment the source's controller changes, which is what "as long as you control this"
     * actually means.
     *
     * Evaluated against the source's *projected* controller, so it responds to every kind
     * of control-changing effect (one-shot steals, Threaten, static control Auras). One-way
     * (CR 611.2b): once the source's controller changes (or the source leaves the battlefield)
     * the effect ends for good — `EndedDurationExpiryCheck` physically removes it, so
     * regaining control does not restart it.
     *
     * Examples: Aladdin (ARN) — "Gain control of target artifact for as long as you control
     * this creature."  Scroll of Isildur (LTR) — "Gain control of up to one target artifact
     * for as long as you control this Saga."
     */
    @SerialName("WhileYouControlSource")
    @Serializable
    data class WhileYouControlSource(
        val sourceDescription: String = "this permanent"
    ) : Duration {
        override val description = "for as long as you control $sourceDescription"
    }

    /**
     * Effect is consumed the first time its replacement effect is applied
     * or end of turn if not consumed.
     */
    @SerialName("NextUse")
    @Serializable
    data class NextUse(
        val sourceDescription: String
    ) : Duration {
        override val description = "the next time $sourceDescription this turn"
    }
}

/**
 * Convenience object for DSL-style duration creation.
 */
object Durations {
    val EndOfTurn = Duration.EndOfTurn
    val UntilYourNextTurn = Duration.UntilYourNextTurn
    val UntilNextEndStep = Duration.UntilNextEndStep
    val EndOfCombat = Duration.EndOfCombat
    val Permanent = Duration.Permanent

    fun whileOnBattlefield(source: String = "this permanent") =
        Duration.WhileSourceOnBattlefield(source)

    fun whileSourceTapped(source: String = "this creature") =
        Duration.WhileSourceTapped(source)

    val WhileAffectedTapped = Duration.WhileAffectedTapped

    fun whileYouControlSource(source: String = "this permanent") =
        Duration.WhileYouControlSource(source)

    fun whileAffectedHasCounter(counterType: String) =
        Duration.WhileAffectedHasCounter(counterType)

    fun untilPhase(phase: String) = Duration.UntilPhase(phase)
    fun until(condition: String) = Duration.UntilCondition(condition)
}
