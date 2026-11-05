package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.evaluation.BoardEvaluator
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration

/**
 * What an opponent can still do, with the permanents it has on the battlefield, to a spell or
 * ability the AI aims at its permanents — played out, not guessed.
 *
 * [Strategist] scores a candidate by simulating it to the quiet state it leads to, and that
 * simulation passes priority for everyone: it scores a world in which the opponent never uses the
 * untapped hexproof granter beside its best creature. So a removal spell aimed at that creature
 * read as a clean kill, and the AI fired it into the granter — off game `74644ce0` (2026-11-04),
 * Prophesied End at Kiora of Fire and Ashes with Liliana the Faultless ("{1}, {T}, Discard a card:
 * Another target creature or planeswalker you control gains hexproof until end of turn") untapped;
 * the spell fizzled, and a second removal spell only resolved because Liliana was by then tapped.
 *
 * This is [CombatResponses]' counterpart on the stack. When a candidate targets permanents an
 * opponent controls, that opponent gets priority with the candidate on the stack and is offered
 * the non-mana activated abilities its permanents can afford right then — the engine's own legal
 * actions, so mana, tap costs, summoning sickness and once-per-turn limits are the rules' answer,
 * with a selection cost (Liliana's discard) paid as the AI would pay its own
 * ([SelectionCostPayment]) — narrowed to the ones that can touch what the candidate targets: a
 * targeted ability aimed at one of those permanents, or any ability of one of them. Each is
 * simulated to the quiet state, and only an answer that **saves** a target counts — a permanent the
 * candidate removes when everyone passes that is still on the battlefield after the answer
 * (hexproof or protection fizzling the spell, indestructible, a regeneration shield). The opponent
 * takes the one that leaves the AI worst off, *if* it beats letting the candidate resolve by
 * [CombatResponses.MIN_GAIN]: an answer that is possible but pointless is not assumed, and an
 * ability that only does what its controller could do at any other moment (Kiora of Fire and
 * Ashes making a Dragon in response to removal on Kiora) is not an answer to this spell.
 *
 * Aiming the removal at the granter itself is then what scores best: the granter cannot protect
 * itself ("another target"), and the next removal spell meets no answer.
 *
 * **Visible information only.** Only abilities of permanents on the battlefield are offered;
 * nothing in a hidden hand, graveyard or library is enumerated.
 *
 * **Cost.** Gated per decision ([respondersAt], cached on the decision's root state): a board where
 * no opponent has an affordable non-mana activated ability costs one enumeration per decision and
 * nothing per candidate. Where one does, a targeting candidate costs one cast step plus at most
 * [MAX_OFFERS] extra simulations. Rollout playouts never build one.
 */
