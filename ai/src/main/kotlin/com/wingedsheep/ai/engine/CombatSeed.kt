package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.evaluation.LifeDifferential
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.combat.CombatDefenders
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId

/**
 * The attack plan `CombatAdvisor` starts from, before it simulates anything.
 *
 * [CombatAdvisor.chooseAttackers] has always been two phases — a heuristic seed, then a local search
 * that mutates it one attacker at a time and scores each mutation with a full engine simulation.
 * Phase 7 needs the first phase without the second: a rollout's combat step must be decided in
 * microseconds, and anything that simulates *inside* a playout makes the playout quadratic.
 *
 * So the seed lives here and has two callers — `CombatAdvisor`, which local-searches from it, and
 * [com.wingedsheep.ai.engine.rollout.PlayoutPolicy], which plays it as-is. One implementation, so
 * the combat a rollout imagines is the combat the AI would actually seed.
 *
 * Blocking needs no equivalent: `CombatAdvisor.chooseBlockers(useSimulation = false)` is already the
 * seed-only path and reaches no simulator.
 */
object CombatSeed {

    /**
     * Which creatures to send in, before local search.
     *
     * Three passes, in order:
     * 1. **Lethal alpha strike** — if damage gets through even against optimal blocking, everything
     *    attacks and nothing else matters (in a pod under [minimalLethalInPods], only the fewest
     *    attackers that still kill — [minimalLethal]). Deliberately ignores [attackPenalty]: a `CardAdvisor`
     *    that would rather keep a wall home does not get a vote on the turn we win.
     * 2. **No-downside and clearly profitable attackers** — evasive, vigilant, indestructible,
     *    survives every blocker, profitable trades.
     * 3. **Excess attackers** — more bodies than they have blockers means the surplus gets through,
     *    minus one held back when the crack-back is close to lethal.
     *
     * @param attackPenalty per-creature discouragement from a `CardAdvisor`, summed by the caller.
     *   A discouraged creature is left out of the seed; `CombatAdvisor`'s local search can still add
     *   it back when it pays. Defaults to "no advisor has an opinion", which is every card today.
     * @param aimAtPlaneswalkers send attackers that will connect at opposing planeswalkers when
     *   the loyalty damage is worth more than the life — see [aimAtPlaneswalkers]. Off, every
     *   attacker goes at the primary seat, the behaviour before `AiProfile.attackPlaneswalkers`.
     * @param walkerEngines prices each walker as an engine, reading its abilities through this
     *   catalog ([PlaneswalkerThreat], `AiProfile.walkersPricedAsEngines`); null is the legacy reading.
     * @param minimalLethalInPods with more than one opponent left, shrink the lethal alpha strike to
     *   [minimalLethal] (`AiProfile.minimalLethalInPods`). 1v1 is unaffected.
     */
    fun attackers(
        state: GameState,
        projected: ProjectedState,
        legalAction: LegalAction,
        playerId: EntityId,
        cardRegistry: CardRegistry? = null,
        attackPenalty: (EntityId) -> Double = { 0.0 },
        aimAtPlaneswalkers: Boolean = false,
        walkerEngines: IntentCatalog? = null,
        minimalLethalInPods: Boolean = false,
    ): AttackSeed {
        val defendingPlayers = legalAction.validAttackTargets ?: emptyList()
        if (defendingPlayers.isEmpty()) {
            return AttackSeed(emptyMap(), null, lethal = false)
        }

        // CR 802.2a — who we attack is a choice, and `validAttackTargets` mixes players with
        // opponent planeswalkers. Aim at the *player* closest to dying that our creatures can
        // actually reach: in 1v1 that is the sole opponent, and in a pod it is the seat an alpha
        // strike can finish — never a seat behind Blazing Archon ("creatures can't attack you").
        val opponentId = primaryDefender(state, legalAction) ?: return AttackSeed(emptyMap(), null, lethal = false)
        // CR 508.1b–c — each attacker chooses its own defender. One that may not attack the primary
        // seat (Goblin Rock Sled vs a seat with no Mountain) goes at its own best legal defender;
        // one with none at all stays home rather than voiding the whole declaration.
        val defenderOf = (legalAction.validAttackers ?: emptyList())
            .mapNotNull { attacker -> defenderFor(state, legalAction, attacker, opponentId)?.let { attacker to it } }
            .toMap()
        val validAttackers = defenderOf.keys.toList()
        if (validAttackers.isEmpty()) return AttackSeed(emptyMap(), opponentId, lethal = false)

        val opponentLife = if (opponentId in state.turnOrder) state.lifeTotal(opponentId) else 20
        val opponentCreatures = CombatMath.getOpponentUntappedCreatures(state, projected, opponentId)
        val blockersBySeat = mutableMapOf(opponentId to opponentCreatures)
        // The untapped creatures that could block [attacker] — those of the seat it attacks.
        fun facing(attacker: EntityId): List<EntityId> {
            val seat = CombatDefenders.defendingPlayerOf(state, defenderOf.getValue(attacker))
            return blockersBySeat.getOrPut(seat) { CombatMath.getOpponentUntappedCreatures(state, projected, seat) }
        }
        val mandatory = (legalAction.mandatoryAttackers ?: emptyList()).filter { it in defenderOf }

        // ── Lethal check: alpha-strike if damage gets through even with optimal blocking ──
        // Only creatures that can attack the primary seat count toward killing it.
        val reachPrimary = validAttackers.filter { defenderOf[it] == opponentId }
        if (CombatMath.isLethalAttack(state, projected, reachPrimary, opponentCreatures, opponentLife)) {
            // In 1v1 the game ends, so everything attacks. In a pod the kill only needs enough to
            // finish the dying seat; the rest stays home for the opponents still at the table.
            val alphaStrike = if (minimalLethalInPods && state.getOpponents(playerId).size > 1) {
                minimalLethal(state, projected, reachPrimary, opponentCreatures, opponentLife, mandatory.toSet())
                    .associateWith { defenderOf.getValue(it) }
            } else {
                defenderOf
            }
            val affordable = CombatTaxBudget.affordableAttack(
                state, projected, playerId, cardRegistry, alphaStrike, mandatory.toSet()
            )
            // A trimmed alpha strike is not the attack the lethal check approved, so it goes back to
            // being an ordinary plan for the caller's local search to improve on.
            return AttackSeed(affordable, opponentId, lethal = affordable.size == alphaStrike.size)
        }

        // ── Heuristic seed: no-downside and clearly profitable attackers ──
        // Creatures an advisor discourages from attacking are left out of the seed;
        // the local search below can still add them back when it pays.
        val discouraged = validAttackers.filter { entityId ->
            entityId !in mandatory && attackPenalty(entityId) > 0.0
        }.toSet()

        val seedMap = mutableMapOf<EntityId, EntityId>()
        for (entityId in mandatory) {
            seedMap[entityId] = defenderOf.getValue(entityId)
        }
        for (entityId in validAttackers) {
            if (entityId in seedMap || entityId in discouraged) continue
            val power = projected.getPower(entityId) ?: 0
            if (power <= 0) continue

            val toughness = projected.getToughness(entityId) ?: 0
            val keywords = projected.getKeywords(entityId)
            val isEvasive = CombatMath.isEvasive(state, projected, entityId, facing(entityId))

            // Always attack: no risk
            if (Keyword.VIGILANCE.name in keywords ||
                Keyword.INDESTRUCTIBLE.name in keywords ||
                facing(entityId).isEmpty() ||
                isEvasive
            ) {
                seedMap[entityId] = defenderOf.getValue(entityId)
                continue
            }

            // Attack if we survive any single blocker
            val survivesAllBlockers = facing(entityId).all { blockerId ->
                val bPower = projected.getPower(blockerId) ?: 0
                toughness > bPower
            }
            if (survivesAllBlockers) {
                seedMap[entityId] = defenderOf.getValue(entityId)
                continue
            }

            // Attack if trample and we'd deal significant damage through
            if (Keyword.TRAMPLE.name in keywords) {
                val bestBlockerToughness = facing(entityId)
                    .filter { CombatMath.canBeBlockedBy(state, projected, entityId, it) }
                    .maxOfOrNull { projected.getToughness(it) ?: 0 } ?: 0
                val damageThrough = (power - bestBlockerToughness).coerceAtLeast(0)
                if (damageThrough > 0 &&
                    toughness > (facing(entityId).minOfOrNull { projected.getPower(it) ?: 0 } ?: 0)
                ) {
                    seedMap[entityId] = defenderOf.getValue(entityId)
                    continue
                }
            }

            // Attack if every blocking option is worse for the opponent than taking the damage
            // (e.g., a 3/3 into a board of 2/2s — opponent must take 3 or trade down)
            // Accept even trades when we have more creatures — trading favors the larger army
            if (CombatMath.isProfitableAttack(
                    state, projected, entityId, facing(entityId), cardRegistry,
                    myCreatureCount = validAttackers.size,
                    opponentCreatureCount = facing(entityId).size
                )
            ) {
                seedMap[entityId] = defenderOf.getValue(entityId)
                continue
            }
        }

        // Attack if we have more attackers than they have blockers (excess gets through)
        if (seedMap.size < validAttackers.size) {
            val unblockedSlots = validAttackers.size - opponentCreatures.size
            if (unblockedSlots > 0) {
                // Sort remaining by value ascending — send cheapest creatures first,
                // hold back the most valuable ones in case we need a blocker
                val remaining = validAttackers
                    .filter { it !in seedMap && it !in discouraged && (projected.getPower(it) ?: 0) > 0 }
                    .sortedBy { CombatMath.creatureValue(state, projected, it) }

                // Estimate opponent's crack-back damage through our optimal blocking.
                // Accounts for evasion — flying creatures we can't block deal guaranteed damage.
                val myLife = state.lifeTotal(playerId)
                val allOpponentAttackers = projected.getBattlefieldControlledBy(opponentId).filter {
                    projected.isCreature(it) && Keyword.DEFENDER.name !in projected.getKeywords(it)
                }
                val myPotentialBlockers = validAttackers.filter { it !in seedMap && !projected.cantBlock(it) }
                val crackBackDamage = if (allOpponentAttackers.isNotEmpty()) {
                    CombatMath.calculateDamageThroughOptimalBlocking(
                        state, projected, allOpponentAttackers, myPotentialBlockers
                    )
                } else 0

                // If opponent threatens near-lethal damage next turn, hold back our best blocker.
                // Prefer deathtouch creatures as hold-backs (they trade with anything).
                val holdBack = if (crackBackDamage > 0 && myLife <= crackBackDamage * 1.5) {
                    remaining.filterNot { projected.cantBlock(it) }.maxByOrNull { entityId ->
                        val keywords = projected.getKeywords(entityId)
                        val hasDeathtouch = Keyword.DEATHTOUCH.name in keywords
                        val toughness = projected.getToughness(entityId) ?: 0
                        if (hasDeathtouch) 1000 + toughness else toughness
                    }
                } else null

                for (entityId in remaining) {
                    if (entityId != holdBack) {
                        seedMap[entityId] = defenderOf.getValue(entityId)
                    }
                }
            }
        }

        if (aimAtPlaneswalkers) {
            aimAtPlaneswalkers(state, projected, legalAction, cardRegistry, seedMap, walkerEngines)
        }

        // A plan whose attack tax we cannot pay is one we would be asked to pay for and could only
        // decline, ending up back at this same choice — see [CombatTaxBudget].
        return AttackSeed(
            CombatTaxBudget.affordableAttack(
                state, projected, playerId, cardRegistry, seedMap, mandatory.toSet()
            ),
            opponentId,
            lethal = false,
        )
    }

