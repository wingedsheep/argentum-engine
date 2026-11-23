package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.evaluation.BoardEvaluator
import com.wingedsheep.ai.engine.knowledge.CombatReach
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration

/**
 * What the other side of a fight can still do to it with the permanents it has on the battlefield —
 * played out, not guessed.
 *
 * [CombatAdvisor] scores an attack or a block by simulating the combat through the engine. Left to
 * itself that simulation passes priority for everyone, so it scores a world in which the opponent
 * never uses the Leonin Sun Standard sitting untapped beside two open Plains, the Prodigal Sorcerer
 * that could finish a damaged attacker, or the first-strike granter that turns a trade into a
 * blowout. An attack that only works while the blockers stay at their printed size read as safe.
 *
 * This component gives each opponent its windows back: right after attackers are declared (a
 * defender pumping or making a blocker before it blocks — the block it would then make is the
 * block predicted against the bigger board) and right after blockers are declared (either side's
 * classic trick window). At each window the opponent is offered every non-mana activated ability
 * its permanents can afford right now — the engine's own legal actions, so mana, tap costs,
 * summoning sickness, sacrifice costs and once-per-turn limits are the rules' answer rather than a
 * model of them, with a selection cost paid as the AI would pay its own ([SelectionCostPayment]) — narrowed to the ones whose effect can touch a fight ([CombatReach]), with their
 * targets drawn from the creatures in the fight and repeatable pumps tried at each affordable
 * count. Each is simulated through the rest of combat, and the opponent takes the one that leaves
 * the AI worst off, *if* it beats passing by [MIN_GAIN]: a trick that is possible but bad for them
 * — or merely neutral — is not assumed. Greedily, so a second ability can follow the first while
 * mana remains.
 *
 * **Visible information only.** Only abilities of permanents on the battlefield are offered;
 * nothing in a hidden hand, graveyard or library is enumerated.
 *
 * **Cost.** The whole model is gated per decision ([respondersAt], cached on the decision's root
 * state): a board where no opponent has an affordable combat ability costs one enumeration per
 * decision and nothing per simulated plan. Where one does, a plan costs a bounded number of extra
 * combat simulations ([MAX_RESPONSE_SIMULATIONS]). Rollout playouts never build one.
 *
 * The comparison between two responses reads the board as it will stand once the turn is over —
 * until-end-of-turn effects and marked damage gone, and on the AI's turn the responder's lands
 * untapped — so a pump that changes nothing in the fight never looks like a gain for them, and
 * mana spent on our turn is not counted against them a second time.
 */
