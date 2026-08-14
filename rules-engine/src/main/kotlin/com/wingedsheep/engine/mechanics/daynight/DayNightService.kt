package com.wingedsheep.engine.mechanics.daynight

import com.wingedsheep.engine.core.DayNightChangedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.effects.permanent.types.flipDfcInPlace
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.sdk.core.DayNight
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId

/**
 * The one place the game's **day/night** designation changes (CR 731), and the one place the
 * daybound/nightbound transforms that a designation change entails are applied (CR 702.145b/c/e/f).
 *
 * It is to `GameState.dayNight` what `SpeedService` is to a player's speed: every writer routes through
 * here so the rules that govern the value — and, crucially, the werewolf transforms that ride on it —
 * can't drift apart between call sites. There are exactly three writers:
 *
 *  - the untap-step turn-based action (CR 502.2 / 731.2), called from
 *    [com.wingedsheep.engine.core.BeginningPhaseManager];
 *  - the [DayNightCheck] state-based sweep, for the "you control a daybound/nightbound permanent and
 *    it's neither day nor night" designation starts (CR 702.145d/g);
 *  - the [com.wingedsheep.sdk.scripting.effects.SetDayNightEffect] executor, behind "it becomes
 *    day"/"it becomes night" text (e.g. Into the Night).
 *
 * **Why the transform cascade lives here, in the same call.** CR 702.145b's second ability ("as it
 * becomes night, if this permanent is front face up, transform it") and 702.145e's ("as it becomes day
 * … transform it") make the transforms a *consequence* of the designation change. Emitting the
 * `TransformedEvent`s in the very same event batch as the [DayNightChangedEvent] guarantees they reach
 * `TriggerDetector.detectTriggers` on the same path the designation change travels — so a werewolf's
 * "whenever this transforms" ability (Wildsong Howler) fires whether day/night flipped in the untap
 * step or via an effect. The lingering "any time … it's night" catches of 702.145c/f (a permanent that
 * *arrives* while a designation already holds) are the [DayNightCheck] SBA's job, using the same
 * idempotent [applyTransformCascade] sweep.
 */
object DayNightService {

    /**
     * The untap step's day/night turn-based action (CR 502.2 / 731.2). Reads the previous turn's active
     * active side's per-player spell counts — snapshotted onto
     * [GameState.previousTurnActiveTeamSpellCounts] by `TurnManager.startTurn` before the per-turn
     * counters reset — and changes the designation if the check warrants it:
     *  - **731.2a** it's day and nobody on that side cast a spell → it becomes night;
     *  - **731.2b** it's night and any player on that side cast two or more spells → it becomes day;
     *  - **731.2c** it's neither → no check happens and it stays neither.
     *
     * Returns the state and events unchanged in the 731.2c case and whenever the check's condition isn't
     * met, so the untap step can call it unconditionally every turn. When it does flip the designation the
     * returned events carry the [DayNightChangedEvent] plus any [flipDfcInPlace] `TransformedEvent`s the
     * flip cascades (CR 702.145b#2 / e#1) — see [set].
     */
    fun checkUntapStepDesignation(
        state: GameState,
        cardRegistry: CardRegistry
    ): Pair<GameState, List<GameEvent>> = when (state.dayNight) {
        // 731.2c — neither day nor night: the check doesn't happen.
        null -> state to emptyList()
        // 731.2a / 502.2a — day + nobody on the previous active side cast a spell → night.
        DayNight.DAY ->
            if (state.previousTurnActiveTeamSpellCounts.values.all { it == 0 }) {
                becomeNight(state, cardRegistry, UNTAP_STEP_SOURCE)
            } else {
                state to emptyList()
            }
        // 731.2b / 502.2a — night + any player on the previous active side cast 2+ → day.
        DayNight.NIGHT ->
            if (state.previousTurnActiveTeamSpellCounts.values.any { it >= 2 }) {
                becomeDay(state, cardRegistry, UNTAP_STEP_SOURCE)
            } else {
                state to emptyList()
            }
    }

    /**
     * Apply daybound's "enters transformed" entry modification to [entityId].
     *
     * The permanent must already have its card identity and battlefield controller. Non-spell entry
     * paths do not otherwise create a [DoubleFacedComponent], so this helper creates the front-face
     * identity before applying the night-side entry. The returned transform event is deliberately
     * discarded: entering transformed is not transforming, so transform triggers must not fire.
     */
    fun applyDayboundEntry(
        state: GameState,
        cardRegistry: CardRegistry,
        entityId: EntityId
    ): GameState {
        val entity = state.getEntity(entityId) ?: return state
        val cardDefinitionId =
            entity.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                ?.cardDefinitionId
                ?: return state
        val cardDefinition = cardRegistry.getCard(cardDefinitionId) ?: return state
        if (Keyword.DAYBOUND !in cardDefinition.keywords || !cardDefinition.isDoubleFaced) return state

        val withDfc = if (entity.get<DoubleFacedComponent>() == null) {
            state.updateEntity(entityId) { container ->
                container.with(
                    DoubleFacedComponent(
                        frontCardDefinitionId = cardDefinition.name,
                        backCardDefinitionId = cardDefinition.backFace!!.name,
                        currentFace = DoubleFacedComponent.Face.FRONT,
                    )
                )
            }
        } else {
            state
        }
        if (withDfc.dayNight != DayNight.NIGHT) return withDfc
        val dfc = withDfc.getEntity(entityId)?.get<DoubleFacedComponent>() ?: return withDfc
        if (dfc.currentFace != DoubleFacedComponent.Face.FRONT) return withDfc
        return flipDfcInPlace(withDfc, cardRegistry, entityId)?.first ?: withDfc
    }