    /**
     * The fewest attackers out of [lethal] that still kill the dying seat through optimal blocking
     * ([CombatMath.isLethalAttack]), for a pod, where whatever attacks is not home for the next
     * opponent's swing.
     *
     * Only an attacker that would actually be home to block is worth holding back: one that can
     * block and would be tapped by attacking (no vigilance). Those are the drop candidates, the
     * non-evasive ones first, then by toughness and power; every other attacker, and [mandatory]
     * ones, stay in. A drop is kept only while the attack survives one piece of interaction — it
     * is still lethal with any single attacker removed (a removal spell, a flash blocker) — so the
     * trim never cuts the kill down to exact lethal. When the full attack does not even have that
     * margin, the trim falls back to plain lethal, as before. Non-mandatory attackers that cannot
     * reach the dying seat are left out — they are not part of the kill.
     */
    internal fun minimalLethal(
        state: GameState,
        projected: ProjectedState,
        lethal: List<EntityId>,
        blockers: List<EntityId>,
        life: Int,
        mandatory: Set<EntityId>,
    ): List<EntityId> {
        fun isLethal(attack: List<EntityId>) = CombatMath.isLethalAttack(state, projected, attack, blockers, life)
        fun survivesOneAnswer(attack: List<EntityId>) =
            attack.isNotEmpty() && attack.all { answered -> isLethal(attack - answered) }
        val stillKills: (List<EntityId>) -> Boolean =
            if (survivesOneAnswer(lethal)) ::survivesOneAnswer else ::isLethal

        val keep = lethal.toMutableList()
        val dropOrder = lethal
            .filter {
                it !in mandatory && !projected.cantBlock(it) &&
                    Keyword.VIGILANCE.name !in projected.getKeywords(it)
            }
            .sortedWith(
                compareBy<EntityId> { CombatMath.isEvasive(state, projected, it, blockers) }
                    .thenByDescending { projected.getToughness(it) ?: 0 }
                    .thenByDescending { projected.getPower(it) ?: 0 }
            )
        for (attacker in dropOrder) {
            keep.remove(attacker)
            if (!stillKills(keep)) keep.add(attacker)
        }
        return keep + mandatory.filter { it !in keep }
    }