internal class CombatResponses(
    private val simulator: GameSimulator,
    private val evaluator: BoardEvaluator,
    private val cardRegistry: CardRegistry,
) {
    /** When in combat an opponent may answer. */
    private enum class Window { BEFORE_BLOCKS, AFTER_BLOCKS }

    private class Gate(val root: GameState, val viewer: EntityId, val responders: Set<EntityId>)

    @Volatile
    private var gate: Gate? = null

    /**
     * The opponents of [viewer] who have, at [root], an affordable activated ability that can
     * touch a fight. Empty means the model has nothing to add to this decision. Cached on [root]'s
     * identity: every plan of one decision is simulated from the same root.
     */
    fun respondersAt(root: GameState, viewer: EntityId): Set<EntityId> {
        gate?.let { if (it.root === root && it.viewer == viewer) return it.responders }
        val responders = root.getOpponents(viewer).filter { opponent ->
            val defending = !root.isActiveTurnFor(opponent)
            simulator.getPermanentAbilityActions(root, opponent).any { offer ->
                usable(offer) && reaches(root, offer.action as ActivateAbility, defending, window = null)
            }
        }.toSet()
        gate = Gate(root, viewer, responders)
        return responders
    }

    /**
     * Drive [state] — inside combat, attackers declared, nothing on the stack — to the end of
     * combat, letting each of [responders] answer at its windows.
     *
     * @param declareBlocks answers a defending player's declare-blockers step; null when blocks
     *   are already declared in [state].
     * @param emptyBlocksMustBeLegal a rejected declaration ends the simulation (null) instead of
     *   being skipped — for the caller that asks "what if they don't block", where a creature that
     *   must block makes the question meaningless.
     * @return the state after combat, with the until-end-of-turn effects the responses created —
     *   and the damage they were holding up — already worn off, as they will be before the AI next
     *   has to face that board; null when the combat could not be simulated.
     */
    fun playOut(
        state: GameState,
        viewer: EntityId,
        responders: Set<EntityId>,
        declareBlocks: ((GameState, LegalAction, EntityId) -> GameAction)?,
        emptyBlocksMustBeLegal: Boolean = false,
    ): GameState? {
        val start = state
        val run = Run(viewer, responders, declareBlocks, emptyBlocksMustBeLegal)
        val end = run.drive(state, respond = true) ?: return null
        return if (run.responded) settleResponses(start, end, responders) else end
    }

    /** One [playOut]: its budget, and whether any response was taken. */
    private inner class Run(
        val viewer: EntityId,
        val responders: Set<EntityId>,
        val declareBlocks: ((GameState, LegalAction, EntityId) -> GameAction)?,
        val emptyBlocksMustBeLegal: Boolean,
    ) {
        var simulationsLeft = MAX_RESPONSE_SIMULATIONS
        var responded = false

        /**
         * Pass priority through combat — declaring blocks when a defender is asked — until combat
         * ends. With [respond], each responder holding priority on an empty stack in a window
         * first gets [bestResponse]; without it everyone passes, which is the continuation a
         * candidate response is scored by.
         */
        fun drive(from: GameState, respond: Boolean, alreadyBlocked: Set<EntityId> = emptySet()): GameState? {
            var current = from
            // Defenders whose blocks are in: asked once, never again.
            val blocked = alreadyBlocked.toMutableSet()
            if (declareBlocks == null) blocked += current.turnOrder
            // Windows a responder has passed in, so it is not asked twice in one step.
            val passed = mutableSetOf<Pair<EntityId, Step>>()
            // The myopic score of the line the last response chose — the baseline of the next ask
            // in the same window, which is the same continuation.
            var carried: Pair<Step, Double>? = null
            var iterations = 0
            while (iterations++ < MAX_ITERATIONS && !current.gameOver && current.pendingDecision == null) {
                val priority = current.priorityPlayerId ?: break
                val declare = declareBlocks
                if (declare != null && priority != viewer && priority !in blocked && current.step == Step.DECLARE_BLOCKERS) {
                    val offer = simulator.getLegalActions(current, priority).find { it.actionType == "DeclareBlockers" }
                    if (offer != null) {
                        val transition = simulator.simulate(current, declare(current, offer, priority), stopBeforeCombatDamage = respond)
                        if (transition is SimulationResult.StoppedAtLimit) return null
                        if (emptyBlocksMustBeLegal && transition is SimulationResult.Illegal) return null
                        current = transition.state
                        blocked += priority
                        continue
                    }
                }
                val window = windowAt(current, priority)
                if (respond && window != null && priority in responders && (priority to current.step) !in passed) {
                    val baseline = carried?.takeIf { it.first == current.step }?.second
                    val answer = bestResponse(current, priority, window, baseline, blocked)
                    if (answer != null) {
                        responded = true
                        current = answer.first
                        carried = current.step to answer.second
                        continue
                    }
                    passed += priority to current.step
                }
                val transition = simulator.simulate(current, PassPriority(priority), stopBeforeCombatDamage = respond)
                if (transition is SimulationResult.StoppedAtLimit) return null
                current = transition.state
                if (current.phase != Phase.COMBAT) break
            }
            return current
        }

        /**
         * The response [responder] makes at [state], with the myopic score of the line it leads to
         * — or null when no affordable ability beats passing by [MIN_GAIN].
         */
        private fun bestResponse(
            state: GameState,
            responder: EntityId,
            window: Window,
            knownBaseline: Double?,
            blocked: Set<EntityId>,
        ): Pair<GameState, Double>? {
            if (simulationsLeft <= 0) return null
            val candidates = offers(state, responder, window)
            if (candidates.isEmpty()) return null
            val baseline = knownBaseline ?: run {
                simulationsLeft--
                val passed = drive(state, respond = false, alreadyBlocked = blocked) ?: return null
                settledScore(passed, responder)
            }
            var best: Pair<GameState, Double>? = null
            for (candidate in candidates) {
                if (simulationsLeft <= 0) break
                simulationsLeft--
                val activated = simulator.simulate(state, candidate, stopBeforeCombatDamage = true)
                if (activated !is SimulationResult.Terminal) continue
                val after = activated.state
                val outcome = if (after.gameOver) after else drive(after, respond = false, alreadyBlocked = blocked) ?: continue
                val score = settledScore(outcome, responder)
                if (best == null || score < best.second) best = after to score
            }
            return best?.takeIf { baseline - it.second > MIN_GAIN }
        }

        /** The viewer's score of [end] read as it stands once this turn is over. */
        private fun settledScore(end: GameState, responder: EntityId): Double {
            if (end.gameOver) return evaluator.evaluate(end, end.projectedState, viewer)
            var settled = end.copy(floatingEffects = end.floatingEffects.filter { it.duration != Duration.EndOfTurn })
            for (id in settled.getBattlefield()) {
                if (settled.getEntity(id)?.has<DamageComponent>() == true) {
                    settled = settled.updateEntity(id) { it.without<DamageComponent>() }
                }
            }
            if (!end.isActiveTurnFor(responder)) settled = untapLands(settled, responder) { true }
            return evaluator.evaluate(settled, settled.projectedState, viewer)
        }
    }

    /** The window [priority] holds priority in at [state], if it is one. */
    private fun windowAt(state: GameState, priority: EntityId): Window? {
        if (state.stack.isNotEmpty()) return null
        return when (state.step) {
            // Before blocks only a defender has something to do: grow or make the blockers it is
            // about to declare.
            Step.DECLARE_ATTACKERS -> if (state.isActiveTurnFor(priority)) null else Window.BEFORE_BLOCKS
            Step.DECLARE_BLOCKERS -> Window.AFTER_BLOCKS
            else -> null
        }
    }

    /** Concrete activations [responder] could answer with at [state], at most [MAX_OFFERS]. */
    private fun offers(state: GameState, responder: EntityId, window: Window): List<GameAction> {
        val defending = !state.isActiveTurnFor(responder)
        val fighters = fighters(state, responder, window)
        if (fighters.isEmpty()) return emptyList()
        val projected = state.projectedState
        val out = LinkedHashSet<GameAction>()
        for (offer in simulator.getPermanentAbilityActions(state, responder)) {
            if (!usable(offer)) continue
            if (!reaches(state, offer.action as ActivateAbility, defending, window)) continue
            val bound = XCostSelection.bindBestX(state, offer)
            val targeted = if (!bound.requiresTargets) listOf(bound.action) else {
                val infos = TargetSelection.targetInfosFor(bound) ?: continue
                val single = infos.singleOrNull()
                if (single != null && single.maxTargets == 1) {
                    // A single-target answer is tried on each creature in the fight it may hit —
                    // the damaged attacker, the blocker about to trade — not on whatever a
                    // board-wide ranking puts first.
                    single.validTargets.filter { it in fighters }
                        .sortedByDescending { CombatMath.creatureValue(state, projected, it) }
                        .take(MAX_TARGETS_PER_OFFER)
                        .map { target ->
                            TargetSelection.applyTargets(
                                bound.action, listOf(TargetSelection.toChosenTarget(state, single, target, responder)),
                            )
                        }
                } else {
                    listOf(TargetSelection.fillHeuristically(state, bound, responder, fillPartialRequirements = true))
                }
            }
            for (target in targeted) {
                // "Tap two untapped artifacts", "sacrifice a creature": the engine never picks a
                // selection cost for the activator, so an unpaid one would be refused.
                val action = SelectionCostPayment.withPlainCosts(state, offer, target)
                out += action
                // A repeatable pump is worth trying at every count the mana allows: one +1/+1 can
                // change nothing where two win the fight, so stopping at the first would miss it.
                val repeats = offer.maxRepeatableActivations ?: 1
                val activation = action as? ActivateAbility
                if (activation != null && repeats > 1) {
                    for (count in 2..minOf(repeats, MAX_REPEATS)) out += activation.copy(repeatCount = count)
                }
            }
            if (out.size >= MAX_OFFERS) break
        }
        return out.take(MAX_OFFERS)
    }

    /** The creatures a response at [window] could matter to. */
    private fun fighters(state: GameState, responder: EntityId, window: Window): Set<EntityId> {
        val projected = state.projectedState
        return state.getBattlefield().filter { id ->
            if (!projected.isCreature(id)) return@filter false
            val entity = state.getEntity(id) ?: return@filter false
            entity.has<AttackingComponent>() || entity.has<BlockingComponent>() ||
                (window == Window.BEFORE_BLOCKS && projected.getController(id) == responder && !entity.has<TappedComponent>())
        }.toSet()
    }

    private fun usable(offer: LegalAction): Boolean =
        offer.affordable && !offer.isManaAbility && !offer.hasUnfillableTargetRequirement && offer.action is ActivateAbility

    /**
     * Whether [activation] can touch a fight at [window] (null: any window). An ability the
     * catalog cannot read — granted by a source the definition lookup does not reach — counts:
     * the simulation is the judge of whether it matters.
     */
    private fun reaches(state: GameState, activation: ActivateAbility, defending: Boolean, window: Window?): Boolean {
        val reach = reachOf(state, activation, cardRegistry) ?: return true
        if (CombatReach.FIGHT in reach) return true
        return CombatReach.BLOCKER in reach && defending && window != Window.AFTER_BLOCKS
    }

    /**
     * [end] with what the responses left behind that will be gone before the AI next faces this
     * board: their until-end-of-turn effects (and the marked damage those effects were holding
     * up — damage and "until end of turn" end together, in the cleanup step) and, on the AI's own
     * turn, the lands the responders tapped for them, which untap before that mana matters again.
     * Without this the crack-back estimate would read the opponent's team pump as standing guard.
     */
    private fun settleResponses(start: GameState, end: GameState, responders: Set<EntityId>): GameState {
        val before = start.floatingEffects.mapTo(HashSet()) { it.id }
        val created = end.floatingEffects.filter { it.id !in before && it.duration == Duration.EndOfTurn }
        var settled = end.copy(floatingEffects = end.floatingEffects - created.toSet())
        val touched = created.flatMapTo(HashSet()) { it.effect.affectedEntities }
        for (id in touched) {
            if (settled.getEntity(id)?.has<DamageComponent>() == true) {
                settled = settled.updateEntity(id) { it.without<DamageComponent>() }
            }
        }
        for (responder in responders) {
            if (start.isActiveTurnFor(responder)) continue
            settled = untapLands(settled, responder) { id -> start.getEntity(id)?.has<TappedComponent>() != true }
        }
        return settled
    }

    /** [state] with [player]'s tapped lands that pass [include] untapped. */
    private fun untapLands(state: GameState, player: EntityId, include: (EntityId) -> Boolean): GameState {
        val projected = state.projectedState
        var out = state
        for (id in projected.getBattlefieldControlledBy(player)) {
            if (!projected.hasType(id, "LAND") || !include(id)) continue
            if (out.getEntity(id)?.has<TappedComponent>() == true) out = out.updateEntity(id) { it.without<TappedComponent>() }
        }
        return out
    }

    companion object {
        /**
         * What [activation]'s ability can do to a fight, read off its definition (or the grant that
         * gave it); null when the definition lookup does not reach it.
         */
        fun reachOf(state: GameState, activation: ActivateAbility, cardRegistry: CardRegistry): Set<CombatReach>? {
            val card = state.getEntity(activation.sourceId)?.get<CardComponent>()
            val definition = card?.let { cardRegistry.getCard(it.cardDefinitionId) }
            val ability = definition?.script?.activatedAbilities?.find { it.id == activation.abilityId }
                ?: definition?.cardFaces?.firstNotNullOfOrNull { face ->
                    face.script.activatedAbilities.find { it.id == activation.abilityId }
                }
                ?: card?.copyActivatedAbilities?.find { it.id == activation.abilityId }
                ?: state.grantedActivatedAbilities
                    .firstOrNull { it.entityId == activation.sourceId && it.ability.id == activation.abilityId }?.ability
                ?: return null
            return CombatReach.of(ability)
        }

        /**
         * How much better than passing a response must leave the responder, in evaluator units,
         * before it is assumed. Below a fraction of the cheapest creature: a response that only
         * shuffles rounding is not one an opponent would spend mana on.
         */
        const val MIN_GAIN = 0.25

        /** Extra combat simulations one [playOut] may spend on responses, baselines included. */
        const val MAX_RESPONSE_SIMULATIONS = 16

        /** Concrete activations tried at one window. */
        const val MAX_OFFERS = 8

        /** Creatures a single-target answer is tried on. */
        const val MAX_TARGETS_PER_OFFER = 3

        /** The most activations of one repeatable ability tried as one answer. */
        const val MAX_REPEATS = 4

        /** Priority passes one combat may take — far above a real combat's. */
        private const val MAX_ITERATIONS = 60
    }
}