    /** "It becomes day" (CR 731.1 / 702.145). */
    fun becomeDay(state: GameState, cardRegistry: CardRegistry, sourceName: String) =
        set(state, cardRegistry, DayNight.DAY, sourceName)

    /** "It becomes night" (CR 731.1 / 702.145). */
    fun becomeNight(state: GameState, cardRegistry: CardRegistry, sourceName: String) =
        set(state, cardRegistry, DayNight.NIGHT, sourceName)

    /**
     * Give the game the [target] designation (CR 731.1). A [target] equal to the current designation is
     * a no-op returning the state unchanged and no events — which is what makes the untap-step check and
     * the SBA idempotent across the many times they run. When the designation actually changes, this
     * emits a [DayNightChangedEvent] and then runs [applyTransformCascade], so the returned event list
     * carries the designation change followed by one `TransformedEvent` per permanent that transforms in
     * response (CR 702.145b#2 / 702.145e#1).
     */
    fun set(
        state: GameState,
        cardRegistry: CardRegistry,
        target: DayNight,
        sourceName: String
    ): Pair<GameState, List<GameEvent>> {
        val current = state.dayNight
        if (current == target) return state to emptyList()

        val afterChange = state.copy(dayNight = target)
        val events = mutableListOf<GameEvent>(
            DayNightChangedEvent(
                oldDesignation = current,
                newDesignation = target,
                sourceName = sourceName
            )
        )

        val (afterCascade, cascadeEvents) = applyTransformCascade(afterChange, cardRegistry)
        events.addAll(cascadeEvents)
        return afterCascade to events
    }

    /**
     * Transform every permanent whose daybound/nightbound keyword is out of step with the current
     * designation, to fixpoint (CR 702.145c/f):
     *  - while it's **night**, each front-face-up permanent with **daybound** transforms to its back;
     *  - while it's **day**, each back-face-up permanent with **nightbound** transforms to its front.
     *
     * Idempotent: after a permanent transforms it no longer matches (a daybound front face becomes a
     * back face, etc.), so re-running the sweep is a no-op — which is why both [set] (on a designation
     * change) and [DayNightCheck] (as a lingering-condition catch) can call it freely. Keyword presence
     * and control are read from projected state so a *granted* daybound/nightbound and control-changing
     * effects are honoured. Returns the state unchanged and no events when it's neither day nor night
     * (CR 731 — no designation, nothing to reconcile).
     */
    fun applyTransformCascade(
        state: GameState,
        cardRegistry: CardRegistry
    ): Pair<GameState, List<GameEvent>> {
        val designation = state.dayNight ?: return state to emptyList()

        var newState = state
        val events = mutableListOf<GameEvent>()

        // Fixpoint loop: transforming one permanent can't create a new mismatch (the flipped face has
        // the opposite keyword), but re-projecting after each flip keeps keyword/face reads honest and
        // guards against any granted-keyword interaction. Bounded by the battlefield size.
        var changed = true
        var guard = 0
        while (changed && guard++ < MAX_CASCADE_ITERATIONS) {
            changed = false
            val projected = newState.projectedState
            for (entityId in newState.getBattlefield()) {
                val dfc = newState.getEntity(entityId)?.get<DoubleFacedComponent>() ?: continue
                val mismatched = when (designation) {
                    DayNight.NIGHT ->
                        dfc.currentFace == DoubleFacedComponent.Face.FRONT &&
                            projected.hasKeyword(entityId, Keyword.DAYBOUND)
                    DayNight.DAY ->
                        dfc.currentFace == DoubleFacedComponent.Face.BACK &&
                            projected.hasKeyword(entityId, Keyword.NIGHTBOUND)
                }
                if (!mismatched) continue

                val (flipped, event) = flipDfcInPlace(newState, cardRegistry, entityId) ?: continue
                newState = flipped
                events.add(event)
                changed = true
            }
        }

        return newState to events
    }

    private const val MAX_CASCADE_ITERATIONS = 100

    /** Attributed source on a [DayNightChangedEvent] produced by the CR 502.2/731.2 untap-step check. */
    private const val UNTAP_STEP_SOURCE = "Untap Step"
}