    /**
     * The opposing planeswalkers [attackerId] may legally attack (CR 508.1b), in the engine's order.
     * Battles are left out: a battle is attacked to defeat it, not to stop an engine.
     */
    fun planeswalkerTargetsFor(
        state: GameState,
        projected: ProjectedState,
        legalAction: LegalAction,
        attackerId: EntityId,
    ): List<EntityId> =
        legalAction.attackTargetsFor(attackerId).filter { it !in state.turnOrder && projected.isPlaneswalker(it) }

    /**
     * Re-aim the attackers in [plan] that will connect at the opposing planeswalker their damage
     * hurts most, when that beats the life it would have taken.
     *
     * An opposing walker is an ongoing value engine ([PlaneswalkerThreat]); a hit that kills it, or
     * knocks its biggest ability out of reach, is usually worth far more than the same points off a
     * healthy life total. Each attacker no untapped creature of the walker's seat can block is a
     * candidate: its damage at the player is priced by the evaluator's life curve
     * ([LifeDifferential.lifeValue], which grows steeply near zero — so a race that is nearly won
     * keeps its damage), its damage at each walker it may attack by [PlaneswalkerThreat.damageValue]
     * at the default board-presence weight. The move with the largest net gain is made first, until
     * none is left: so a small attacker that exactly finishes a walker goes at it while the big one
     * stays on the player. Overkill is not credited: damage past a walker's remaining loyalty is
     * wasted.
     *
     * Attackers that can be blocked keep their defender. Whether a blocker would rather protect the
     * walker than take the hit is a question for a simulation, which `CombatAdvisor`'s local search
     * asks by retargeting them one at a time.
     */
    internal fun aimAtPlaneswalkers(
        state: GameState,
        projected: ProjectedState,
        legalAction: LegalAction,
        cardRegistry: CardRegistry?,
        plan: MutableMap<EntityId, EntityId>,
        walkerEngines: IntentCatalog? = null,
    ) {
        val blockersBySeat = mutableMapOf<EntityId, List<EntityId>>()
        fun blockersOf(seat: EntityId) =
            blockersBySeat.getOrPut(seat) { CombatMath.getOpponentUntappedCreatures(state, projected, seat) }
        fun connects(attacker: EntityId, defender: EntityId) =
            CombatMath.isEvasive(state, projected, attacker, blockersOf(CombatDefenders.defendingPlayerOf(state, defender)))

        val candidates = plan.entries
            .filter { (attacker, defender) ->
                defender in state.turnOrder &&
                    (projected.getPower(attacker) ?: 0) > 0 &&
                    connects(attacker, defender) &&
                    planeswalkerTargetsFor(state, projected, legalAction, attacker).isNotEmpty()
            }
            .map { it.key }
            .sortedByDescending { projected.getPower(it) ?: 0 }
        if (candidates.isEmpty()) return

        // Damage each player is still taking from the connecting attackers, and loyalty each
        // walker has left after the ones already re-aimed at it.
        val faceDamage = mutableMapOf<EntityId, Int>()
        for (attacker in candidates) {
            faceDamage.merge(plan.getValue(attacker), projected.getPower(attacker) ?: 0, Int::plus)
        }
        val loyaltyLeft = mutableMapOf<EntityId, Int>()
        val unassigned = candidates.toMutableList()

        // Best single move first, until no move beats leaving the damage at the player: the pair
        // whose loyalty gain most exceeds the life it gives up. Picking by net gain rather than by
        // attacker keeps the biggest attacker on the player when a smaller one can finish the
        // walker on its own.
        while (unassigned.isNotEmpty()) {
            var bestMove: Triple<EntityId, EntityId, Double>? = null
            for (attacker in unassigned) {
                val power = projected.getPower(attacker) ?: 0
                val player = plan.getValue(attacker)
                val life = state.lifeTotal(player)
                val damageAtPlayer = faceDamage.getValue(player)
                val faceGain = LifeDifferential.lifeValue(life - (damageAtPlayer - power)) -
                    LifeDifferential.lifeValue(life - damageAtPlayer)
                for (walker in planeswalkerTargetsFor(state, projected, legalAction, attacker)) {
                    if (!connects(attacker, walker)) continue
                    val loyalty = PlaneswalkerThreat.loyalty(state, walker)
                    val left = loyaltyLeft.getOrElse(walker) { loyalty }
                    if (left <= 0) continue
                    // What attackers re-aimed at it earlier already took is not this one's to claim.
                    val dealt = loyalty - left
                    val gain = BOARD_PRESENCE_WEIGHT * (
                        PlaneswalkerThreat.damageValue(state, walker, dealt + minOf(power, left), cardRegistry, walkerEngines) -
                            PlaneswalkerThreat.damageValue(state, walker, dealt, cardRegistry, walkerEngines)
                        )
                    val net = gain - faceGain
                    if (net > 0.0 && (bestMove == null || net > bestMove.third)) {
                        bestMove = Triple(attacker, walker, net)
                    }
                }
            }
            val (attacker, walker) = bestMove ?: break
            val power = projected.getPower(attacker) ?: 0
            val player = plan.getValue(attacker)
            faceDamage[player] = faceDamage.getValue(player) - power
            loyaltyLeft[walker] = loyaltyLeft.getOrElse(walker) { PlaneswalkerThreat.loyalty(state, walker) } - power
            plan[attacker] = walker
            unassigned.remove(attacker)
        }
    }

