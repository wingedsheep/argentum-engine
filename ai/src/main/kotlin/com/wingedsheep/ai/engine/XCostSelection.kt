package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.TargetInfo
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng

/**
 * Choosing a value for `{X}`.
 *
 * The legal-action enumerator cannot pick X for the AI: X is announced as part of casting
 * (CR 601.2b), so enumeration runs *before* it exists. It hands over the raw materials instead —
 * [LegalAction.maxAffordableX], [LegalAction.minX], and a deliberately permissive
 * [LegalAction.validTargets] — and leaves the choice to whoever is casting. A human makes it in the
 * client's X-selection phase; this is the AI's equivalent.
 *
 * The output is one fully-consistent [LegalAction] per X worth considering, so nothing downstream
 * has to know X was ever open: [Strategist] simulates and scores them like any other candidate, and
 * [TargetSelection] picks targets from a list already narrowed to what is legal at that X.
 *
 * Three entry points, by how much the caller can afford to spend:
 *
 * - [expandToX] — every X worth a simulation. [Strategist]'s candidate expansion.
 * - [bindBestX] — the single best-looking X, no simulation.
 * - [sampleX] — one X drawn at random from the same set, for a caller that must stay stochastic.
 *
 * Two things this is deliberately *not*:
 *
 * - **Not a scorer.** It proposes X values; simulation decides between them. The only judgement
 *   here is which handful are worth a simulation, because the affordable range can be a dozen wide
 *   and each candidate costs a full simulation.
 * - **Not a target picker.** It narrows `validTargets` to what the chosen X permits and leaves the
 *   choice among the survivors to [TargetSelection], exactly as the server leaves it to the client.
 */
object XCostSelection {

    /**
     * Cap on how many X values one action is expanded into. Each is a simulation in the
     * Strategist's first pass, so the affordable range is sampled rather than swept.
     *
     * Applied by [expandToX] *after* narrowing, so an X that cannot legally be cast is dropped
     * rather than spending a slot the next X down would have used.
     */
    const val MAX_X_CANDIDATES = 5

    /**
     * The X values worth considering for [action], best-first — the **uncapped** proposal, not the
     * final candidate set. [expandToX] applies [MAX_X_CANDIDATES] once each has been narrowed.
     *
     * Two shapes, because "what is a good X" has two different answers:
     *
     * - **X gates which targets are legal** ("mana value X or less", "mana value X", "power X" —
     *   Repeal, Spell Blast, Ent-Draught Basin). X is a function of the target, not a free choice,
     *   so the candidates are exactly the values that make some currently-legal target legal.
     *   Sweeping the affordable range here would mostly generate X values no target matches,
     *   spending the candidate budget on casts that cannot be made.
     * - **X is free of the targets** (Fireball, Genesis Wave, Day of Black Sun, and "up to X target
     *   creatures", where X caps the count rather than gating legality). More X is more effect, so
     *   the top affordable values are the interesting ones.
     *
     * Returns an empty list when no X can legally be chosen — the caller's signal to drop the
     * action rather than submit it at the enumerator's implicit X=0.
     */
    fun candidateXValues(state: GameState, action: LegalAction): List<Int> {
        val maxX = action.maxAffordableX ?: return emptyList()
        val minX = action.minX.coerceAtLeast(0)
        if (maxX < minX) return emptyList()

        targetGatedXValues(state, action)?.let { gated ->
            return gated.filter { it in minX..maxX }
        }

        // A free X of 0 is the enumerator's own default and buys nothing, so the sweep starts at 1
        // unless the card forbids it ("X can't be 0" raises `minX`).
        val lowest = maxOf(minX, 1)
        if (maxX < lowest) return emptyList()
        return (maxX downTo lowest).toList()
    }

    /**
     * [action] as one fully-narrowed [LegalAction] per X worth simulating, best-first, at most
     * [MAX_X_CANDIDATES] of them.
     *
     * Empty when no X can legally be cast — every affordable value leaves a mandatory target slot
     * with nothing to point at. That is the caller's signal to drop the action outright, which
     * beats offering the bare one: submitted at the enumerator's implicit X=0 it would fizzle.
     */
    fun expandToX(state: GameState, action: LegalAction): List<LegalAction> =
        narrowedCandidates(state, action).take(MAX_X_CANDIDATES).toList()

    /**
     * Bind the single best-looking X to [action] — the caller-picks-one form of [expandToX], for a
     * caller that cannot afford to simulate the alternatives.
     *
     * Returns [action] unchanged when it has no X to bind, or when no candidate X survives
     * narrowing — the caller has already committed to this action, so an unbound X (which the
     * engine reads as 0) beats returning nothing.
     */
    fun bindBestX(state: GameState, action: LegalAction): LegalAction {
        if (!action.hasXCost) return action
        return narrowedCandidates(state, action).firstOrNull() ?: action
    }

