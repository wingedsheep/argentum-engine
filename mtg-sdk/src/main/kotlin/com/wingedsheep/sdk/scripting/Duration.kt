package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.util.numberToWord
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
     * Effect lasts through the *whole* of your next turn, ending during that turn's cleanup step
     * (CR 514.2, the same moment an "until end of turn" effect ends) — "until the end of your next
     * turn".
     *
     * Strictly longer than [UntilYourNextTurn], which ends at the *beginning* of your next turn
     * (right after its untap step). The current turn never counts, even when the effect is created
     * on your own turn: a sorcery you cast on your turn keeps its effect alive through this turn,
     * every intervening opponent's turn, and all of your following turn.
     *
     * Implementation: the creating [com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect]
     * records `expiresAfterTurn = turnNumber + 1` and `CleanupPhaseManager.cleanupEndOfTurn` drops
     * it at the cleanup of the first turn the effect's controller takes at or after that floor.
     * Pairing the floor with the controller guard is what keeps it right across extra turns,
     * skipped turns, and eliminated seats — the same mechanism
     * `MayPlayPermission.expiresAfterTurn` uses for the "you may play it until the end of your next
     * turn" wording.
     *
     * The **floating-effect** path honours this duration (control changes, P/T, keyword and
     * type grants, and everything else that lands in `GameState.floatingEffects`), and so does
     * `CantCastSpellsEffect`, whose per-player ban records the same floor (Single Combat). The separate
     * granted-ability records (`grantedTriggeredAbilities`, `grantedStaticAbilities`, …) have no
     * expiry hook for it yet — exactly as they have none for [UntilYourNextTurn] — so don't reach
     * for it there without wiring the matching cleanup first.
     *
     * Example: Evil's Thrall — "gain control of that creature until the end of your next turn".
     */
    @SerialName("EndOfYourNextTurn")
    @Serializable
    data object EndOfYourNextTurn : Duration {
        override val description = "until the end of your next turn"
    }

    /**
     * Effect lasts until the end of your next upkeep step — it outlives the upkeep it was created
     * in (and every other player's turn) and ends as your following upkeep step ends, so it is
     * still in force while that upkeep's triggers resolve.
     *
     * The upkeep-step sibling of [EndOfYourNextTurn], keyed and floored the same way: the creating
     * [com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect] records
     * `expiresAfterTurn = turnNumber + 1`, and `CleanupPhaseManager.expireEndOfYourNextUpkeepEffects`
     * drops it as an upkeep step of its controller's (team's) turn ends at or after that floor, so
     * the creating upkeep never counts. Distinct from [UntilYourNextUpkeep], which ends as that
     * upkeep *begins*. Honoured by the floating-effect path only.
     *
     * Example: Halfdane — "… base power and toughness become equal to … until the end of your next
     * upkeep."
     */
    @SerialName("EndOfYourNextUpkeep")
    @Serializable
    data object EndOfYourNextUpkeep : Duration {
        override val description = "until the end of your next upkeep"
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
     * Effect lasts for as long as each affected permanent remains attached to the effect's source —
     * the mirror of [WhileSourceAttachedToAffected], with the attachment and the host swapped: here
     * the *affected* object is the Aura/Equipment and the *source* is the permanent it is attached to.
     * Per-affected and one-way: an affected entity drops out the instant it becomes unattached from
     * the source, is attached to something else, or the source leaves the battlefield, and
     * `EndedDurationExpiryCheck` latches that off so re-attaching doesn't resurrect it.
     *
     * The engine's own duration for reconfigure's "stops being a creature until it becomes unattached
     * from that creature" (CR 702.151b): the effect is created by the attaching, not by an ability of
     * the Equipment, so it keeps applying if the Equipment loses its abilities while attached.
     * [WhileSourceAttachedToAffected] can't carry it — its source is the attachment, and the
     * effect's object is that same attachment.
     */
    @SerialName("WhileAffectedAttachedToSource")
    @Serializable
    data object WhileAffectedAttachedToSource : Duration {
        override val description = "for as long as it remains attached to it"
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
     * @property counterType The counter kind that must remain present (e.g. `CounterType.BLIGHT`).
     */
    @SerialName("WhileAffectedHasCounter")
    @Serializable
    data class WhileAffectedHasCounter(val counterType: CounterType) : Duration {
        override val description = "for as long as it has a ${counterType.printed} counter on it"
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
     * Effect lasts through the affected entity's controller's next [count] untap steps, then expires.
     * Used for "doesn't untap during its controller's next untap step" effects (Crippling Chill,
     * Mercurial Kite) and, with `count = 2`, "… next two untap steps" (Telekinesis).
     * Unlike UntilYourNextTurn (which tracks the caster), this tracks the affected creature's controller.
     * Each untap step of that controller counts one off (`CleanupPhaseManager
     * .expireAffectedControllersNextUntapEffects` rewrites the duration to `count - 1`, and drops the
     * effect after the last step); a skipped untap step doesn't count.
     */
    @SerialName("UntilAfterAffectedControllersNextUntap")
    @Serializable
    data class UntilAfterAffectedControllersNextUntap(val count: Int = 1) : Duration {
        init {
            require(count >= 1) { "count must be at least 1, was $count" }
        }

        override val description =
            if (count == 1) "doesn't untap during its controller's next untap step"
            else "during its controller's next ${numberToWord(count)} untap steps"
    }

    /**
     * Effect lasts until the end of the affected entity's controller's next turn — "that creature
     * can't attack during its controller's next turn" (Wall of Dust). The affected-object mirror of
     * [EndOfYourNextTurn], keyed like [UntilAfterAffectedControllersNextUntap]: the creating
     * [com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect] records
     * `expiresAfterTurn = turnNumber + 1`, and `CleanupPhaseManager.cleanupEndOfTurn` drops it at
     * the cleanup of the first turn at or after that floor whose active (team) player controls an
     * affected entity. The current turn never counts, even when it is that controller's own turn.
     * Floating-effect path only.
     */
    @SerialName("EndOfAffectedControllersNextTurn")
    @Serializable
    data object EndOfAffectedControllersNextTurn : Duration {
        override val description = "during its controller's next turn"
    }

    /**
     * Effect lasts until the beginning of the affected card's **owner's** next turn — "until that
     * player's next turn" where "that player" is the player whose hand the card went to (Firestorm
     * Phoenix). The affected-object mirror of [UntilYourNextTurn], which is keyed to the effect's
     * controller: when an opponent controlled the creature that put the card there, the window still
     * closes on the owner's turn, not the controller's. Expires on the same post-untap hook as
     * [UntilYourNextTurn].
     *
     * Honoured by the card-scoped hand restrictions — a card-targeted
     * [com.wingedsheep.sdk.scripting.effects.CantPlayCardsFromHandEffect] and
     * [com.wingedsheep.sdk.scripting.effects.RevealCardInHandEffect] — and by floating effects,
     * which lapse when the owner of any affected object starts a turn. The separate granted-ability
     * records have no hook for it.
     */
    @SerialName("UntilAffectedOwnersNextTurn")
    @Serializable
    data object UntilAffectedOwnersNextTurn : Duration {
        override val description = "until its owner's next turn"
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
     * Effect lasts for as long as its controller controls the source **and** the source remains
     * tapped — the conjunction of [WhileYouControlSource] and [WhileSourceTapped].
     *
     * Seasinger (Fallen Empires) is the card that prints both halves in one clause: "Gain control
     * of target creature whose controller controls an Island for as long as you control this
     * creature and this creature remains tapped." Neither half alone is the printed duration: an
     * opponent stealing Seasinger returns the borrowed creature even though Seasinger is still
     * tapped, and untapping Seasinger returns it even though you still control it.
     *
     * Gated per-frame by `StateProjector` — the battlefield and tapped halves when the floating
     * effect is collected, the source-controller half after Layer 2 alongside
     * [WhileYouControlSource] — and one-way per CR 611.2b: `EndedDurationExpiryCheck` physically
     * removes the effect once either half fails, so re-tapping or regaining control does not
     * re-steal the creature.
     */
    @SerialName("WhileYouControlSourceAndSourceTapped")
    @Serializable
    data class WhileYouControlSourceAndSourceTapped(
        val sourceDescription: String = "this creature"
    ) : Duration {
        override val description =
            "for as long as you control $sourceDescription and $sourceDescription remains tapped"
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
     * Effect lasts until the effect's source *card* is cast from exile — "Target land gains
     * '{T}: Add {C}{C}' until this card is cast from exile" (Emrakul, the Exigent Doom), where the
     * card exiled itself from hand as part of the ability's cost.
     *
     * An *event*-bounded duration, not a "for as long as …" gate: it ends only when the very
     * object that was in exile when the effect began is cast. If that card leaves exile any other
     * way it becomes a new object (CR 400.7) that can never be "cast from exile" as this card, so
     * the effect simply lasts indefinitely — which is also what happens if the card had already
     * left exile before the ability resolved.
     *
     * Honoured by the activated-ability grant ([com.wingedsheep.sdk.scripting.effects
     * .GrantActivatedAbilityEffect]): the grant records the source's exile object when it is made,
     * and casting that object from exile removes the grant as the spell moves to the stack. Other
     * grant/effect stores do not read it yet.
     */
    @SerialName("UntilSourceCastFromExile")
    @Serializable
    data object UntilSourceCastFromExile : Duration {
        override val description = "until this card is cast from exile"
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
    val EndOfYourNextTurn = Duration.EndOfYourNextTurn
    val UntilNextEndStep = Duration.UntilNextEndStep
    val EndOfCombat = Duration.EndOfCombat
    val Permanent = Duration.Permanent
    val UntilSourceCastFromExile = Duration.UntilSourceCastFromExile

    fun whileOnBattlefield(source: String = "this permanent") =
        Duration.WhileSourceOnBattlefield(source)

    fun whileSourceTapped(source: String = "this creature") =
        Duration.WhileSourceTapped(source)

    val WhileAffectedTapped = Duration.WhileAffectedTapped

    fun whileYouControlSource(source: String = "this permanent") =
        Duration.WhileYouControlSource(source)

    fun whileAffectedHasCounter(counterType: CounterType) =
        Duration.WhileAffectedHasCounter(counterType)

    fun untilPhase(phase: String) = Duration.UntilPhase(phase)
    fun until(condition: String) = Duration.UntilCondition(condition)
}