    /** The default evaluator's board-presence weight over its life weight (`EvaluationWeights`). */
    private const val BOARD_PRESENCE_WEIGHT = 1.5

    /**
     * The seat a plan aims at: the lowest-life opponent *player* at least one valid attacker may
     * legally attack (per [LegalAction.attackTargetsFor]), falling back to any listed target when
     * no player is reachable (only planeswalkers / battles are). Null when nothing can be attacked.
     */
    fun primaryDefender(state: GameState, legalAction: LegalAction): EntityId? {
        val targets = legalAction.validAttackTargets ?: return null
        val attackers = legalAction.validAttackers ?: emptyList()
        val reachable = targets.filter { target -> attackers.any { target in legalAction.attackTargetsFor(it) } }
        return reachable.filter { it in state.turnOrder }.minByOrNull { state.lifeTotal(it) }
            ?: reachable.firstOrNull()
            ?: targets.firstOrNull()
    }

    /**
     * Where [attackerId] attacks when the plan aims at [primary]: [primary] itself when the engine
     * lets this creature attack it, otherwise the lowest-life player it may attack, otherwise the
     * first permanent it may attack. Null when it has no legal defender at all (CR 508.1b–c: a
     * per-defender restriction such as Goblin Rock Sled's or Blazing Archon's binds one creature
     * and one defender, never the rest of the attack).
     */
    fun defenderFor(state: GameState, legalAction: LegalAction, attackerId: EntityId, primary: EntityId?): EntityId? {
        val legal = legalAction.attackTargetsFor(attackerId)
        if (primary != null && primary in legal) return primary
        return legal.filter { it in state.turnOrder }.minByOrNull { state.lifeTotal(it) } ?: legal.firstOrNull()
    }

