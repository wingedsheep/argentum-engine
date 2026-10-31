package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.BlockTriggerRemoval
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.advisor.CardAdvisorRegistry
import com.wingedsheep.ai.engine.budget.DecisionBudget
import com.wingedsheep.ai.engine.evaluation.BoardEvaluator
import com.wingedsheep.ai.engine.evaluation.LifeDifferential
import com.wingedsheep.ai.engine.evaluation.ThreatAssessment
import com.wingedsheep.ai.insight.CombatPlanTrace
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.combat.rules.BlockCheckContext
import com.wingedsheep.engine.mechanics.combat.rules.BlockEvasionRule
import com.wingedsheep.engine.mechanics.combat.rules.defaultBlockEvasionRules
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.core.doesntUntapDuringUntapStep
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.model.EntityId

/**
 * Specialized advisor for attack and block decisions.
 *
 * Per-creature attack decisions use simulation: declare this creature attacking,
 * predict the opponent's block using [chooseBlockers], resolve combat damage,
 * and compare the resulting board evaluation to not attacking.
 */
class CombatAdvisor(
    private val simulator: GameSimulator,
    private val evaluator: BoardEvaluator,
    private val cardRegistry: CardRegistry? = null,
    private val advisorRegistry: CardAdvisorRegistry = CardAdvisorRegistry(),
    /**
     * Price the estimated crack-back as life actually lost, instead of a flat penalty that only
     * fires when it is exactly lethal.
     *
     * The old rule is a cliff: at 5 life, four incoming damage costs **nothing** and five costs
     * 3.0. Everything between "free" and "dead" reads as free, so an attack plan that empties the
     * board of blockers is scored purely on the damage it deals — which is `race-03`, where the
     * AI sends the ground creature that was the only answer to the crack-back.
     *
     * When on, the penalty is `lifeValue(now) - lifeValue(now - incoming)` scaled by the
     * evaluator's own life weight. That is not a new guess: it is the same curve
     * [com.wingedsheep.ai.engine.evaluation.LifeDifferential] already uses, so anticipated damage
     * is charged at exactly the rate real damage is. It is continuous by construction, and still
     * dominant at lethal because `lifeValue` prices death at −100.
     */
    private val priceCrackBackAsLife: Boolean = false,
    /**
     * Under [priceCrackBackAsLife], charge the crack-back a defender would actually take rather
     * than the one it could survive by chump-blocking with everything.
     *
     * The estimate behind the crack-back lets every untapped creature soak a hit, so a 2/2 kept home
     * "prevents" a 3/3's three damage for free — and priced as life, that makes holding a creature
     * back to chump look better than attacking with it. That is `race-06` (the 3/3 is tapped, the
     * 2/2 should get in): life-priced alone, the AI held the Bears. Chump blocks are still how the
     * AI survives, so they stay in the *lethal* test; the life charged is the damage taken without
     * them, capped one short of dead, since below that the AI would chump rather than die.
     */
    private val crackBackWithoutChumps: Boolean = false,
    /** The composite evaluator's `life` coefficient, so the two are in the same units. */
    private val lifeWeight: Double = 1.0,
    /**
     * Keep blocking local search from buying life with a creature while the life is not needed.
     * [AiProfile.chumpOnlyWhenInDanger] carries the argument; this is where it is enforced.
     */
    private val chumpOnlyWhenInDanger: Boolean = false,
    /**
     * Choose each attacker's defender, not only whether it attacks: opposing planeswalkers are
     * targets. [AiProfile.attackPlaneswalkers] carries the argument; the seed aims connecting
     * attackers at walkers ([CombatSeed.aimAtPlaneswalkers]) and the local search retargets.
     */
    internal val attackPlaneswalkers: Boolean = false,
    /**
     * Value life by the opponent's clock rather than by a lethal-this-turn cliff: after the blocks
     * are chosen, add a chump block whenever the creature it costs is less than the bodies it
     * saves on the opponent's *next* attack ([clockChumps]). [AiProfile.chumpForTheClock] carries
     * the argument.
     */
    private val chumpForTheClock: Boolean = false,
    /**
     * Defend our own planeswalkers: damage aimed at one is the walker's, not ours, and a block on an
     * attacker aimed at one is worth the walker value it saves ([protectPlaneswalkers]).
     * [AiProfile.defendPlaneswalkers] carries the argument.
     */
    private val defendPlaneswalkers: Boolean = false,
    /**
     * Price walkers as engines in the attack seed's aim and the walker blocks, reading their
     * abilities through this catalog ([PlaneswalkerThreat]). [AiProfile.walkersPricedAsEngines]
     * carries the argument; null is the legacy reading.
     */
    internal val walkerEngines: IntentCatalog? = null,
    /**
     * Let the survival pass stack several chump blockers on a trampler, and count double strike in
     * the damage that gets through ([stackOnTramplers], [calculateIncomingDamage]).
     * [AiProfile.stackChumpsOnTramplers] carries the argument.
     */
    private val stackChumpsOnTramplers: Boolean = false,
    /**
     * Gang up on an attacker two blockers kill without either dying, before trading for it
     * ([freeGangBlock]). [AiProfile.freeGangBlocks] carries the argument.
     */
    private val freeGangBlocks: Boolean = false,
    /**
     * Score attack plans by the race: credit our follow-up swing ([followUpValue]), read the board
     * as it stands at our next untap so the crack-back is a tapped attacker's only price, and take
     * the worse of the predicted blocks and no blocks when the blocks cost the defender a creature
     * ([evaluateAttackPlan]). [AiProfile.attacksReadTheRace] carries the argument.
     */
    private val attacksReadTheRace: Boolean = false,
    /**
     * Score every attack plan with each until-end-of-turn control change already ended: a
     * creature borrowed for the turn is back with its owner's side by the crack-back, so it is no
     * blocker of ours, and losing it in combat costs them, not us ([scoreAttackOutcome]).
     * [AiProfile.borrowedCreaturesGoHome] carries the argument.
     */
    private val borrowedCreaturesGoHome: Boolean = false,
    /** The composite evaluator's `threatAssessment` coefficient — [followUpValue]'s weight. */
    private val raceWeight: Double = 1.2,
    /**
     * Press a winning position: credit an attack plan for the damage it deals, in proportion to how
     * far the board it leaves dominates ([pressureValue], [BoardRole]), and try the all-in plan as
     * a local-search start ([alphaStrikeStart]). [AiProfile.pressWhenAhead] carries the argument.
     */
    private val pressWhenAhead: Boolean = false,
    /**
     * Let every simulated combat give the opponents their windows: the activated abilities their
     * permanents can afford — a team pump, a pinger, a first-strike granter — used when they win
     * the fight for them ([CombatResponses]). [AiProfile.anticipateCombatResponses] carries the
     * argument; needs [cardRegistry].
     */
    anticipateCombatResponses: Boolean = false,
    /**
     * In a pod, shrink a lethal alpha strike to the attackers the kill needs, keeping the rest home
     * for the opponents still in the game ([CombatSeed.minimalLethal]).
     * [AiProfile.minimalLethalInPods] carries the argument.
     */
    internal val minimalLethalInPods: Boolean = false,
    /**
     * In a pod, aim the attack at a seat it kills, else at the opponent with the biggest board,
     * instead of the lowest life total ([CombatSeed.primaryDefender]).
     * [AiProfile.threatAwareDefender] carries the argument.
     */
    internal val threatAwareDefender: Boolean = false,
    /**
     * Let the block heuristic see a blocker whose block trigger destroys the attacker it blocks
     * (Alaborn Zealot), as it sees deathtouch ([BlockTriggerRemoval]). The attack evaluator predicts
     * the defender's blocks with that heuristic, so this is also what prices such a blocker into an
     * attack. [AiProfile.blockTriggersKill] carries the argument; needs [cardRegistry].
     */
    private val blockTriggersKill: Boolean = false,
    /**
     * Read an attack plan's crack-back as it can really come: idle blockers stack on tramplers, and
     * each affordable activated ability of the opponent's that can change a fight is played out
     * once, a plan whose swing back one of them makes lethal priced as lethal ([CrackBackStress]).
     * [AiProfile.stressCrackBack] carries the argument; needs [cardRegistry].
     */
    stressCrackBack: Boolean = false,
) {
    /** See [anticipateCombatResponses]; null when off. */
    private val responses: CombatResponses? =
        if (anticipateCombatResponses && cardRegistry != null) CombatResponses(simulator, evaluator, cardRegistry) else null

    /** See [stressCrackBack]; null when off. */
    private val crackBackStress: CrackBackStress? =
        if (stressCrackBack && cardRegistry != null) CrackBackStress(simulator, cardRegistry) else null

    /**
     * The engine's own block-legality rules, asked alongside [CombatMath.canBeBlockedBy]'s
     * hand-mirrored subset. That subset has no protection check at all, so a black creature was
     * planned in front of a pro-black attacker; the engine refused the whole declaration, and
     * [legalizeBlockerPlan] peeled blockers off cheapest-first until nothing — the free block
     * beside it included — was left. Null without a registry: tests that build a bare advisor keep
     * the hand-mirrored check alone.
     */
    private val blockEvasionRules: List<BlockEvasionRule>? by lazy {
        cardRegistry?.let { defaultBlockEvasionRules(PredicateEvaluator(it)) }
    }

    /** Matches a block trigger's attacker filter ("blocks a creature with flying"); see [blockTriggersKill]. */
    private val blockTriggerPredicates: PredicateEvaluator? by lazy {
        if (blockTriggersKill) cardRegistry?.let { PredicateEvaluator(it) } else null
    }

    companion object {
        /**
         * Max engine simulations for blocking local search — now a **floor**, not a ceiling.
         *
         * `DecisionBudget` derives the real allowance from the tier, and combat declaration is
         * always CRITICAL, so combat never gets less search than it did before Phase 4. This
         * constant is what `SearchAllowances.LEGACY` and every sub-NORMAL tier resolve to.
         */
        const val MAX_BLOCK_SIMULATIONS = 10

        /**
         * How much cheaper a clock-pass chump must make the plan before it is added — creature
         * value units, a fraction of the cheapest real body, so ties and rounding noise keep the
         * creature.
         */
        private const val CLOCK_MARGIN = 0.5

        /** The least a body counts for when it is thrown away: even a 0-value token blocks again. */
        private const val MIN_BODY_VALUE = 0.25

        /**
         * Priority passes [plannedAttackersFrom] may spend walking a quiet precombat state to
         * declare attackers — one per seat per step, with room for a straggling trigger.
         */
        const val MAX_PASSES_TO_DECLARE_ATTACKERS = 8
    }

    /**
     * Sum of the [CardAdvisor.attackPenalty]s declared for the creatures in an attack plan.
     *
     * Advisors use this to discourage attacking with a specific creature (a wall you
     * want back on defence, a creature whose value is its static ability). Returns 0.0
     * when no attacker has an advisor, which is the case for every card today.
     */
    private fun attackPenaltyFor(
        state: GameState,
        projected: ProjectedState,
        attackers: Collection<EntityId>,
        playerId: EntityId
    ): Double {
        if (attackers.isEmpty()) return 0.0
        return attackers.sumOf { entityId ->
            val name = state.getEntity(entityId)?.get<CardComponent>()?.name ?: return@sumOf 0.0
            val advisor = advisorRegistry.getAdvisor(name) ?: return@sumOf 0.0
            advisor.attackPenalty(state, projected, entityId, playerId) ?: 0.0
        }
    }

    /**
     * The creatures this advisor would attack with if [state] — our own precombat main or
     * beginning of combat, stack empty — were simply passed through to declare attackers: the plan
     * the AI would submit a moment later, local search included.
     *
     * Read by the "a pump needs a planned attack" checks, which ask whether an end-of-turn grant
     * lands on a creature the attack will actually use. Null whenever the plan cannot be read
     * cheaply and exactly — not our turn, a step past beginning of combat, something on the stack
     * or a decision pending on the way, or more passes than a quiet turn needs — and every caller
     * reads null as "can attack", the permissive answer.
     */
    internal fun plannedAttackersFrom(
        state: GameState,
        playerId: EntityId,
        budgetFor: (GameState) -> DecisionBudget,
    ): Set<EntityId>? {
        val current = atOurDeclareAttackers(state, playerId) ?: return null
        val declare = simulator.getLegalActions(current, playerId)
            .firstOrNull { it.actionType == "DeclareAttackers" } ?: return null
        val plan = chooseAttackers(current, declare, playerId, budgetFor(current))
        return (plan as? DeclareAttackers)?.attackers?.keys
    }

    /** An attack plan read off a precombat position, and what it is worth — see [plannedAttackFrom]. */
    internal data class PlannedAttack(val attackers: Set<EntityId>, val value: Double)

    /**
     * [plannedAttackersFrom], plus the plan's value: the board after it is simulated through
     * combat (the defender blocking by heuristic) with the crack-back priced in — the same number
     * the attack search ranks plans by. Comparable across two precombat positions of the same turn,
     * which is what lets a pump be priced by the attack it buys. Null where [plannedAttackersFrom]
     * is, or when the plan cannot be simulated.
     */
    internal fun plannedAttackFrom(
        state: GameState,
        playerId: EntityId,
        budgetFor: (GameState) -> DecisionBudget,
    ): PlannedAttack? {
        val current = atOurDeclareAttackers(state, playerId) ?: return null
        val declare = simulator.getLegalActions(current, playerId)
            .firstOrNull { it.actionType == "DeclareAttackers" } ?: return null
        val plan = chooseAttackers(current, declare, playerId, budgetFor(current)) as? DeclareAttackers ?: return null
        val opponent = current.turnOrder.firstOrNull { it != playerId } ?: return null
        val value = evaluateAttackPlan(current, playerId, opponent, plan.attackers, settleExpiring = true) ?: return null
        return PlannedAttack(plan.attackers.keys, value)
    }

    /** [state] passed through to our own declare-attackers step, or null — see [plannedAttackersFrom]. */
    private fun atOurDeclareAttackers(state: GameState, playerId: EntityId): GameState? {
        var current = state
        var passes = 0
        while (current.step != Step.DECLARE_ATTACKERS) {
            if (current.gameOver || current.pendingDecision != null || current.stack.isNotEmpty()) return null
            if (!current.isActiveTurnFor(playerId)) return null
            if (current.step != Step.PRECOMBAT_MAIN && current.step != Step.BEGIN_COMBAT) return null
            if (passes++ >= MAX_PASSES_TO_DECLARE_ATTACKERS) return null
            val priority = current.priorityPlayerId ?: return null
            current = (simulator.simulate(current, PassPriority(priority)) as? SimulationResult.Terminal)?.state
                ?: return null
        }
        if (!current.isActiveTurnFor(playerId)) return null
        return current
    }

    /**
     * Build a DeclareAttackers action choosing which creatures to send in.
     *
     * Two-phase approach:
     * 1. Heuristic seed: always-attack creatures (evasive, vigilance, indestructible),
     *    plus lethal alpha-strike detection. Lives in [CombatSeed] since Phase 7, because a
     *    rollout playout needs the seed without the simulation-driven second phase.
     * 2. Local search: try adding/removing one attacker at a time, simulate each through
     *    the engine (opponent blocks via heuristic, combat resolves), keep improvements
     */
    fun chooseAttackers(
        state: GameState,
        legalAction: LegalAction,
        playerId: EntityId,
        budget: DecisionBudget = DecisionBudget.legacy(),
        /** Local testing mode: collects the plans local search simulated. Null in production. */
        trace: CombatPlanTrace? = null,
    ): GameAction {
        val projected = state.projectedState
        val validAttackers = legalAction.validAttackers ?: emptyList()
        val mandatory = legalAction.mandatoryAttackers ?: emptyList()

        val seed = CombatSeed.attackers(
            state, projected, legalAction, playerId, cardRegistry,
            attackPenalty = { entityId ->
                attackPenaltyFor(state, projected, listOf(entityId), playerId)
            },
            aimAtPlaneswalkers = attackPlaneswalkers,
            walkerEngines = walkerEngines,
            minimalLethalInPods = minimalLethalInPods,
            threatAwareDefender = threatAwareDefender,
        )
        val opponentId = seed.defenderId ?: return DeclareAttackers(playerId, emptyMap())
        // Which defender each attacker may legally take (CR 508.1b–c) — the engine lists it per
        // attacker, and every plan below is built from and checked against that list.
        val canAttack = { attacker: EntityId, defender: EntityId -> defender in legalAction.attackTargetsFor(attacker) }
        // The seed's lethal check is printed-board math. An opponent holding a combat response
        // (an untapped pump, a blocker-maker) can answer it, so under [anticipateCombatResponses]
        // that alpha strike goes through local search like any plan — the seed is its start, and
        // a kill the responses can't stop still scores as one.
        val lethalThroughResponses = responses?.respondersAt(state, playerId).isNullOrEmpty()
        if (seed.lethal && lethalThroughResponses) {
            return DeclareAttackers(
                playerId,
                legalizeAttackerPlan(
                    state, playerId,
                    CombatSeed.legalize(
                        state, legalAction,
                        AttackRequirementRepair.repair(
                            state, cardRegistry, playerId, seed.attackers, validAttackers, mandatory.toSet(), canAttack,
                        ),
                        opponentId,
                        threatAwareDefender,
                    ),
                    mandatory.toSet(),
                ),
            )
        }

        // An attacker cap on the defenders (a tapped Mirri, Dueling Grounds) can make the seed
        // itself illegal, and local search gives up on an illegal starting plan — trim it first.
        val seedMap = legalizeAttackerPlan(state, playerId, seed.attackers, mandatory.toSet()).toMutableMap()

        // ── Local search: try add/remove mutations via simulation ──
        // Only run if we're at DECLARE_ATTACKERS (simulation needs to submit DeclareAttackers).
        // Skip entirely when no opponent controls a creature: nothing can block, so combat is
        // deterministic, and with zero enemy creatures there's no crack-back to weigh — the
        // heuristic seed (attack with everything) is already optimal. Running a full-combat
        // simulation here only burns time without ever changing the plan.
        val enemyControlsCreature = state.turnOrder.any { other ->
            other != playerId && projected.getBattlefieldControlledBy(other).any { projected.isCreature(it) }
        }
        // Who each attacker may hit: its default seat, plus — under [attackPlaneswalkers] — every
        // opposing planeswalker it may legally attack. With a walker in reach the choice of
        // defender is a real one even when nothing can block, so the search runs then too.
        val defenderOf = { attacker: EntityId -> CombatSeed.defenderFor(state, legalAction, attacker, opponentId, threatAwareDefender) }
        val defendersOf = { attacker: EntityId ->
            val primary = listOfNotNull(defenderOf(attacker))
            if (!attackPlaneswalkers) primary
            else (primary + CombatSeed.planeswalkerTargetsFor(state, projected, legalAction, attacker)).distinct()
        }
        val walkerInReach = attackPlaneswalkers &&
            validAttackers.any { CombatSeed.planeswalkerTargetsFor(state, projected, legalAction, it).isNotEmpty() }
        if (state.step == Step.DECLARE_ATTACKERS && (enemyControlsCreature || walkerInReach)) {
            improveAttackViaLocalSearch(
                state, playerId, opponentId, validAttackers, mandatory.toSet(), seedMap, budget, trace,
                defenderFor = defenderOf,
                defendersFor = defendersOf,
            )
        }

        // Local search may have added attackers back on top of an already-trimmed seed, so the tax
        // is re-priced against the plan we are about to submit. See [CombatTaxBudget].
        // The tax trim can drop the creature an attack requirement needs, so repair last.
        // Legalize last: no attacker may go at a defender the engine doesn't list for it, or the
        // whole declaration is rejected and the seat attacks with nothing.
        return DeclareAttackers(
            playerId,
            legalizeAttackerPlan(
                state, playerId,
                CombatSeed.legalize(
                    state, legalAction,
                    AttackRequirementRepair.repair(
                        state, cardRegistry, playerId,
                        CombatTaxBudget.affordableAttack(
                            state, projected, playerId, cardRegistry, seedMap, mandatory.toSet()
                        ),
                        validAttackers, mandatory.toSet(), canAttack,
                    ),
                    opponentId,
                    threatAwareDefender,
                ),
                mandatory.toSet(),
            ),
        )
    }

    /**
     * Whole-declaration restrictions the per-attacker target lists can't show — "no more than one
     * creature can attack you each combat" (a tapped Mirri, Weatherlight Duelist), Dueling
     * Grounds — reject the entire declaration, and a rejected declaration means the seat attacks
     * with nothing. Validate the plan through the authoritative engine and drop the least
     * valuable optional attacker until it is legal; mandatory attackers stay. The attacker-side
     * twin of [legalizeBlockerPlan]. Only at the declare-attackers step, where the engine can
     * judge a declaration.
     */
    private fun legalizeAttackerPlan(
        state: GameState,
        playerId: EntityId,
        proposed: Map<EntityId, EntityId>,
        mandatoryAttackerIds: Set<EntityId>,
    ): Map<EntityId, EntityId> {
        if (state.step != Step.DECLARE_ATTACKERS || proposed.isEmpty()) return proposed
        val candidate = proposed.toMutableMap()
        while (simulator.simulate(state, DeclareAttackers(playerId, candidate)) is SimulationResult.Illegal) {
            val removable = candidate.keys
                .filterNot { it in mandatoryAttackerIds }
                .minByOrNull { CombatMath.creatureValue(state, state.projectedState, it) }
                ?: return candidate
            candidate.remove(removable)
        }
        return candidate
    }

    /**
     * Build a DeclareBlockers action choosing which creatures block which attackers.
     *
     * Uses a two-phase approach:
     * 1. Generate a seed plan using fast combat math heuristics
     * 2. Improve via local search: try small mutations (swap/add/remove one blocker),
     *    simulate each through the engine, keep improvements until no mutation helps
     *
     * The engine simulation correctly handles all keyword interactions (first strike,
     * deathtouch, trample, lifelink, indestructible, double strike, etc.) without
     * heuristic blind spots. Local search keeps the simulation count low (~10-20).
     *
     * When called from within attack simulation (nested), skips local search
     * to avoid exponential cost and uses the heuristic seed plan directly.
     */
    fun chooseBlockers(
        state: GameState,
        legalAction: LegalAction,
        playerId: EntityId,
        useSimulation: Boolean = false,
        budget: DecisionBudget = DecisionBudget.legacy(),
        /** Local testing mode: collects the plans local search simulated. Null in production. */
        trace: CombatPlanTrace? = null,
    ): GameAction {
        val projected = state.projectedState
        val validBlockers = legalAction.validBlockers ?: emptyList()
        val mandatory = legalAction.mandatoryBlockerAssignments ?: emptyMap()

        if (validBlockers.isEmpty()) {
            return DeclareBlockers(playerId, emptyMap())
        }

        val attackers = getAttackingCreatures(state)
        if (attackers.isEmpty()) {
            return DeclareBlockers(playerId, emptyMap())
        }

        val myLife = state.lifeTotal(playerId)

        // Build mandatory blocker base (preserved across all plans)
        val mandatoryMap = mutableMapOf<EntityId, List<EntityId>>()
        val mandatoryBlockerIds = mutableSetOf<EntityId>()
        for ((blockerId, mustBlockAttackers) in mandatory) {
            if (mustBlockAttackers.isNotEmpty()) {
                mandatoryMap[blockerId] = mustBlockAttackers.take(legalAction.blockerMaxBlockCounts?.get(blockerId) ?: 1)
                mandatoryBlockerIds.add(blockerId)
            }
        }

        val sortedAttackers = attackers.sortedByDescending { CombatMath.effectiveDamage(projected, it) }

        // ── Phase 1: Generate seed plan via heuristic ──
        val seedMap = mandatoryMap.toMutableMap()
        val seedAssigned = mandatoryBlockerIds.toMutableSet()
        chooseBlockersViaHeuristic(
            state, projected, sortedAttackers, validBlockers,
            seedAssigned, seedMap, myLife
        )
        if (defendPlaneswalkers) {
            protectPlaneswalkers(state, projected, playerId, attackers, validBlockers, seedAssigned, seedMap)
        }

        // ── Phase 2: Improve via local search (if not nested) ──
        var bestMap = if (useSimulation) {
            improveViaLocalSearch(
                state, projected, playerId, attackers, validBlockers,
                mandatoryBlockerIds, seedMap, budget, myLife, trace
            )
        } else {
            seedMap
        }

        // ── Chump-block pass: if facing immediate lethal, assign remaining blockers ──
        // Only chump-block when unblocked damage would kill us THIS turn.
        // Don't chump-block for next-turn lethal — losing a blocker now makes next turn worse.
        val assignedBlockers = bestMap.keys.toMutableSet()
        val lethalAsPlanned = isLethalThisCombat(state, projected, attackers, bestMap, myLife, playerId)
        if (lethalAsPlanned) {
            val unblockedAttackers = attackers
                .filter { attacker -> bestMap.values.none { attacker in it } }

            val sortedUnblocked = unblockedAttackers.sortedByDescending { chumpPriority(projected, it) }

            for (attacker in sortedUnblocked) {
                val needed = if (needsTwoBlockers(projected, attacker)) 2 else 1
                val available = availableBlockersFor(state, projected, attacker, validBlockers, assignedBlockers)
                    .sortedBy { CombatMath.creatureValue(state, projected, it) }
                if (available.size < needed) continue
                available.take(needed).forEach { blocker ->
                    bestMap[blocker] = listOf(attacker)
                    assignedBlockers.add(blocker)
                }
            }

            // ── Survival pass: a plan that still lets lethal through is not a plan ──
            // Everything above prices a block by what it *wins* — a favourable trade, a gang block
            // that eats the best attacker — which is the right currency only if we get another turn
            // to spend the board on. When the leftovers are lethal we do not, and two blockers
            // standing in front of one creature while a bigger one walks in unblocked is a loss
            // however good the trade reads. So rebuild the assignment for damage *prevented*, and
            // adopt it only when it genuinely saves us: positions the AI already survives keep the
            // plan they had.
            if (isLethalThisCombat(state, projected, attackers, bestMap, myLife, playerId)) {
                val survival = damageMinimisingPlan(state, projected, attackers, validBlockers, mandatoryMap)
                if (stackChumpsOnTramplers) {
                    stackOnTramplers(state, projected, attackers, validBlockers, survival, myLife)
                }
                if (!isLethalThisCombat(state, projected, attackers, survival, myLife, playerId)) {
                    bestMap = survival
                    assignedBlockers.clear()
                    assignedBlockers.addAll(survival.keys)
                }
            }
        }

        // ── Clock pass: buy the life the next attack would need, when it costs less than it saves ──
        if (chumpForTheClock) {
            if (!isLethalThisCombat(state, projected, attackers, bestMap, myLife, playerId)) {
                bestMap = clockChumps(state, projected, playerId, attackers, validBlockers, bestMap, myLife)
                assignedBlockers.clear()
                assignedBlockers.addAll(bestMap.keys)
            }
        }
        // ── Proactive chump-block: if facing lethal within 2 turns, sacrifice low-value creatures ──
        // Only do this when NOT already facing immediate lethal (that's handled above).
        else if (!lethalAsPlanned) {
            val inDanger = isLifeInDanger(state, projected, attackers, bestMap, myLife, playerId, chumpOnlyWhenInDanger)
            if (inDanger) {
                val unblockedAttackers = attackers
                    .filter { attacker -> bestMap.values.none { attacker in it } }
                    .sortedByDescending { chumpPriority(projected, it) }

                for (attacker in unblockedAttackers) {
                    // One cheap body cannot stop a menace attacker; the pair it would take is not
                    // a cheap chump any more.
                    if (needsTwoBlockers(projected, attacker)) continue
                    val available = availableBlockersFor(state, projected, attacker, validBlockers, assignedBlockers)
                        .filter { CombatMath.creatureValue(state, projected, it) < 2.0 }
                        .sortedBy { CombatMath.creatureValue(state, projected, it) }
                    val cheapest = available.firstOrNull() ?: continue
                    bestMap[cheapest] = listOf(attacker)
                    assignedBlockers.add(cheapest)
                }
            }
        }

        // ── Menace fix: remove illegal single-blocker assignments for menace attackers ──
        fixMenaceAssignments(state, projected, bestMap, validBlockers, assignedBlockers)

        // ── Block tax: a plan we cannot pay for is a plan we would only decline (see
        // [CombatTaxBudget]) ──
        val affordable = CombatTaxBudget.affordableBlock(
            state, projected, playerId, cardRegistry,
            legalizeBlockerPlan(state, playerId, bestMap, mandatoryBlockerIds),
            mandatoryBlockerIds,
        )

        return DeclareBlockers(playerId, affordable)
    }

    /**
     * Pairwise combat checks cannot see assignment-wide restrictions such as "can't be blocked by
     * more than one creature". Validate the completed plan through the authoritative engine and
     * peel off optional assignments until it is legal. Mandatory assignments are preserved.
     */
    private fun legalizeBlockerPlan(
        state: GameState,
        playerId: EntityId,
        proposed: Map<EntityId, List<EntityId>>,
        mandatoryBlockerIds: Set<EntityId>,
    ): Map<EntityId, List<EntityId>> {
        val candidate = proposed.toMutableMap()
        while (simulator.simulate(state, DeclareBlockers(playerId, candidate)) is SimulationResult.Illegal) {
            val removable = candidate.keys
                .filterNot { it in mandatoryBlockerIds }
                .minByOrNull { CombatMath.creatureValue(state, state.projectedState, it) }
                ?: return candidate.filterKeys { it in mandatoryBlockerIds }
            candidate.remove(removable)
        }
        return candidate
    }

    /**
     * Fix illegal blocker assignments for menace attackers.
     * Menace requires 2+ blockers. If only 1 blocker is assigned, try to add a second;
     * if none available, remove the assignment entirely.
     */
    private fun fixMenaceAssignments(
        state: GameState,
        projected: ProjectedState,
        blockerMap: MutableMap<EntityId, List<EntityId>>,
        validBlockers: List<EntityId>,
        assignedBlockers: MutableSet<EntityId>
    ) {
        // Build attacker → blockers map
        val attackerBlockers = mutableMapOf<EntityId, MutableList<EntityId>>()
        for ((blockerId, attackerList) in blockerMap) {
            for (attackerId in attackerList) {
                attackerBlockers.getOrPut(attackerId) { mutableListOf() }.add(blockerId)
            }
        }

        for ((attackerId, blockers) in attackerBlockers) {
            val keywords = projected.getKeywords(attackerId)
            if (Keyword.MENACE.name !in keywords) continue
            if (blockers.size >= 2) continue

            // Single blocker on menace — try to find a second
            val available = availableBlockersFor(state, projected, attackerId, validBlockers, assignedBlockers)
            if (available.isNotEmpty()) {
                val second = available.minByOrNull { CombatMath.creatureValue(state, projected, it) }!!
                blockerMap[second] = listOf(attackerId)
                assignedBlockers.add(second)
            } else {
                // Can't find a second blocker — remove the single illegal assignment
                for (b in blockers) {
                    blockerMap.remove(b)
                    assignedBlockers.remove(b)
                }
            }
        }
    }

    /**
     * Local search: starting from the heuristic seed plan, try targeted mutations
     * and simulate each through the engine. Keep improvements until convergence.
     *
     * Instead of exhaustively trying all possible mutations (O(B×A) simulations),
     * generates only the most promising candidates using combat math as a filter.
     * Each candidate is then validated via full engine simulation to catch triggers,
     * replacement effects, and keyword interactions that math alone would miss.
     *
     * Caps at `budget.allowances.blockSimulations` total simulations to keep decision time
     * bounded — [MAX_BLOCK_SIMULATIONS] is that allowance's floor, so blocking never searches
     * less than it did before the budget existed.
     */
    private fun improveViaLocalSearch(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        attackers: List<EntityId>,
        validBlockers: List<EntityId>,
        mandatoryBlockerIds: Set<EntityId>,
        seedMap: MutableMap<EntityId, List<EntityId>>,
        budget: DecisionBudget,
        myLife: Int,
        trace: CombatPlanTrace? = null,
    ): MutableMap<EntityId, List<EntityId>> {
        var currentPlan = seedMap.toMutableMap()
        // An illegal seed simulates to nothing, and returning it unsearched hands the whole
        // decision to `legalizeBlockerPlan`'s cheapest-first peeling — which can strip a perfectly
        // good block that sat beside the illegal one. Search from the legal part of it instead.
        val seedOutcome = evaluateBlockingPlan(state, playerId, currentPlan)
            ?: run {
                currentPlan = legalizeBlockerPlan(state, playerId, currentPlan, mandatoryBlockerIds).toMutableMap()
                evaluateBlockingPlan(state, playerId, currentPlan)
            }
            ?: return currentPlan
        var currentScore = seedOutcome.score
        var currentLosers = seedOutcome.losingBlockers
        trace?.recordBlock(currentPlan, currentScore)

        // Blockers the seed already loses. Under [chumpOnlyWhenInDanger] a mutation may keep these
        // (the heuristic put them there for a reason of its own) but may not add to them unless
        // the life it buys is life we need.
        val seedLosers = seedOutcome.losingBlockers
        val chumpsAllowed = !chumpOnlyWhenInDanger ||
            isLifeInDanger(state, projected, attackers, currentPlan, myLife, playerId, countCreaturesThatUntap = true) ||
            // The printed numbers say we live; the simulation, with the attacker's affordable
            // pumps used, says we don't. That life is needed.
            (responses != null && seedOutcome.lethal)
        var simulationsLeft = budget.allowances.blockSimulations
        val deadline = budget.combatDeadlineNanos

        val maxIterations = 2
        for (iteration in 1..maxIterations) {
            if (simulationsLeft <= 0 || System.nanoTime() > deadline) break

            val mutations = generateBlockMutations(
                state, projected, attackers, validBlockers,
                mandatoryBlockerIds, currentPlan, currentLosers
            )

            var bestMutation: MutableMap<EntityId, List<EntityId>>? = null
            var bestScore = currentScore
            var bestLosers = currentLosers

            for (mutation in mutations) {
                if (simulationsLeft <= 0 || System.nanoTime() > deadline) break
                simulationsLeft--
                val outcome = evaluateBlockingPlan(state, playerId, mutation) ?: continue
                // A blocker lost protecting one of our planeswalkers buys the walker, not life;
                // the simulated score prices that trade, so the life gate does not apply to it.
                val newLosers = outcome.losingBlockers - seedLosers
                if (!chumpsAllowed && newLosers.any { !(defendPlaneswalkers && blocksForAWalker(state, mutation[it])) }) continue
                val score = outcome.score
                trace?.recordBlock(mutation, score)
                if (score > bestScore) {
                    bestScore = score
                    bestMutation = mutation
                    bestLosers = outcome.losingBlockers
                }
            }

            if (bestMutation != null) {
                currentPlan = bestMutation
                currentScore = bestScore
                currentLosers = bestLosers
            } else {
                break // converged
            }
        }

        return currentPlan
    }

    /**
     * Generate a small set of promising blocking mutations from the current plan.
     * Uses combat math to filter out obviously bad candidates before expensive simulation.
     *
     * [simulatedLosers] are the blockers [currentPlan]'s own simulation lost without taking their
     * attacker with them. The printed numbers can call such a block a free kill — a 2/3 in front
     * of a 2/1 first striker — while the simulation, with the attacker's affordable pump used
     * after blocks ([anticipateCombatResponses]), shows the 2/3 dying for nothing. Removing that
     * blocker is always tried, so "don't block" gets scored against what the simulation saw.
     *
     * Returns at most ~8-12 candidate plans.
     */
    private fun generateBlockMutations(
        state: GameState,
        projected: ProjectedState,
        attackers: List<EntityId>,
        validBlockers: List<EntityId>,
        mandatoryBlockerIds: Set<EntityId>,
        currentPlan: Map<EntityId, List<EntityId>>,
        simulatedLosers: Set<EntityId> = emptySet(),
    ): List<MutableMap<EntityId, List<EntityId>>> {
        val mutations = mutableListOf<MutableMap<EntityId, List<EntityId>>>()

        val blockedAttackerIds = currentPlan.values.flatten().toSet()
        val assignedBlockerIds = currentPlan.keys

        // 1. Remove: try removing blockers assigned to low-value attackers
        //    (the heuristic might have over-blocked)
        for (blockerId in currentPlan.keys) {
            if (blockerId in mandatoryBlockerIds) continue
            val targetAttacker = currentPlan[blockerId]?.firstOrNull() ?: continue
            val blockerValue = CombatMath.creatureValue(state, projected, blockerId)
            val attackerValue = CombatMath.creatureValue(state, projected, targetAttacker)
            // Only try removing if the blocker is more valuable than what it's blocking,
            // if the blocker doesn't kill the attacker, or if the simulation lost it for nothing
            val kills = CombatMath.wouldKillInCombat(state, projected, blockerId, targetAttacker)
            if (blockerValue > attackerValue || !kills || blockerId in simulatedLosers) {
                val mutation = currentPlan.toMutableMap()
                mutation.remove(blockerId)
                mutations.add(mutation)
            }
        }

        // 2. Add: try assigning unassigned blockers to unblocked attackers
        //    (the heuristic might have under-blocked)
        val unblockedAttackers = attackers.filter { it !in blockedAttackerIds }
            .sortedByDescending { CombatMath.effectiveDamage(projected, it) }
        val unassignedBlockers = validBlockers.filter { it !in assignedBlockerIds }
            .sortedBy { CombatMath.creatureValue(state, projected, it) }

        for (attacker in unblockedAttackers.take(3)) {
            for (blocker in unassignedBlockers.take(3)) {
                if (!canBlock(state, projected, attacker, blocker)) continue
                val mutation = currentPlan.toMutableMap()
                mutation[blocker] = listOf(attacker)
                mutations.add(mutation)
            }
        }

        // 3. Move: try moving a blocker from a low-value assignment to an unblocked attacker
        if (unblockedAttackers.isNotEmpty()) {
            val worstAssignment = currentPlan.entries
                .filter { it.key !in mandatoryBlockerIds }
                .minByOrNull { (_, targets) ->
                    val attacker = targets.firstOrNull() ?: return@minByOrNull Double.MAX_VALUE
                    CombatMath.creatureValue(state, projected, attacker)
                }
            if (worstAssignment != null) {
                val bestUnblocked = unblockedAttackers.first()
                if (canBlock(state, projected, bestUnblocked, worstAssignment.key)) {
                    val mutation = currentPlan.toMutableMap()
                    mutation[worstAssignment.key] = listOf(bestUnblocked)
                    mutations.add(mutation)
                }
            }
        }

        return mutations
    }

    /**
     * Fast heuristic blocker selection using combat math only (no engine simulation).
     *
     * Three-pass priority system:
     * - Pass 1 (Free kills): blocker kills attacker AND survives — always take these
     * - Pass 2 (Profitable trades): blocker kills attacker, dies, trade favorable by value
     * - Pass 3 (Damage prevention): blocker survives without killing attacker, prevents damage
     * - Pass 1b (Free gang kills, under [freeGangBlocks]): two blockers kill the attacker and both survive
     * - Pass 4 (Gang blocks): two blockers combine to kill what neither could alone
     */
    private fun chooseBlockersViaHeuristic(
        state: GameState,
        projected: ProjectedState,
        sortedAttackers: List<EntityId>,
        validBlockers: List<EntityId>,
        assignedBlockers: MutableSet<EntityId>,
        blockerMap: MutableMap<EntityId, List<EntityId>>,
        myLife: Int
    ) {
        val tradeRatio = CombatMath.tradeWillingnessRatio(myLife)
        // Track which attackers are already blocked (e.g., by mandatory blockers)
        val blockedAttackers = blockerMap.values.flatten().toMutableSet()

        // ── Pass 1: Free kills — blocker kills attacker and survives ──
        for (attacker in sortedAttackers) {
            val blocker = findSingleBlocker(state, projected, attacker, validBlockers, assignedBlockers) { info ->
                info.effectivelyKillThem && info.weSurvive
            } ?: continue

            blockerMap[blocker] = listOf(attacker)
            assignedBlockers.add(blocker)
            blockedAttackers.add(attacker)
        }

        // ── Pass 1b: Free gang kills — two blockers kill the attacker and both survive ──
        // Better than any trade below: the attacker dies and we lose nothing.
        if (freeGangBlocks) {
            for (attacker in sortedAttackers) {
                if (attacker in blockedAttackers) continue
                val pair = freeGangBlock(state, projected, attacker, validBlockers, assignedBlockers) ?: continue
                pair.forEach { blocker ->
                    blockerMap[blocker] = listOf(attacker)
                    assignedBlockers.add(blocker)
                }
                blockedAttackers.add(attacker)
            }
        }

        // ── Pass 2: Profitable trades — blocker kills attacker but dies ──
        for (attacker in sortedAttackers) {
            if (attacker in blockedAttackers) continue
            val attackerValue = CombatMath.combatTradeValue(projected, attacker)

            val blocker = findSingleBlocker(state, projected, attacker, validBlockers, assignedBlockers) { info ->
                info.effectivelyKillThem && !info.weSurvive &&
                    info.blockerValue * tradeRatio <= attackerValue
            } ?: continue

            blockerMap[blocker] = listOf(attacker)
            assignedBlockers.add(blocker)
            blockedAttackers.add(attacker)
        }

        // ── Pass 3: Damage prevention — blocker survives, always block ──
        // If the blocker survives, the cost is essentially zero (blocking doesn't tap).
        // Always take free damage prevention.
        for (attacker in sortedAttackers) {
            if (attacker in blockedAttackers) continue
            val aPower = projected.getPower(attacker) ?: 0
            if (aPower <= 0) continue

            val blocker = findSingleBlocker(state, projected, attacker, validBlockers, assignedBlockers) { info ->
                info.weSurvive
            } ?: continue

            blockerMap[blocker] = listOf(attacker)
            assignedBlockers.add(blocker)
            blockedAttackers.add(attacker)
        }

        // ── Pass 4: Gang blocks for remaining unblocked attackers ──
        for (attacker in sortedAttackers) {
            if (attacker in blockedAttackers) continue
            val aKeywords = projected.getKeywords(attacker)
            if (Keyword.DEATHTOUCH.name in aKeywords) continue

            val aPower = projected.getPower(attacker) ?: 0
            val aToughness = projected.getToughness(attacker) ?: 0
            val aHasFirstStrike = Keyword.FIRST_STRIKE.name in aKeywords || Keyword.DOUBLE_STRIKE.name in aKeywords
            val attackerValue = CombatMath.combatTradeValue(projected, attacker)

            val available = availableBlockersFor(state, projected, attacker, validBlockers, assignedBlockers)
            if (available.size < 2) continue

            val sorted = available.sortedBy { CombatMath.creatureValue(state, projected, it) }
            for (i in sorted.indices) {
                for (j in i + 1 until sorted.size) {
                    val b1 = sorted[i]
                    val b2 = sorted[j]
                    val combinedPower = (projected.getPower(b1) ?: 0) + (projected.getPower(b2) ?: 0)
                    if (combinedPower < aToughness) continue

                    if (aHasFirstStrike) {
                        val b1Survives = !CombatMath.wouldKillInCombat(state, projected, attacker, b1)
                        val b2Survives = !CombatMath.wouldKillInCombat(state, projected, attacker, b2)
                        // Check if surviving blockers still have enough power to kill the attacker.
                        // A blocker that dies to first strike is fine if the other one finishes the job.
                        // Also count power from a dying blocker if it has first/double strike itself
                        // (it deals damage simultaneously in the first-strike step).
                        val b1Keywords = projected.getKeywords(b1)
                        val b2Keywords = projected.getKeywords(b2)
                        val b1HasFS = Keyword.FIRST_STRIKE.name in b1Keywords || Keyword.DOUBLE_STRIKE.name in b1Keywords
                        val b2HasFS = Keyword.FIRST_STRIKE.name in b2Keywords || Keyword.DOUBLE_STRIKE.name in b2Keywords
                        val survivingPower =
                            (if (b1Survives || b1HasFS) projected.getPower(b1) ?: 0 else 0) +
                            (if (b2Survives || b2HasFS) projected.getPower(b2) ?: 0 else 0)
                        if (survivingPower < aToughness) continue
                    }

                    // Cost of the gang-block = sum of every blocker the attacker can kill
                    // with its power (MTG rule 702.19c: attacker assigns lethal damage in order).
                    // The attacker picks the order that kills as many blockers as possible, so
                    // start from the lowest-toughness blocker.
                    val blockersByToughness = listOf(b1, b2)
                        .sortedBy { projected.getToughness(it) ?: 0 }
                    var damageLeft = aPower
                    var dyingValue = 0.0
                    for (b in blockersByToughness) {
                        val bToughness = projected.getToughness(b) ?: 0
                        if (damageLeft >= bToughness) {
                            dyingValue += CombatMath.combatTradeValue(projected, b)
                            damageLeft -= bToughness
                        }
                    }
                    if (attackerValue > dyingValue * 1.2) {
                        blockerMap[b1] = listOf(attacker)
                        blockerMap[b2] = listOf(attacker)
                        assignedBlockers.add(b1)
                        assignedBlockers.add(b2)
                        blockedAttackers.add(attacker)
                        break
                    }
                }
                if (attacker in blockedAttackers) break
            }
        }

        // ── Pass 5: Triple gang blocks for large creatures that pairs can't kill ──
        for (attacker in sortedAttackers) {
            if (attacker in blockedAttackers) continue
            val aKeywords = projected.getKeywords(attacker)
            if (Keyword.DEATHTOUCH.name in aKeywords) continue

            val aPower = projected.getPower(attacker) ?: 0
            val aToughness = projected.getToughness(attacker) ?: 0
            val aHasFirstStrike = Keyword.FIRST_STRIKE.name in aKeywords || Keyword.DOUBLE_STRIKE.name in aKeywords
            val attackerValue = CombatMath.combatTradeValue(projected, attacker)

            val available = availableBlockersFor(state, projected, attacker, validBlockers, assignedBlockers)
            if (available.size < 3) continue

            // Cap candidates to keep combinatorics reasonable (C(8,3) = 56)
            val sorted = available
                .sortedBy { CombatMath.combatTradeValue(projected, it) }
                .take(8)

            var found = false
            for (i in sorted.indices) {
                if (found) break
                for (j in i + 1 until sorted.size) {
                    if (found) break
                    for (k in j + 1 until sorted.size) {
                        val b1 = sorted[i]; val b2 = sorted[j]; val b3 = sorted[k]
                        val combinedPower = (projected.getPower(b1) ?: 0) +
                            (projected.getPower(b2) ?: 0) + (projected.getPower(b3) ?: 0)
                        if (combinedPower < aToughness) continue

                        if (aHasFirstStrike) {
                            val survivors = listOf(b1, b2, b3).filter { blocker ->
                                !CombatMath.wouldKillInCombat(state, projected, attacker, blocker) ||
                                    Keyword.FIRST_STRIKE.name in projected.getKeywords(blocker) ||
                                    Keyword.DOUBLE_STRIKE.name in projected.getKeywords(blocker)
                            }
                            val survivingPower = survivors.sumOf { projected.getPower(it) ?: 0 }
                            if (survivingPower < aToughness) continue
                        }

                        // Value check: attacker must be worth more than the blockers we lose.
                        // Use combatTradeValue (ignores tapped/sickness state multipliers).
                        val blockersByToughness = listOf(b1, b2, b3)
                            .sortedBy { projected.getToughness(it) ?: 0 }
                        var damageLeft = aPower
                        var dyingValue = 0.0
                        for (b in blockersByToughness) {
                            val bToughness = projected.getToughness(b) ?: 0
                            if (damageLeft >= bToughness) {
                                dyingValue += CombatMath.combatTradeValue(projected, b)
                                damageLeft -= bToughness
                            }
                        }
                        if (attackerValue > dyingValue * 1.2) {
                            blockerMap[b1] = listOf(attacker)
                            blockerMap[b2] = listOf(attacker)
                            blockerMap[b3] = listOf(attacker)
                            assignedBlockers.add(b1)
                            assignedBlockers.add(b2)
                            assignedBlockers.add(b3)
                            blockedAttackers.add(attacker)
                            found = true
                        }
                    }
                }
            }
        }
    }

    /**
     * The cheapest pair of blockers that kills [attacker] while neither of them can die to it:
     * each survives the attacker's whole power (or is indestructible), the attacker has no
     * deathtouch, and the pair's combat damage reaches its toughness. Null when there is none, or
     * when the attacker can't be destroyed by damage.
     */
    private fun freeGangBlock(
        state: GameState,
        projected: ProjectedState,
        attacker: EntityId,
        validBlockers: List<EntityId>,
        assignedBlockers: Set<EntityId>,
    ): List<EntityId>? {
        val aKeywords = projected.getKeywords(attacker)
        if (Keyword.DEATHTOUCH.name in aKeywords || Keyword.INDESTRUCTIBLE.name in aKeywords) return null
        val aPower = projected.getPower(attacker) ?: 0
        val aToughness = projected.getToughness(attacker) ?: 0
        if (aToughness <= 0) return null
        // Damage each survivor deals to the attacker; deathtouch makes any of it lethal.
        val survivors = availableBlockersFor(state, projected, attacker, validBlockers, assignedBlockers)
            .filter { blocker ->
                val bKeywords = projected.getKeywords(blocker)
                (Keyword.INDESTRUCTIBLE.name in bKeywords || (projected.getToughness(blocker) ?: 0) > aPower) &&
                    (projected.getPower(blocker) ?: 0) > 0 &&
                    CombatMath.blockerDealsDamage(state, projected, attacker, blocker)
            }
            .associateWith { blocker ->
                val power = projected.getPower(blocker) ?: 0
                if (Keyword.DEATHTOUCH.name in projected.getKeywords(blocker)) maxOf(power, aToughness) else power
            }
        if (survivors.size < 2) return null
        val ids = survivors.keys.toList()
        var best: List<EntityId>? = null
        var bestCost = Double.MAX_VALUE
        for (i in ids.indices) {
            for (j in i + 1 until ids.size) {
                if (survivors.getValue(ids[i]) + survivors.getValue(ids[j]) < aToughness) continue
                val cost = CombatMath.creatureValue(state, projected, ids[i]) + CombatMath.creatureValue(state, projected, ids[j])
                if (cost < bestCost) {
                    bestCost = cost
                    best = listOf(ids[i], ids[j])
                }
            }
        }
        return best
    }

    /**
     * Info about a potential blocker for evaluating blocking decisions.
     */
    private data class BlockInfo(
        val blockerId: EntityId,
        val effectivelyKillThem: Boolean,
        val weSurvive: Boolean,
        val blockerValue: Double
    )

    /**
     * Find the best single blocker for an attacker matching the given predicate.
     * Returns the cheapest matching blocker, or null if none qualifies.
     */
    private fun findSingleBlocker(
        state: GameState,
        projected: ProjectedState,
        attacker: EntityId,
        validBlockers: List<EntityId>,
        assignedBlockers: Set<EntityId>,
        predicate: (BlockInfo) -> Boolean
    ): EntityId? {
        if (needsTwoBlockers(projected, attacker)) return null
        val aPower = projected.getPower(attacker) ?: 0
        val aToughness = projected.getToughness(attacker) ?: 0
        val aKeywords = projected.getKeywords(attacker)
        val aHasDeathtouch = Keyword.DEATHTOUCH.name in aKeywords
        val aHasFirstStrike = Keyword.FIRST_STRIKE.name in aKeywords || Keyword.DOUBLE_STRIKE.name in aKeywords

        return availableBlockersFor(state, projected, attacker, validBlockers, assignedBlockers)
            .mapNotNull { blockerId ->
                val bPower = projected.getPower(blockerId) ?: 0
                val bToughness = projected.getToughness(blockerId) ?: 0
                val bKeywords = projected.getKeywords(blockerId)
                val bHasDeathtouch = Keyword.DEATHTOUCH.name in bKeywords
                val bHasFirstStrike = Keyword.FIRST_STRIKE.name in bKeywords || Keyword.DOUBLE_STRIKE.name in bKeywords
                val blockerValue = CombatMath.combatTradeValue(projected, blockerId)

                // A block trigger that destroys the attacker resolves before any combat damage, so
                // first strike cannot stop it; when it also destroys the blocker, the blocker dies.
                val blockTrigger = blockTriggerPredicates?.let { predicates ->
                    BlockTriggerRemoval.against(state, projected, cardRegistry!!, predicates, blockerId, attacker)
                }
                val weKillThem = bPower >= aToughness || bHasDeathtouch
                val bIsIndestructible = Keyword.INDESTRUCTIBLE.name in bKeywords
                val weSurvive = if (bIsIndestructible) {
                    true // indestructible creatures always survive combat
                } else if (blockTrigger != null) {
                    !blockTrigger.destroysItself
                } else if (!aHasDeathtouch) {
                    (bToughness > aPower) ||
                        (bHasFirstStrike && !aHasFirstStrike && weKillThem)
                } else {
                    false
                }
                val blockerDealsDamage = CombatMath.blockerDealsDamage(state, projected, attacker, blockerId)
                val effectivelyKillThem = blockTrigger != null || (weKillThem && blockerDealsDamage)

                val info = BlockInfo(blockerId, effectivelyKillThem, weSurvive, blockerValue)
                if (predicate(info)) info else null
            }
            .minByOrNull { it.blockerValue }
            ?.blockerId
    }

    /**
     * A simulated blocking plan: its board score, and the blockers it lost without taking anything
     * with them — dead after combat while every attacker they blocked is still on the battlefield.
     * Read off the simulated outcome rather than predicted, so first strike, deathtouch, a pump or
     * a damage-assignment order all count as they actually resolved.
     */
    private data class BlockPlanOutcome(
        val score: Double,
        val losingBlockers: Set<EntityId>,
        /** We are dead after this combat — read off the simulation, so an attacker's pump counts. */
        val lethal: Boolean = false,
    )

    /**
     * Simulate a blocking plan through the engine's combat resolution and return
     * the board evaluation score from the blocker's perspective.
     *
     * Returns null if the simulation fails.
     */
    private fun evaluateBlockingPlan(
        state: GameState,
        playerId: EntityId,
        blockerMap: Map<EntityId, List<EntityId>>
    ): BlockPlanOutcome? {
        val blockAction = DeclareBlockers(playerId, blockerMap)
        val responders = responses?.respondersAt(state, playerId).orEmpty()
        val simResult = simulator.simulate(state, blockAction, stopBeforeCombatDamage = responders.isNotEmpty())
        if (simResult is SimulationResult.Illegal || simResult is SimulationResult.StoppedAtLimit) return null

        var current = simResult.state

        if (responses != null && responders.isNotEmpty()) {
            // The attacker gets its window after our blocks: a pump or a pinger it can afford.
            current = responses.playOut(current, playerId, responders, declareBlocks = null) ?: return null
        } else {
            // Drive through combat damage resolution
            var iterations = 0
            while (iterations < 50 && !current.gameOver && current.pendingDecision == null) {
                iterations++
                val priorityPlayer = current.priorityPlayerId ?: break
                val transition = simulator.simulate(current, PassPriority(priorityPlayer))
                if (transition is SimulationResult.StoppedAtLimit) return null
                current = transition.state
                if (current.phase != Phase.COMBAT) break
            }
        }

        val postProjected = current.projectedState
        val baseScore = evaluator.evaluate(current, postProjected, playerId)

        // Estimate our next-turn attack potential: what damage can we push through?
        // Scored against the opponent it pays off best against — in a pod we get to choose who to
        // swing at, so the counter-attack is worth what its *best* target is worth. In 1v1 there is
        // one opponent and this is the original single calculation.
        val myAttackers = CombatMath.getCreaturesThatCanAttack(current, postProjected, playerId)
        val counterAttackBonus = current.getOpponents(playerId).maxOfOrNull { opponentId ->
            val opponentBlockers = CombatMath.getOpponentUntappedCreatures(current, postProjected, opponentId)
            val ourDamageThrough = if (myAttackers.isNotEmpty()) {
                CombatMath.calculateDamageThroughOptimalBlocking(current, postProjected, myAttackers, opponentBlockers)
            } else 0
            val opponentLife = current.lifeTotal(opponentId)

            // Small bonus for blocking plans that preserve our attack potential.
            // Nudges the AI toward blocks that keep our counter-attack alive.
            if (ourDamageThrough >= opponentLife) {
                3.0
            } else if (ourDamageThrough > 0) {
                ourDamageThrough.toDouble() * 0.15
            } else {
                0.0
            }
        } ?: 0.0

        val survivors = current.getBattlefield().toSet()
        val losingBlockers = blockerMap.filter { (blocker, blocked) ->
            blocked.isNotEmpty() && blocker !in survivors && blocked.all { it in survivors }
        }.keys

        // Life is one way to lose this combat; commander damage (CR 903.10a) is another.
        val lethal = current.lifeTotal(playerId) <= 0 || current.getEntity(playerId)?.has<PlayerLostComponent>() == true
        return BlockPlanOutcome(baseScore + counterAttackBonus, losingBlockers, lethal)
    }

    /**
     * Check if unblocked damage would kill us — either this turn (immediate lethal)
     * or set us up to die on the opponent's next attack (next-turn lethal).
     *
     * The next-turn check considers all opponent creatures (they'll untap) vs our
     * available blockers, and returns true if the combined damage across both turns
     * would be fatal. This makes the AI block more aggressively when at low life.
     */
    private fun isLifeInDanger(
        state: GameState,
        projected: ProjectedState,
        attackers: List<EntityId>,
        blockerMap: Map<EntityId, List<EntityId>>,
        myLife: Int,
        playerId: EntityId,
        countCreaturesThatUntap: Boolean = false,
    ): Boolean {
        // Immediate lethal — to life or to one commander's damage
        if (isLethalThisCombat(state, projected, attackers, blockerMap, myLife, playerId)) return true
        val incomingDamage = calculateIncomingDamage(state, projected, attackers, blockerMap)

        // Next-turn check: after taking this damage, would opponent's next attack kill us?
        val lifeAfter = myLife - incomingDamage

        // Our blockers next turn: untapped creatures that aren't currently assigned to block
        // (conservatively — some may die in this combat, but this is a fast heuristic)
        //
        // [countCreaturesThatUntap] corrects the "untapped" half: the opponent's next attack comes
        // after *our* untap step, so a creature that attacked this turn is a blocker by then.
        // Counting only today's untapped creatures overstates the danger after every swing of ours,
        // and an overstated danger is exactly what licenses a chump.
        val myBlockers = projected.getBattlefieldControlledBy(playerId)
            .filter { entityId ->
                projected.isCreature(entityId) && (
                    state.getEntity(entityId)?.has<TappedComponent>() != true ||
                        (countCreaturesThatUntap && !projected.doesntUntapDuringUntapStep(entityId))
                    )
            }

        // The commander damage this combat deals is not in [state] yet; the next swing adds to it.
        val blockedAttackerIds = blockerMap.values.flatten().toSet()
        val nextTurnDamage = incomingNextTurnDamage(
            state, projected, playerId, myBlockers, life = lifeAfter,
            commanderDamageThisCombat = { attacker ->
                if (attacker !in attackers) 0
                else commanderDamageToUs(
                    state, attacker, playerId, incomingDamageFrom(state, projected, attacker, blockedAttackerIds, blockerMap),
                )
            },
        )
        if (nextTurnDamage > 0 && lifeAfter <= nextTurnDamage) return true

        return false
    }

    /**
     * Damage the most dangerous opposing *side* can push through [myBlockers] on its next turn.
     *
     * Summed within a team (CR 805.10 — teammates attack in one combat, against one set of
     * blockers) but maxed across teams, because opposing teams attack on separate turns and we
     * untap in between. In a two-player game this is one team of one player: the original call.
     *
     * In life units: a swing that kills us with commander damage (CR 903.10a) — one opposing
     * commander reaching the damage it still needs through our best blocks
     * ([CombatMath.commanderDamageLethal]) — is reported as at least [life], so every caller's
     * "`>= life` is lethal" check reads it as lethal. [commanderDamageThisCombat] is commander
     * damage a combat still being planned will add before that swing.
     */
    private fun incomingNextTurnDamage(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        myBlockers: List<EntityId>,
        chumpBlocks: Boolean = true,
        stackOnTramplers: Boolean = false,
        life: Int = state.lifeTotal(playerId),
        commanderDamageThisCombat: (EntityId) -> Int = { 0 },
    ): Int {
        val sides = state.sidesFor(playerId) ?: return 0
        // A creature that can't block is no defence, however untapped it is. Counting it made
        // attacking with one (Aesthir Glider) look like it opened the crack-back it never closed.
        val blockers = myBlockers.filterNot { projected.cantBlock(it) }
        return sides.opponents.maxOf { team ->
            val damage = team.sumOf { CombatMath.estimateNextTurnDamage(state, projected, it, blockers, chumpBlocks, stackOnTramplers) }
            val commanderLethal = team.any { opponent ->
                CombatMath.commanderDamageLethal(
                    state, projected, CombatMath.nextTurnAttackers(state, projected, opponent), playerId, blockers,
                    chumpBlocks, stackOnTramplers, commanderDamageThisCombat,
                )
            }
            if (commanderLethal) maxOf(damage, life) else damage
        }
    }

    /**
     * Add chump blocks to [plan] while each one costs less than it saves on the opponent's next
     * attack — life priced by the clock, not by whether this hit alone is lethal.
     *
     * A plan's price is the creature value its added chumps throw away now plus
     * [nextAttackChumpCost]: what it would take to survive the attack after, from the life and the
     * blockers this plan leaves us. Production game `5ae3888c` turn 14 is the shape: at 12 life
     * facing a 10/9 with a 6/4 menace waiting behind it, chumping the 10/9 with a 2/1 leaves 12
     * life and one chump (the 10/9 again) needed next turn; taking the hit leaves 2 life, where
     * surviving takes three bodies — the 10/9's chump and both halves of the menace pair. The old
     * next-turn test let every creature chump next turn and gave menace one body, so it read both
     * lines as safe and the AI took ten.
     *
     * The floor stays a floor: when the next attack is survivable without chumps, or the chump
     * would not lower the price (dead either way, or the hit does not change what next turn
     * costs), nothing is added. When our own next attack is lethal through their remaining
     * blockers, the race is won and the bodies are kept to swing with.
     */
    private fun clockChumps(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        attackers: List<EntityId>,
        validBlockers: List<EntityId>,
        plan: MutableMap<EntityId, List<EntityId>>,
        myLife: Int,
    ): MutableMap<EntityId, List<EntityId>> {
        if (winsTheRaceFirst(state, projected, playerId, attackers, plan)) return plan

        var current = plan
        var price = nextAttackChumpCost(state, projected, playerId, attackers, current, myLife)
        repeat(attackers.size) {
            if (price == 0.0) return current
            val assigned = current.keys
            val blocked = current.values.flatten().toSet()
            val best = attackers
                .filter { it !in blocked }
                .mapNotNull { attacker ->
                    val needed = if (needsTwoBlockers(projected, attacker)) 2 else 1
                    val trample = Keyword.TRAMPLE.name in projected.getKeywords(attacker)
                    val bodies = availableBlockersFor(state, projected, attacker, validBlockers, assigned)
                        .sortedWith(
                            if (trample) {
                                compareByDescending<EntityId> { projected.getToughness(it) ?: 0 }
                                    .thenBy { CombatMath.creatureValue(state, projected, it) }
                            } else {
                                compareBy { CombatMath.creatureValue(state, projected, it) }
                            }
                        )
                        .take(needed)
                    if (bodies.size < needed) return@mapNotNull null
                    val candidate = current.toMutableMap().apply { bodies.forEach { put(it, listOf(attacker)) } }
                    val cost = bodies.sumOf { CombatMath.creatureValue(state, projected, it).coerceAtLeast(MIN_BODY_VALUE) }
                    val next = nextAttackChumpCost(state, projected, playerId, attackers, candidate, myLife)
                    Triple(candidate, cost, next)
                }
                .minByOrNull { (_, cost, next) -> cost + next }
                ?: return current
            val (candidate, cost, next) = best
            if (cost + next >= price - CLOCK_MARGIN) return current
            current = candidate
            price = next
        }
        return current
    }

    /**
     * Creature value we would have to chump away to survive the strongest opposing side's next
     * attack after this combat resolves under [plan] — [Double.POSITIVE_INFINITY] when it cannot
     * be survived, or this combat already kills us.
     *
     * Their attackers are every creature that can attack next turn, summoning-sick ones included
     * (they will not be by then), less the ones [plan] kills. Our blockers are every creature that
     * untaps before then, less the ones [plan] loses.
     */
    private fun nextAttackChumpCost(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        attackers: List<EntityId>,
        plan: Map<EntityId, List<EntityId>>,
        myLife: Int,
    ): Double {
        val lifeAfter = myLife - calculateIncomingDamage(state, projected, attackers, plan)
        if (lifeAfter <= 0) return Double.POSITIVE_INFINITY
        val (deadBlockers, deadAttackers) = combatCasualties(state, projected, plan)
        val ourBlockers = projected.getBattlefieldControlledBy(playerId).filter {
            projected.isCreature(it) && it !in deadBlockers && !projected.cantBlock(it) && untapsInTime(state, projected, it)
        }
        val sides = state.sidesFor(playerId) ?: return 0.0
        return sides.opponents.maxOfOrNull { team ->
            val theirAttackers = team.flatMap { projected.getBattlefieldControlledBy(it) }.filter {
                projected.isCreature(it) && it !in deadAttackers &&
                    Keyword.DEFENDER.name !in projected.getKeywords(it) && !projected.cantAttack(it) &&
                    untapsInTime(state, projected, it)
            }
            CombatMath.survivalChumpCost(state, projected, theirAttackers, ourBlockers, lifeAfter)
                ?: Double.POSITIVE_INFINITY
        } ?: 0.0
    }

    /** Untapped now, or untaps in its controller's next untap step. */
    private fun untapsInTime(state: GameState, projected: ProjectedState, entityId: EntityId): Boolean =
        state.getEntity(entityId)?.has<TappedComponent>() != true || !projected.doesntUntapDuringUntapStep(entityId)

    /**
     * Blockers [plan] loses and attackers it kills, read from combat math: a blocker dies when an
     * attacker it blocks would kill it on its own; an attacker dies when one of its blockers kills
     * it alone or their combined power reaches its toughness.
     */
    private fun combatCasualties(
        state: GameState,
        projected: ProjectedState,
        plan: Map<EntityId, List<EntityId>>,
    ): Pair<Set<EntityId>, Set<EntityId>> {
        val deadBlockers = plan.filter { (blocker, blocked) ->
            blocked.any { CombatMath.wouldKillInCombat(state, projected, it, blocker) }
        }.keys
        val deadAttackers = plan.values.flatten().toSet().filter { attacker ->
            val blockers = plan.filterValues { attacker in it }.keys
                .filter { CombatMath.blockerDealsDamage(state, projected, attacker, it) }
            blockers.any { CombatMath.wouldKillInCombat(state, projected, it, attacker) } ||
                (blockers.isNotEmpty() && blockers.sumOf { projected.getPower(it) ?: 0 } >= (projected.getToughness(attacker) ?: 0) &&
                    Keyword.INDESTRUCTIBLE.name !in projected.getKeywords(attacker))
        }.toSet()
        return deadBlockers to deadAttackers
    }

    /**
     * Two-player only: after this combat resolves under [plan], is our next attack lethal through
     * the blockers the attacking player will have on our turn — its creatures still untapped now
     * (the ones that attacked stay tapped) that survive this combat? Then the race is ours and the
     * clock pass keeps its bodies to swing with.
     */
    private fun winsTheRaceFirst(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        attackers: List<EntityId>,
        plan: Map<EntityId, List<EntityId>>,
    ): Boolean {
        val opponent = state.getOpponents(playerId).singleOrNull() ?: return false
        val (deadBlockers, deadAttackers) = combatCasualties(state, projected, plan)
        val ourAttackers = projected.getBattlefieldControlledBy(playerId).filter {
            projected.isCreature(it) && it !in deadBlockers && !projected.cantAttack(it) &&
                Keyword.DEFENDER.name !in projected.getKeywords(it) && untapsInTime(state, projected, it)
        }
        if (ourAttackers.isEmpty()) return false
        val theirBlockers = CombatMath.getOpponentUntappedCreatures(state, projected, opponent)
            .filter { it !in deadAttackers && it !in attackers }
        return CombatMath.isLethalAttack(state, projected, ourAttackers, theirBlockers, state.lifeTotal(opponent))
    }

    /**
     * How much a chump block on [attacker] is worth, as a sort key: the damage it takes off the
     * table, biggest first.
     *
     * A trampler keeps hitting us for everything the blocker cannot soak, so a chump in front of
     * one buys a point or two; a chump in front of anything else buys the whole hit. Scaling
     * separates the two groups outright rather than trying to price them on one scale — no
     * trampler is ever worth chumping ahead of a non-trampler.
     */
    private fun chumpPriority(projected: ProjectedState, attacker: EntityId): Int {
        val power = projected.getPower(attacker) ?: 0
        return if (Keyword.TRAMPLE.name in projected.getKeywords(attacker)) -power else power * 1000
    }

    /**
     * Assign blockers for damage prevented rather than for value: the biggest hits first, paid for
     * with the cheapest legal body that can stand in front of each.
     *
     * Only [mandatoryMap] is carried over — every other blocker is put back in the pool, which is
     * the point. This is the plan for a turn we do not survive otherwise, so a blocker committed to
     * a trade or to the second half of a gang block is a blocker standing in the wrong place.
     */
    private fun damageMinimisingPlan(
        state: GameState,
        projected: ProjectedState,
        attackers: List<EntityId>,
        validBlockers: List<EntityId>,
        mandatoryMap: Map<EntityId, List<EntityId>>,
    ): MutableMap<EntityId, List<EntityId>> {
        val plan = mandatoryMap.toMutableMap()
        val assigned = plan.keys.toMutableSet()
        val alreadyBlocked = plan.values.flatten().toSet()

        val byDamage = attackers
            .filter { it !in alreadyBlocked }
            .sortedByDescending { chumpPriority(projected, it) }

        for (attacker in byDamage) {
            val keywords = projected.getKeywords(attacker)
            // Menace wants two bodies or none — a lone blocker on it is an illegal plan that
            // `fixMenaceAssignments` would strip right back out, damage and all.
            val needed = if (Keyword.MENACE.name in keywords) 2 else 1
            val available = availableBlockersFor(state, projected, attacker, validBlockers, assigned)
                .sortedWith(
                    if (Keyword.TRAMPLE.name in keywords) {
                        // Trample spills whatever the blockers cannot soak, so here the wall is
                        // worth more than the cheap body.
                        compareByDescending<EntityId> { projected.getToughness(it) ?: 0 }
                            .thenBy { CombatMath.creatureValue(state, projected, it) }
                    } else {
                        compareBy { CombatMath.creatureValue(state, projected, it) }
                    }
                )
            if (available.size < needed) continue
            available.take(needed).forEach { blocker ->
                plan[blocker] = listOf(attacker)
                assigned.add(blocker)
            }
        }
        return plan
    }

    /**
     * Add spare blockers behind tramplers until [plan] stops being lethal at [myLife].
     *
     * [damageMinimisingPlan] gives each attacker one body (two for menace), which is all a
     * non-trampler needs — but a trampler spills whatever its blockers cannot soak, so against one
     * a single chump buys a point or two and the plan stays lethal. Each step puts the spare
     * blocker that soaks the most (its toughness; any body against deathtouch, where one damage is
     * lethal) behind the trampler spilling the most, and stops once we survive or no spare blocker
     * can stand behind a spilling trampler. Mutates [plan].
     */
    private fun stackOnTramplers(
        state: GameState,
        projected: ProjectedState,
        attackers: List<EntityId>,
        validBlockers: List<EntityId>,
        plan: MutableMap<EntityId, List<EntityId>>,
        myLife: Int,
    ) {
        val assigned = plan.keys.toMutableSet()
        while (calculateIncomingDamage(state, projected, attackers, plan) >= myLife) {
            val step = attackers
                .filter { attacker ->
                    Keyword.TRAMPLE.name in projected.getKeywords(attacker) &&
                        // Unblocked tramplers had no blocker to spare in [damageMinimisingPlan];
                        // stacking only ever deepens a block already made.
                        plan.values.any { attacker in it } &&
                        !(defendPlaneswalkers && !attacksAPlayer(state, attacker))
                }
                .map { it to trampleSpill(projected, it, plan) }
                .filter { (_, spill) -> spill > 0 }
                .sortedByDescending { (_, spill) -> spill }
                .firstNotNullOfOrNull { (attacker, _) ->
                    val deathtouch = Keyword.DEATHTOUCH.name in projected.getKeywords(attacker)
                    availableBlockersFor(state, projected, attacker, validBlockers, assigned)
                        .filter { (projected.getToughness(it) ?: 0) > 0 }
                        .sortedWith(
                            compareByDescending<EntityId> { if (deathtouch) 1 else projected.getToughness(it) ?: 0 }
                                .thenBy { CombatMath.creatureValue(state, projected, it) }
                        )
                        .firstOrNull()
                        ?.let { attacker to it }
                } ?: return
            val (attacker, blocker) = step
            plan[blocker] = listOf(attacker)
            assigned.add(blocker)
        }
    }

    /** What a blocked trampler still pushes through its blockers in [plan]. */
    private fun trampleSpill(projected: ProjectedState, attacker: EntityId, plan: Map<EntityId, List<EntityId>>): Int {
        val keywords = projected.getKeywords(attacker)
        val blockers = plan.filterValues { attacker in it }.keys
        val lethalToBlockers = if (Keyword.DEATHTOUCH.name in keywords) {
            blockers.size
        } else {
            blockers.sumOf { projected.getToughness(it) ?: 0 }
        }
        val hits = if (stackChumpsOnTramplers && Keyword.DOUBLE_STRIKE.name in keywords) 2 else 1
        return (hits * (projected.getPower(attacker) ?: 0) - lethalToBlockers).coerceAtLeast(0)
    }

    /**
     * Calculate damage that will get through given current blocking assignments.
     * Accounts for unblocked creatures and trample overflow.
     */
    private fun calculateIncomingDamage(
        state: GameState,
        projected: ProjectedState,
        attackers: List<EntityId>,
        blockerMap: Map<EntityId, List<EntityId>>
    ): Int {
        val blockedAttackerIds = blockerMap.values.flatten().toSet()
        return attackers.sumOf { incomingDamageFrom(state, projected, it, blockedAttackerIds, blockerMap) }
    }

    /** The share of [calculateIncomingDamage] that [attacker] deals us. */
    private fun incomingDamageFrom(
        state: GameState,
        projected: ProjectedState,
        attacker: EntityId,
        blockedAttackerIds: Set<EntityId>,
        blockerMap: Map<EntityId, List<EntityId>>,
    ): Int {
        val aPower = projected.getPower(attacker) ?: 0
        if (aPower <= 0) return 0
        // Damage aimed at a planeswalker or a battle is not damage to us (CR 510.1b).
        if (defendPlaneswalkers && !attacksAPlayer(state, attacker)) return 0
        val aKeywords = projected.getKeywords(attacker)
        return if (attacker !in blockedAttackerIds) {
            // A double striker hits twice; counted under [stackChumpsOnTramplers].
            val hits = if (stackChumpsOnTramplers && Keyword.DOUBLE_STRIKE.name in aKeywords) 2 else 1
            hits * aPower
        } else if (Keyword.TRAMPLE.name in aKeywords) {
            trampleSpill(projected, attacker, blockerMap)
        } else {
            0
        }
    }

    /**
     * Whether [blockerMap] lets this combat kill [playerId]: the damage through it reaches
     * [myLife], or one attacking commander's share reaches the commander damage we still have room
     * for ([CombatMath.commanderDamageRoom], CR 903.10a). The one "is this plan lethal" test the
     * chump, survival and clock passes and [isLifeInDanger] share.
     */
    private fun isLethalThisCombat(
        state: GameState,
        projected: ProjectedState,
        attackers: List<EntityId>,
        blockerMap: Map<EntityId, List<EntityId>>,
        myLife: Int,
        playerId: EntityId,
    ): Boolean {
        val blockedAttackerIds = blockerMap.values.flatten().toSet()
        val shares = attackers.associateWith { incomingDamageFrom(state, projected, it, blockedAttackerIds, blockerMap) }
        if (shares.values.sum() >= myLife) return true
        return shares.any { (attacker, damage) ->
            damage > 0 && commanderDamageToUs(state, attacker, playerId, damage) >=
                (CombatMath.commanderDamageRoom(state, attacker, playerId) ?: return@any false)
        }
    }

    /** [damage] when [attacker] is attacking [playerId] (not a teammate or a walker), else 0. */
    private fun commanderDamageToUs(state: GameState, attacker: EntityId, playerId: EntityId, damage: Int): Int =
        if (defenderOf(state, attacker)?.let { it == playerId } != false) damage else 0



    /**
     * Simulate a full attack with an arbitrary set of attackers: declare attackers,
     * pass priority through to declare blockers, let the blocking AI choose blocks,
     * then resolve through combat damage to get the post-combat state.
     *
     * Returns the post-combat GameState, or null if the simulation fails.
     */
    private fun simulateFullAttack(
        state: GameState,
        playerId: EntityId,
        opponentId: EntityId,
        attackerMap: Map<EntityId, EntityId>,
        /** False: every defending player declares no blocks, instead of the heuristic's. */
        defenderBlocks: Boolean = true,
    ): GameState? {
        val attackAction = DeclareAttackers(playerId, attackerMap)
        val responders = responses?.respondersAt(state, playerId).orEmpty()
        if (responses != null && responders.isNotEmpty()) {
            val declared = simulator.simulate(state, attackAction, stopBeforeCombatDamage = true)
            if (declared is SimulationResult.Illegal || declared is SimulationResult.StoppedAtLimit) return null
            return responses.playOut(
                declared.state, playerId, responders,
                declareBlocks = { current, offer, defender ->
                    if (defenderBlocks) chooseBlockers(current, offer, defender, useSimulation = false)
                    else DeclareBlockers(defender, emptyMap())
                },
                emptyBlocksMustBeLegal = !defenderBlocks,
            )
        }
        val simResult = simulator.simulate(state, attackAction)
        if (simResult is SimulationResult.Illegal || simResult is SimulationResult.StoppedAtLimit) return null
        var current = simResult.state

        // Drive through combat: pass priority, handle blockers, resolve damage.
        var iterations = 0
        // In a pod a plan can attack several seats (CR 508.1b): each one attacked declares its
        // own blocks, so the heuristic blocker answers for whichever defending player is asked.
        val blocked = mutableSetOf<EntityId>()
        while (iterations < 50 && !current.gameOver && current.pendingDecision == null) {
            iterations++
            val priorityPlayer = current.priorityPlayerId ?: break

            if (priorityPlayer != playerId && priorityPlayer !in blocked && current.step == Step.DECLARE_BLOCKERS) {
                val legalActions = simulator.getLegalActions(current, priorityPlayer)
                val blockAction = legalActions.find { it.actionType == "DeclareBlockers" }
                if (blockAction != null) {
                    val blockerAction = if (defenderBlocks) {
                        chooseBlockers(current, blockAction, priorityPlayer, useSimulation = false)
                    } else {
                        DeclareBlockers(priorityPlayer, emptyMap())
                    }
                    val transition = simulator.simulate(current, blockerAction)
                    if (transition is SimulationResult.StoppedAtLimit) return null
                    // Declining every block can be illegal (a creature that must block); then
                    // there is no such response to score.
                    if (!defenderBlocks && transition is SimulationResult.Illegal) return null
                    current = transition.state
                    blocked += priorityPlayer
                    continue
                }
            }

            val transition = simulator.simulate(current, PassPriority(priorityPlayer))
            if (transition is SimulationResult.StoppedAtLimit) return null
            current = transition.state
            if (current.phase != Phase.COMBAT) break
        }

        return current
    }

    /**
     * Evaluate an attack plan by simulating it through the engine.
     * After this combat resolves, also estimates the opponent's counter-attack
     * on their next turn using [CombatMath] (no recursive simulation — just math).
     *
     * Returns the board score from the attacker's perspective, or null on failure.
     */
    private fun evaluateAttackPlan(
        state: GameState,
        playerId: EntityId,
        opponentId: EntityId,
        attackerMap: Map<EntityId, EntityId>,
        /**
         * Read the board after combat with every until-end-of-turn floating effect gone, as it
         * will be when the opponent's crack-back arrives. Set by [plannedAttackFrom], which
         * compares a pumped board's plan with an unpumped one: unsettled, the pump's toughness is
         * still standing guard in the crack-back estimate.
         */
        settleExpiring: Boolean = false,
    ): Double? {
        val simulated = simulateFullAttack(state, playerId, opponentId, attackerMap)
            ?: return null
        val predicted = scoreAttackOutcome(state, simulated, playerId, attackerMap, settleExpiring)
        if (!attacksReadTheRace || attackerMap.isEmpty() || !defenderLostACreature(state, simulated, playerId)) {
            return predicted
        }
        // The predicted blocks cost the defender a creature. Not blocking is also on offer, and it
        // keeps that creature for the swing back: the plan is worth the worse of the two.
        val unblocked = simulateFullAttack(state, playerId, opponentId, attackerMap, defenderBlocks = false)
            ?: return predicted
        return minOf(predicted, scoreAttackOutcome(state, unblocked, playerId, attackerMap, settleExpiring))
    }

    /** Some creature an opponent controlled before the attack is gone after it. */
    private fun defenderLostACreature(state: GameState, after: GameState, playerId: EntityId): Boolean {
        val projected = state.projectedState
        val survivors = after.getBattlefield().toSet()
        return state.getOpponents(playerId).any { opponent ->
            projected.getBattlefieldControlledBy(opponent).any { projected.isCreature(it) && it !in survivors }
        }
    }

    /** [evaluateAttackPlan]'s score of one simulated combat, [simulated]. */
    private fun scoreAttackOutcome(
        state: GameState,
        simulated: GameState,
        playerId: EntityId,
        attackerMap: Map<EntityId, EntityId>,
        settleExpiring: Boolean,
    ): Double {
        val postCombat = when {
            settleExpiring ->
                simulated.copy(floatingEffects = simulated.floatingEffects.filter { it.duration != Duration.EndOfTurn })
            // A creature stolen until end of turn goes home at cleanup, before anything this score
            // prices happens: it cannot block the crack-back for us, and if it died attacking, the
            // opponent lost it. Read as ours, it was a blocker to keep home and an asset to protect,
            // and the AI cast Threaten and then declared no attack.
            borrowedCreaturesGoHome -> simulated.copy(
                floatingEffects = simulated.floatingEffects.filterNot {
                    it.duration == Duration.EndOfTurn && it.effect.layer == Layer.CONTROL
                },
            )
            else -> simulated
        }
        val postProjected = postCombat.projectedState
        // Under [attacksReadTheRace] the board is read as it stands at our next untap: the
        // crack-back below is what a tapped attacker costs, and the evaluator's tapped discount and
        // untapped-only defence would charge the same missing blocker a second time.
        val atOurUntap = if (attacksReadTheRace) {
            attackerMap.keys.fold(postCombat) { acc, attacker ->
                if (acc.getEntity(attacker)?.has<TappedComponent>() == true &&
                    !postProjected.doesntUntapDuringUntapStep(attacker)
                ) {
                    acc.updateEntity(attacker) { it.without<TappedComponent>() }
                } else {
                    acc
                }
            }
        } else {
            postCombat
        }
        val baseScore = evaluator.evaluate(atOurUntap, postProjected, playerId)

        // Per-card advisor penalties, read off the pre-combat state (the attackers may
        // be dead by now). Deliberately not applied to the lethal alpha strike in
        // [chooseAttackers] — a kill beats any advisor preference.
        val advisorPenalty = attackPenaltyFor(state, state.projectedState, attackerMap.keys, playerId)

        // Estimate next-turn counter-attack: what damage can the opponent deal through our blocks?
        val myBlockers = postProjected.getBattlefieldControlledBy(playerId).filter { entityId ->
            postProjected.isCreature(entityId) &&
                postCombat.getEntity(entityId)?.has<TappedComponent>() != true
        }
        val myLife = postCombat.lifeTotal(playerId)
        val stackOnTramplers = crackBackStress != null
        val nextTurnDamage = incomingNextTurnDamage(
            postCombat, postProjected, playerId, myBlockers, stackOnTramplers = stackOnTramplers,
        ).let { onBoard ->
            // The creatures alone let us survive it; the abilities beside them may not. A swing
            // back one visible activation makes lethal is priced as lethal — a merely bigger one
            // is left to the board, so the opponent's options never make the AI timid short of
            // its life being on the line.
            if (onBoard >= myLife) return@let onBoard
            val stressed = crackBackStress?.worstCrackBack(state, postCombat, playerId, myLife) { answered ->
                val answeredProjected = answered.projectedState
                val blockers = myBlockers.filter { id ->
                    answeredProjected.isCreature(id) && answered.getEntity(id)?.has<TappedComponent>() != true
                }
                incomingNextTurnDamage(answered, answeredProjected, playerId, blockers, stackOnTramplers = true)
            }
            if (stressed != null && stressed >= myLife) stressed else onBoard
        }
        val lifeLost = if (priceCrackBackAsLife && crackBackWithoutChumps && nextTurnDamage < myLife) {
            val withoutChumps = incomingNextTurnDamage(
                postCombat, postProjected, playerId, myBlockers, chumpBlocks = false, stackOnTramplers = stackOnTramplers,
            )
            maxOf(nextTurnDamage, minOf(withoutChumps, myLife - 1))
        } else {
            nextTurnDamage
        }

        // Penalty for plans that hand the opponent a crack-back. See [priceCrackBackAsLife] for
        // why the flat version is a cliff and what replaces it.
        val crackBackPenalty = if (priceCrackBackAsLife) {
            val after = myLife - lifeLost
            -(LifeDifferential.lifeValue(myLife) - LifeDifferential.lifeValue(after)) * lifeWeight
        } else if (nextTurnDamage >= myLife) {
            -3.0
        } else {
            0.0
        }

        // Our next swing is worth nothing to a plan whose crack-back kills us before it comes.
        val followUp = if (attacksReadTheRace && nextTurnDamage < myLife) {
            followUpValue(postCombat, postProjected, playerId)
        } else {
            0.0
        }

        val pressure = if (pressWhenAhead && nextTurnDamage < myLife) {
            pressureValue(state, atOurUntap, playerId)
        } else {
            0.0
        }

        return baseScore + crackBackPenalty + followUp + pressure - advisorPenalty
    }

    /**
     * Time as a resource: what the damage this combat deals is worth to the side that is ahead.
     *
     * A static leaf cannot see time. An attack leaf and a pass leaf are scored at the same moment,
     * so the turn a pass hands the opponent — a draw step, a land drop, one more look for the answer
     * — costs nothing in either, and the only thing an attack has to show for itself is life, which
     * [com.wingedsheep.ai.engine.evaluation.LifeDifferential] prices on a *defender's* curve: half a
     * point each above 15. Six damage into a player on 20 was worth 3.5, less than the 2/2 a block
     * took to deal it, so a team of four Bears facing one Hill Giant sat at home — and every turn it
     * sat there was a turn for the Giant's controller to draw out of it.
     *
     * For the player who is ahead, damage is progress toward the only finish the lead buys, and it
     * is worth what it brings the kill forward. That is the race's own unit: the share of the
     * opponent's life removed is urgency already cashed, priced on the evaluator's race scale
     * ([ThreatAssessment.raceLeadValue] at [raceWeight], the scale [followUpValue] uses). The credit
     * is scaled by [BoardRole.dominance] read off the board the combat **leaves** — so trading down
     * is paid for only while what remains still dominates, and a trade that gives the lead away
     * pays itself nothing. Behind or at parity the dominance is not positive and the term is zero:
     * the side that is behind keeps scoring attacks exactly as before.
     *
     * Bounded by the caller: no credit at all for a plan whose crack-back is lethal, and every other
     * plan still pays its crack-back as life.
     */
    private fun pressureValue(state: GameState, after: GameState, playerId: EntityId): Double {
        val afterProjected = after.projectedState
        return state.getOpponents(playerId).sumOf { opponent ->
            val lifeBefore = state.lifeTotal(opponent)
            val dealt = lifeBefore - after.lifeTotal(opponent)
            if (lifeBefore <= 0 || dealt <= 0) return@sumOf 0.0
            val dominance = BoardRole.dominance(after, afterProjected, playerId, opponent)
            if (dominance <= 0.0) return@sumOf 0.0
            val cashed = (dealt.toDouble() / lifeBefore).coerceAtMost(1.0)
            dominance * ThreatAssessment.raceLeadValue(cashed) * raceWeight
        }
    }

    /**
     * Under [pressWhenAhead], the plan that sends every legal attacker at its default defender — a
     * start for local search beside the heuristic seed.
     *
     * Local search climbs one attacker at a time, and a swarm is the plan it cannot climb to: a lone
     * 2/2 into a 3/3 is the worst plan on the board, so from an empty seed the first step never
     * comes, though four of them together get six damage through. The seed proposes a swarm only
     * when attackers outnumber blockers; this offers the search the all-in plan whatever the count,
     * and the search removes from it whatever does not pay. One extra simulation per decision.
     */
    private fun alphaStrikeStart(
        validAttackers: List<EntityId>,
        mandatoryAttackers: Set<EntityId>,
        defenderFor: (EntityId) -> EntityId?,
    ): Map<EntityId, EntityId>? {
        if (!pressWhenAhead) return null
        val all = validAttackers.mapNotNull { attacker -> defenderFor(attacker)?.let { attacker to it } }.toMap()
        return all.takeIf { it.size > 1 && it.keys.containsAll(mandatoryAttackers) }
    }

    /**
     * Our next swing, the counterpart of the crack-back: how fast the board left by this combat
     * kills the opponent, priced on the evaluator's race scale.
     *
     * The clock is in whole turns — the opponent's life over the damage our creatures push through
     * every creature they will have to block with, each of them free to chump — and enters as its
     * urgency, one over the turns, the same reading [com.wingedsheep.ai.engine.evaluation.ThreatAssessment]
     * gives the race. The evaluator's own race term sums raw power, which saturates once it reaches
     * their life: six power into a 3/3 on 6 life and four power on 2 read as the same won race,
     * though one kills next turn and the other takes two. This is the term that tells them apart.
     * Only our side is priced here: theirs is the crack-back, already charged as life.
     */
    private fun followUpValue(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
        val ourAttackers = projected.getBattlefieldControlledBy(playerId).filter {
            projected.isCreature(it) && !projected.cantAttack(it) &&
                Keyword.DEFENDER.name !in projected.getKeywords(it) && untapsInTime(state, projected, it)
        }
        if (ourAttackers.isEmpty()) return 0.0
        val urgency = state.getOpponents(playerId).maxOfOrNull { opponent ->
            val life = state.lifeTotal(opponent)
            if (life <= 0) return@maxOfOrNull 1.0
            val blockers = projected.getBattlefieldControlledBy(opponent).filter {
                projected.isCreature(it) && !projected.cantBlock(it) && untapsInTime(state, projected, it)
            }
            val through = CombatMath.calculateDamageThroughOptimalBlocking(state, projected, ourAttackers, blockers)
            if (through <= 0) 0.0 else 1.0 / ((life + through - 1) / through)
        } ?: 0.0
        return ThreatAssessment.raceLeadValue(urgency) * raceWeight
    }

    /**
     * Local search for attack plan: starting from heuristic seed, try add/remove
     * mutations and simulate each. Keep improvements until convergence.
     *
     * Also tries the "no attack" baseline to ensure attacking is better than passing.
     */
    private fun improveAttackViaLocalSearch(
        state: GameState,
        playerId: EntityId,
        opponentId: EntityId,
        validAttackers: List<EntityId>,
        mandatoryAttackers: Set<EntityId>,
        attackerMap: MutableMap<EntityId, EntityId>,
        budget: DecisionBudget,
        trace: CombatPlanTrace? = null,
        /** The defender an attacker takes when the search adds it; null when it can attack nobody. */
        defenderFor: (EntityId) -> EntityId? = { opponentId },
        /**
         * Every defender the search may give an attacker — [defenderFor]'s, plus opposing
         * planeswalkers under [attackPlaneswalkers]. With more than one, an added attacker tries
         * each and an attacking one may be retargeted.
         */
        defendersFor: (EntityId) -> List<EntityId> = { listOfNotNull(defenderFor(it)) },
    ) {
        val deadline = budget.combatDeadlineNanos
        // Baseline: use current board evaluation. Attack plans must beat this. When the empty
        // declaration can't be simulated, the board as it stands is scored the same way — with
        // the crack-back — so the plans are measured against a like-for-like baseline.
        val noAttackScore = evaluateAttackPlan(state, playerId, opponentId, emptyMap())
            ?: scoreAttackOutcome(state, state, playerId, emptyMap(), settleExpiring = false)
        trace?.recordAttack(emptyMap(), noAttackScore)
        var currentScore = if (attackerMap.isEmpty()) {
            noAttackScore
        } else {
            // A seed that can't be simulated is unscored, not proven: a lethal one never reaches
            // here, so search from no attack rather than submit it on the heuristic's word.
            evaluateAttackPlan(state, playerId, opponentId, attackerMap) ?: run {
                if (mandatoryAttackers.isNotEmpty()) return
                attackerMap.clear()
                noAttackScore
            }
        }
        trace?.recordAttack(attackerMap, currentScore)

        // If seed plan is worse than not attacking, start from empty
        if (currentScore < noAttackScore && mandatoryAttackers.isEmpty()) {
            attackerMap.clear()
            currentScore = noAttackScore
        }

        // The all-in plan, a start the one-attacker-at-a-time climb cannot reach on its own.
        alphaStrikeStart(validAttackers, mandatoryAttackers, defenderFor)
            ?.takeIf { it != attackerMap && System.nanoTime() <= deadline }
            ?.let { alpha ->
                val score = evaluateAttackPlan(state, playerId, opponentId, alpha) ?: return@let
                trace?.recordAttack(alpha, score)
                if (score > currentScore) {
                    attackerMap.clear()
                    attackerMap.putAll(alpha)
                    currentScore = score
                }
            }

        val maxIterations = budget.allowances.attackSearchIterations
        for (iteration in 1..maxIterations) {
            if (System.nanoTime() > deadline) break
            var bestMutation: Map<EntityId, EntityId>? = null
            var bestScore = currentScore

            // Mutation 1: Add each non-attacking creature, at each defender it may take
            for (attacker in validAttackers) {
                if (System.nanoTime() > deadline) break
                if (attacker in attackerMap) continue
                for (defender in defendersFor(attacker)) {
                    if (System.nanoTime() > deadline) break
                    val mutation = attackerMap.toMutableMap()
                    mutation[attacker] = defender
                    val score = evaluateAttackPlan(state, playerId, opponentId, mutation) ?: continue
                    trace?.recordAttack(mutation, score)
                    if (score > bestScore) {
                        bestScore = score
                        bestMutation = mutation
                    }
                }
            }

            // Mutation 1b: Retarget each attacker at another defender it may take — the player
            // instead of a walker, a walker instead of the player, or one walker for another.
            for ((attacker, current) in attackerMap.toList()) {
                if (System.nanoTime() > deadline) break
                for (defender in defendersFor(attacker)) {
                    if (defender == current) continue
                    if (System.nanoTime() > deadline) break
                    val mutation = attackerMap.toMutableMap()
                    mutation[attacker] = defender
                    val score = evaluateAttackPlan(state, playerId, opponentId, mutation) ?: continue
                    trace?.recordAttack(mutation, score)
                    if (score > bestScore) {
                        bestScore = score
                        bestMutation = mutation
                    }
                }
            }

            // Mutation 2: Remove each non-mandatory attacker
            for (attacker in attackerMap.keys.toList()) {
                if (System.nanoTime() > deadline) break
                if (attacker in mandatoryAttackers) continue
                val mutation = attackerMap.toMutableMap()
                mutation.remove(attacker)
                // For empty plan, use no-attack baseline score
                val score = if (mutation.isEmpty()) {
                    noAttackScore
                } else {
                    evaluateAttackPlan(state, playerId, opponentId, mutation) ?: continue
                }
                trace?.recordAttack(mutation, score)
                if (score > bestScore) {
                    bestScore = score
                    bestMutation = mutation
                }
            }

            if (bestMutation != null) {
                attackerMap.clear()
                attackerMap.putAll(bestMutation)
                currentScore = bestScore
            } else {
                break // converged
            }
        }
    }

    /** Return unassigned blockers that can legally block [attacker] (evasion + blocker restriction check). */
    private fun availableBlockersFor(
        state: GameState,
        projected: ProjectedState,
        attacker: EntityId,
        validBlockers: List<EntityId>,
        assignedBlockers: Set<EntityId>
    ): List<EntityId> {
        return validBlockers.filter { it !in assignedBlockers && canBlock(state, projected, attacker, it) }
    }

    /** Can [blocker] block [attacker] on its own terms — evasion, protection, restrictions. */
    private fun canBlock(state: GameState, projected: ProjectedState, attacker: EntityId, blocker: EntityId): Boolean {
        if (!CombatMath.canBeBlockedBy(state, projected, attacker, blocker, cardRegistry)) return false
        val rules = blockEvasionRules ?: return true
        val registry = cardRegistry ?: return true
        val blockingPlayer = projected.getController(blocker) ?: return true
        val ctx = BlockCheckContext(state, projected, attacker, blocker, blockingPlayer, registry)
        return rules.none { it.check(ctx) != null }
    }

    /**
     * Menace: a lone blocker on this attacker is not a block, it is an illegal
     * declaration. Every single-blocker pass has to skip it rather than leave it for
     * [fixMenaceAssignments] to strip, because stripping frees the blocker *after* the passes that
     * could have used it elsewhere have run.
     */
    private fun needsTwoBlockers(projected: ProjectedState, attacker: EntityId): Boolean =
        Keyword.MENACE.name in projected.getKeywords(attacker)

    /**
     * Block for our planeswalkers: while an attacker aimed at one would hurt it, add the block that
     * saves the most walker value net of the creatures it costs — [PlaneswalkerThreat.damageValue]
     * for the loyalty damage it stops (all of the walker when that damage would kill it), less the
     * blocker's value if the attacker kills it, plus the attacker's if the block kills that. A
     * blocker that survives costs nothing; a chump costs its body.
     *
     * So a valuable walker gets a chump or a trade, and a near-dead token is let go rather than
     * bought with a good creature — the token is worth a point or two, the body more. Runs on
     * blockers the heuristic left free, before the life passes: a plan that then lets lethal
     * through to us is rebuilt by the survival pass, which prices damage to the player only.
     * Both the AI's own blocks and the opponent model the attack search simulates against start
     * here, so an attack at a defended walker is no longer scored as if nobody would stand in front
     * of it.
     */
    private fun protectPlaneswalkers(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        attackers: List<EntityId>,
        validBlockers: List<EntityId>,
        assignedBlockers: MutableSet<EntityId>,
        blockerMap: MutableMap<EntityId, List<EntityId>>,
    ) {
        val byWalker = attackers.groupBy { defenderOf(state, it) }.filterKeys { walker ->
            walker != null && projected.isPlaneswalker(walker) && projected.getController(walker) == playerId
        }
        for ((walker, walkerAttackers) in byWalker) {
            walker ?: continue
            repeat(walkerAttackers.size) {
                val damage = damageThrough(projected, walkerAttackers, blockerMap)
                if (damage <= 0) return@repeat
                val lostNow = PlaneswalkerThreat.damageValue(state, walker, damage, cardRegistry, walkerEngines)
                val blocked = blockerMap.values.flatten().toSet()
                var best: Triple<EntityId, List<EntityId>, Double>? = null
                for (attacker in walkerAttackers) {
                    if (attacker in blocked || (projected.getPower(attacker) ?: 0) <= 0) continue
                    val needed = if (needsTwoBlockers(projected, attacker)) 2 else 1
                    val bodies = availableBlockersFor(state, projected, attacker, validBlockers, assignedBlockers)
                        .sortedBy { blockCost(state, projected, attacker, listOf(it)) }
                        .take(needed)
                    if (bodies.size < needed) continue
                    val after = blockerMap.toMutableMap().apply { bodies.forEach { put(it, listOf(attacker)) } }
                    val saved = lostNow - PlaneswalkerThreat.damageValue(
                        state, walker, damageThrough(projected, walkerAttackers, after), cardRegistry, walkerEngines,
                    )
                    val net = saved - blockCost(state, projected, attacker, bodies)
                    if (net > 0.0 && (best == null || net > best.third)) best = Triple(attacker, bodies, net)
                }
                val (attacker, bodies, _) = best ?: return@repeat
                bodies.forEach { blockerMap[it] = listOf(attacker); assignedBlockers.add(it) }
            }
        }
    }

    /**
     * What standing [bodies] in front of [attacker] costs in creatures: each body the attacker kills,
     * less the attacker when the bodies kill it. Board units, like [PlaneswalkerThreat.damageValue].
     */
    private fun blockCost(state: GameState, projected: ProjectedState, attacker: EntityId, bodies: List<EntityId>): Double {
        val lost = bodies.filter { CombatMath.wouldKillInCombat(state, projected, attacker, it) }
            .sumOf { CombatMath.creatureValue(state, projected, it).coerceAtLeast(MIN_BODY_VALUE) }
        val dealing = bodies.filter { CombatMath.blockerDealsDamage(state, projected, attacker, it) }
        val kills = Keyword.INDESTRUCTIBLE.name !in projected.getKeywords(attacker) && (
            dealing.any { CombatMath.wouldKillInCombat(state, projected, it, attacker) } ||
                (dealing.isNotEmpty() && dealing.sumOf { projected.getPower(it) ?: 0 } >= (projected.getToughness(attacker) ?: 0))
            )
        return lost - if (kills) CombatMath.creatureValue(state, projected, attacker) else 0.0
    }

    /** Damage [attackers] deal past the blocks in [blockerMap], trample overflow included. */
    private fun damageThrough(
        projected: ProjectedState,
        attackers: List<EntityId>,
        blockerMap: Map<EntityId, List<EntityId>>,
    ): Int {
        val blocked = blockerMap.values.flatten().toSet()
        return attackers.sumOf { attacker ->
            val power = projected.getPower(attacker) ?: 0
            when {
                power <= 0 -> 0
                attacker !in blocked -> power
                Keyword.TRAMPLE.name !in projected.getKeywords(attacker) -> 0
                else -> {
                    val blockers = blockerMap.filterValues { attacker in it }.keys
                    val absorbed = if (Keyword.DEATHTOUCH.name in projected.getKeywords(attacker)) blockers.size
                    else blockers.sumOf { projected.getToughness(it) ?: 0 }
                    (power - absorbed).coerceAtLeast(0)
                }
            }
        }
    }

    /** What [attacker] is attacking — a player, planeswalker or battle — or null when it is not attacking. */
    private fun defenderOf(state: GameState, attacker: EntityId): EntityId? =
        state.getEntity(attacker)?.get<com.wingedsheep.engine.state.components.combat.AttackingComponent>()?.defenderId

    /** Whether [attacker] is attacking a player, so its unblocked damage is life lost. */
    private fun attacksAPlayer(state: GameState, attacker: EntityId): Boolean =
        defenderOf(state, attacker)?.let { it in state.turnOrder } ?: true

    /** Whether a blocker assigned to [blocked] is standing in front of an attacker aimed at a planeswalker. */
    private fun blocksForAWalker(state: GameState, blocked: List<EntityId>?): Boolean =
        blocked.orEmpty().any { attacker ->
            defenderOf(state, attacker)?.let { it !in state.turnOrder && state.projectedState.isPlaneswalker(it) } == true
        }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun getAttackingCreatures(state: GameState): List<EntityId> {
        return state.getBattlefield().filter { entityId ->
            state.getEntity(entityId)?.has<com.wingedsheep.engine.state.components.combat.AttackingComponent>() == true
        }
    }
}