    /**
     * One X drawn uniformly from [expandToX]'s candidates, with the advanced generator.
     *
     * For [com.wingedsheep.ai.engine.rollout.PlayoutPolicy], whose contract is that it must not
     * simulate *and* must not be deterministic: always taking the head would cast every X spell in
     * every playout for the largest affordable X, collapsing R playouts of that line into one
     * sample of it. Narrowing reads the board and plays nothing forward, so this stays inside the
     * no-simulation rule.
     *
     * Returns [action] and the generator untouched when there is no X to bind, matching
     * [bindBestX] — the caller has already committed to the action.
     */
    fun sampleX(state: GameState, action: LegalAction, rng: GameRng): Pair<LegalAction, GameRng> {
        if (!action.hasXCost) return action to rng
        val candidates = expandToX(state, action)
        if (candidates.isEmpty()) return action to rng
        return rng.pick(candidates)
    }

    /**
     * The proposals of [candidateXValues], each narrowed to a consistent action, unsatisfiable ones
     * dropped. Lazy so a caller that wants one X pays for one narrowing.
     */
    private fun narrowedCandidates(state: GameState, action: LegalAction): Sequence<LegalAction> =
        candidateXValues(state, action).asSequence().mapNotNull { x ->
            narrowToX(state, action, x)?.withXValue(x)
        }

    /** This action with [x] written into whichever X-carrying `GameAction` shape it wraps. */
    private fun LegalAction.withXValue(x: Int): LegalAction = when (val base = action) {
        is CastSpell -> copy(action = base.copy(xValue = x))
        is ActivateAbility -> copy(action = base.copy(xValue = x))
        else -> this
    }

    /**
     * Re-derive [action] as it would look with X bound to [x]: X-gated target lists narrowed to
     * what is legal, and an X-driven target cap resolved to the real number.
     *
     * This mirrors what the web client does once the player picks X (`pipelinePhases.ts`'s
     * `applyXFilters` / `resolveMaxByX`) — the enumerator is permissive on purpose, and whoever
     * binds X owes the narrowing. Without it the AI would choose a target the server then rejects.
     *
     * Returns null when the narrowing leaves a mandatory requirement with nothing to target, i.e.
     * this X cannot legally be cast at all.
     */
    fun narrowToX(state: GameState, action: LegalAction, x: Int): LegalAction? {
        val requirements = action.targetRequirements
        if (requirements != null) {
            val narrowed = requirements.map { narrowRequirement(state, it, x) ?: return null }
            val first = narrowed.firstOrNull() ?: return action.copy(targetRequirements = narrowed)
            return action.withFlatViewOf(first).copy(targetRequirements = narrowed)
        }

        val targets = action.validTargets ?: return action
        val flat = narrowRequirement(state, flatRequirement(action, targets), x) ?: return null
        return action.withFlatViewOf(flat)
    }

    /**
     * [requirement] written back into the flat target fields.
     *
     * The enumerator mirrors requirement 0 into them for a multi-requirement action, and
     * [TargetSelection.targetInfosFor] reads [LegalAction.targetRequirements] in preference to
     * them. Deriving the flat view from the already-narrowed requirement rather than re-narrowing
     * the flat shape keeps the two from disagreeing, and stops the discarded view from failing on
     * its own and taking a legal X down with it.
     */
    private fun LegalAction.withFlatViewOf(requirement: TargetInfo): LegalAction = copy(
        validTargets = validTargets?.let { requirement.validTargets },
        targetCount = requirement.maxTargets,
        minTargets = requirement.minTargets,
    )

    /**
     * Narrow one requirement to [x], or null when doing so makes it unsatisfiable.
     *
     * A requirement is unsatisfiable when it *must* be filled ([TargetInfo.minTargets] > 0) and
     * either nothing legal survives the filter or the X-driven cap has fallen below the minimum.
     * A requirement that may be left empty ("up to X target creatures") is never fatal — casting it
     * for nothing is legal, and the Strategist scores that against passing like any other line.
     */
    private fun narrowRequirement(state: GameState, requirement: TargetInfo, x: Int): TargetInfo? {
        val valid = filterByX(
            state, requirement.validTargets, x,
            atMostManaValue = requirement.xConstrainsManaValue,
            exactManaValue = requirement.xConstrainsManaValueExactly,
            exactPower = requirement.xConstrainsPower,
        )
        // An X-driven cap *replaces* the enumerator's placeholder rather than clamping it: at
        // enumeration time the count could not be resolved, so the static value carries no
        // information (see LegalAction.targetCount).
        val maxTargets = if (requirement.xConstrainsCount) x else requirement.maxTargets
        if (requirement.minTargets > 0 && (valid.isEmpty() || maxTargets < requirement.minTargets)) {
            return null
        }
        return requirement.copy(
            validTargets = valid,
            maxTargets = maxTargets,
            minTargets = minOf(requirement.minTargets, maxTargets),
        )
    }