internal class TargetResponses(
    private val simulator: GameSimulator,
    private val evaluator: BoardEvaluator,
) {
    private class Gate(val root: GameState, val viewer: EntityId, val responders: Set<EntityId>)

    @Volatile
    private var gate: Gate? = null

    /**
     * The opponents of [viewer] who have, at [root], an affordable non-mana activated ability.
     * Empty means nothing here can change a candidate. Cached on [root]'s identity: every
     * candidate of one decision is simulated from the same root.
     */
    fun respondersAt(root: GameState, viewer: EntityId): Set<EntityId> {
        gate?.let { if (it.root === root && it.viewer == viewer) return it.responders }
        val responders = root.getOpponents(viewer).filter { opponent ->
            simulator.getPermanentAbilityActions(root, opponent).any(::usable)
        }.toSet()
        gate = Gate(root, viewer, responders)
        return responders
    }

    /**
     * [resolved] — what [action] leads to when everyone passes — or, when an opponent whose
     * permanents [action] targets has a visible answer that leaves [viewer] worse off by more
     * than [CombatResponses.MIN_GAIN], the line where it uses that answer.
     */
    fun answered(state: GameState, action: GameAction, viewer: EntityId, resolved: SimulationResult): SimulationResult {
        if (resolved !is SimulationResult.Terminal || resolved.state.gameOver) return resolved
        val targeted = targetedPermanents(action)
        if (targeted.isEmpty()) return resolved
        val responders = respondersAt(state, viewer)
        if (responders.isEmpty()) return resolved
        val projected = state.projectedState
        val resolvedField = resolved.state.getBattlefield().toHashSet()
        var worst: Pair<SimulationResult, Double>? = null
        val baseline by lazy(LazyThreadSafetyMode.NONE) { settledScore(state, resolved.state, viewer, responders) }
        for (responder in responders) {
            val theirs = targeted.filterTo(LinkedHashSet()) { projected.getController(it) == responder }
            if (theirs.isEmpty()) continue
            val window = windowFor(state, action, responder) ?: continue
            for (answer in offers(window, responder, theirs)) {
                val result = simulator.simulate(window, answer)
                if (result !is SimulationResult.Terminal) continue
                // Only an answer that saves a target counts. An ability that merely does what its
                // controller could do at any other moment — Kiora's {8} Dragon in response to
                // removal on Kiora — is not an answer to this spell, and with the responder's mana
                // read as untapped by the settle it would look free.
                if (theirs.none { it !in resolvedField && it in result.state.getBattlefield() }) continue
                val score = settledScore(state, result.state, viewer, responders)
                if (worst == null || score < worst.second) worst = result to score
            }
        }
        val best = worst ?: return resolved
        return if (baseline - best.second > CombatResponses.MIN_GAIN) best.first else resolved
    }

    /**
     * The permanents whose activated abilities [responder] can afford to use at [state] — the
     * answers its board holds up. A source missing from a later state's set (dead, tapped, its
     * controller's mana spent) is an answer that line took away.
     */
    fun answerSources(state: GameState, responder: EntityId): Set<EntityId> =
        simulator.getPermanentAbilityActions(state, responder)
            .filter(::usable)
            .mapTo(HashSet()) { (it.action as ActivateAbility).sourceId }

    /** Whether [action] targets a permanent one of [players] controls at [state]. */
    fun targetsPermanentOf(state: GameState, action: GameAction, players: Set<EntityId>): Boolean {
        val projected = state.projectedState
        return targetedPermanents(action).any { projected.getController(it) in players }
    }

    /** The permanents [action] targets. */
    private fun targetedPermanents(action: GameAction): Set<EntityId> {
        val targets = when (action) {
            is CastSpell -> action.targets
            is ActivateAbility -> action.targets
            else -> return emptySet()
        }
        return targets.mapNotNullTo(LinkedHashSet()) { (it as? ChosenTarget.Permanent)?.entityId }
    }

    /**
     * [action] put on the stack, and priority passed around to [responder] with it still there —
     * null when the cast pauses, is refused, or the stack moves before [responder] gets to act.
     */
    private fun windowFor(state: GameState, action: GameAction, responder: EntityId): GameState? {
        var current = simulator.step(state, action) ?: return null
        val depth = current.stack.size
        if (depth == 0) return null
        repeat(state.turnOrder.size) {
            val priority = current.priorityPlayerId ?: return null
            if (priority == responder) return current
            current = simulator.step(current, PassPriority(priority)) ?: return null
            if (current.stack.size != depth) return null
        }
        return null
    }

    /**
     * Concrete activations [responder] could answer with at [window]: an ability aimed at one of
     * [theirs], or any ability one of [theirs] has. At most [MAX_OFFERS].
     */
    private fun offers(window: GameState, responder: EntityId, theirs: Set<EntityId>): List<GameAction> {
        val out = LinkedHashSet<GameAction>()
        for (offer in simulator.getPermanentAbilityActions(window, responder)) {
            if (!usable(offer)) continue
            val activation = offer.action as ActivateAbility
            val bound = XCostSelection.bindBestX(window, offer)
            val aimed: List<GameAction> = if (!bound.requiresTargets) {
                if (activation.sourceId in theirs) listOf(bound.action) else emptyList()
            } else {
                val single = TargetSelection.targetInfosFor(bound)?.singleOrNull()
                if (single == null || single.maxTargets != 1) {
                    if (activation.sourceId in theirs) {
                        listOf(TargetSelection.fillHeuristically(window, bound, responder, fillPartialRequirements = true))
                    } else emptyList()
                } else {
                    single.validTargets.filter { it in theirs }.map { target ->
                        TargetSelection.applyTargets(
                            bound.action, listOf(TargetSelection.toChosenTarget(window, single, target, responder)),
                        )
                    }
                }
            }
            for (each in aimed) {
                out += SelectionCostPayment.withPlainCosts(window, offer, each)
                if (out.size >= MAX_OFFERS) return out.toList()
            }
        }
        return out.toList()
    }

    private fun usable(offer: LegalAction): Boolean =
        offer.affordable && !offer.isManaAbility && !offer.hasUnfillableTargetRequirement && offer.action is ActivateAbility

    /**
     * [viewer]'s score of [end] read as it stands once this turn is over: until-end-of-turn
     * effects created since [start] gone, and — off their own turn — the lands the responders
     * tapped untapped, so the mana an answer costs on our turn is not counted against them.
     */
    private fun settledScore(start: GameState, end: GameState, viewer: EntityId, responders: Set<EntityId>): Double {
        val before = start.floatingEffects.mapTo(HashSet()) { it.id }
        var settled = end.copy(
            floatingEffects = end.floatingEffects.filterNot { it.id !in before && it.duration == Duration.EndOfTurn },
        )
        for (responder in responders) {
            if (start.isActiveTurnFor(responder)) continue
            val projected = settled.projectedState
            for (id in projected.getBattlefieldControlledBy(responder)) {
                if (!projected.hasType(id, "LAND")) continue
                if (start.getEntity(id)?.has<TappedComponent>() == true) continue
                if (settled.getEntity(id)?.has<TappedComponent>() == true) {
                    settled = settled.updateEntity(id) { it.without<TappedComponent>() }
                }
            }
        }
        return evaluator.evaluate(settled, settled.projectedState, viewer)
    }

    companion object {
        /** Concrete activations tried as answers to one candidate. */
        const val MAX_OFFERS = 8
    }
}