    /**
     * Last line of defence before a plan is submitted: every attacker whose chosen defender the
     * engine doesn't list for it is moved to [defenderFor] — or dropped when it has none — so one
     * restricted creature can never get the whole declaration rejected.
     */
    fun legalize(
        state: GameState,
        legalAction: LegalAction,
        plan: Map<EntityId, EntityId>,
        primary: EntityId?,
    ): Map<EntityId, EntityId> {
        if (legalAction.attackTargetsByAttacker.isNullOrEmpty() && plan.values.all { it in legalAction.validAttackTargets.orEmpty() }) {
            return plan
        }
        val valid = legalAction.validAttackers.orEmpty().toSet()
        return plan.mapNotNull { (attacker, defender) ->
            when {
                attacker !in valid -> null
                defender in legalAction.attackTargetsFor(attacker) -> attacker to defender
                else -> defenderFor(state, legalAction, attacker, primary)?.let { attacker to it }
            }
        }.toMap()
    }

    /**
     * The seed plan, plus the two things `CombatAdvisor`'s local search needs to know about it.
     *
     * @property attackers creature → the player or planeswalker it attacks.
     * @property defenderId the seat the seed aims at, or null when nothing can legally be attacked.
     * @property lethal whether this is the alpha strike from the lethal check. A local search over
     *   a winning attack can only make it worse, so `CombatAdvisor` skips it.
     */
    data class AttackSeed(
        val attackers: Map<EntityId, EntityId>,
        val defenderId: EntityId?,
        val lethal: Boolean,
    )
}