    /**
     * The single-requirement shape ([LegalAction.validTargets] plus the flat `xConstrains*` fields)
     * expressed as a [TargetInfo], so one set of rules covers both shapes.
     *
     * [LegalAction.minTargets] is carried across as-is. It defaults to [LegalAction.targetCount]
     * (itself 1), so a `requiresTargets` action is already floored at 1 without help — and forcing
     * a floor here would make a genuinely optional slot ("up to one target permanent with mana
     * value X or less", which the enumerator still flags `requiresTargets`) fatal the moment X
     * empties it.
     */
    private fun flatRequirement(action: LegalAction, targets: List<EntityId>): TargetInfo = TargetInfo(
        index = 0,
        description = action.targetDescription ?: "",
        minTargets = action.minTargets,
        maxTargets = action.targetCount,
        validTargets = targets,
        xConstrainsManaValue = action.xConstrainsTargetManaValue,
        xConstrainsManaValueExactly = action.xConstrainsTargetManaValueExactly,
        xConstrainsPower = action.xConstrainsTargetPower,
        xConstrainsCount = action.xConstrainsTargetCount,
    )

    /**
     * The X values that make some currently-legal target legal, best target first — or null when no
     * requirement gates target legality on X, which tells [candidateXValues] to sweep instead.
     *
     * For "mana value X **or less**" the candidate is the target's own mana value: any larger X hits
     * the same permanent for more mana, so it is dominated. For the equality forms the candidate is
     * simply the value that matches.
     *
     * Every gating requirement proposes independently, so with two of them the list is a union and
     * some entries satisfy only one. Those are not filtered here: [narrowToX] already drops an X
     * that empties any mandatory requirement, and [expandToX] caps *after* that, so a proposal that
     * cannot be cast costs a narrowing rather than a candidate slot.
     *
     * Ordered by descending value (the biggest thing X could reach first).
     */
    private fun targetGatedXValues(state: GameState, action: LegalAction): List<Int>? {
        val requirements = allRequirements(action)
        if (requirements.none { it.gatesTargetLegalityOnX }) return null
        return requirements
            .flatMap { requirement ->
                requirement.validTargets.mapNotNull { id ->
                    when {
                        requirement.xConstrainsManaValue || requirement.xConstrainsManaValueExactly ->
                            manaValueOf(state, id)
                        requirement.xConstrainsPower -> state.projectedState.getPower(id)
                        else -> null
                    }
                }
            }
            .distinct()
            .sortedDescending()
    }

    /** Whether this requirement's legal targets depend on the chosen X, rather than only its count. */
    private val TargetInfo.gatesTargetLegalityOnX: Boolean
        get() = xConstrainsManaValue || xConstrainsManaValueExactly || xConstrainsPower

    /** Every requirement of [action], in either shape, as one list. */
    private fun allRequirements(action: LegalAction): List<TargetInfo> =
        action.targetRequirements
            ?: action.validTargets?.let { listOf(flatRequirement(action, it)) }
            ?: emptyList()

    private fun filterByX(
        state: GameState,
        ids: List<EntityId>,
        x: Int,
        atMostManaValue: Boolean,
        exactManaValue: Boolean,
        exactPower: Boolean,
    ): List<EntityId> {
        if (!atMostManaValue && !exactManaValue && !exactPower) return ids
        return ids.filter { id ->
            (!atMostManaValue || (manaValueOf(state, id) ?: return@filter false) <= x) &&
                (!exactManaValue || manaValueOf(state, id) == x) &&
                // Power is a projected characteristic — a lord's +1/+1 changes what "power X" can
                // reach, so this must not read the printed stats.
                (!exactPower || state.projectedState.getPower(id) == x)
        }
    }

    /**
     * The mana value the engine's own target filter will see.
     *
     * A face-down permanent has no mana cost and so mana value 0 (CR 708.2a), whatever is printed
     * on the card — which is what `PredicateEvaluator` applies for `ManaValueAtMostX` and
     * `ManaValueEqualsX`. Reading [CardComponent.manaValue] flat would derive X from a morph's
     * printed cost and pick a target the engine then rejects.
     */
    private fun manaValueOf(state: GameState, id: EntityId): Int? =
        if (state.projectedState.isFaceDown(id)) 0
        else state.getEntity(id)?.get<CardComponent>()?.manaValue
}
