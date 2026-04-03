package com.wingedsheep.engine.ai

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWithKeyword
import com.wingedsheep.sdk.scripting.CantBlockCreaturesWithGreaterPower
import com.wingedsheep.sdk.scripting.StaticTarget

/**
 * Pure utility functions for combat math used by CombatAdvisor.
 *
 * All functions are stateless — they read from GameState/ProjectedState
 * and return computed values without side effects.
 */
object CombatMath {

    // ── Evasion & Blocking Checks ───────────────────────────────────────

    /**
     * Returns true if [blocker] can legally block [attacker] based on evasion keywords.
     * This is a heuristic check covering the most common evasion abilities —
     * it doesn't replicate the full BlockEvasionRules pipeline.
     */
    fun canBeBlockedBy(
        state: GameState,
        projected: ProjectedState,
        attacker: EntityId,
        blocker: EntityId,
        cardRegistry: CardRegistry? = null
    ): Boolean {
        val aKeywords = projected.getKeywords(attacker)
        val bKeywords = projected.getKeywords(blocker)

        // Unblockable
        if (AbilityFlag.CANT_BE_BLOCKED.name in aKeywords) return false

        // Flying: only blocked by flying or reach
        if (Keyword.FLYING.name in aKeywords) {
            if (Keyword.FLYING.name !in bKeywords && Keyword.REACH.name !in bKeywords) return false
        }

        // Shadow: only blocked by shadow
        if (Keyword.SHADOW.name in aKeywords) {
            if (Keyword.SHADOW.name !in bKeywords) return false
        }

        // Horsemanship: only blocked by horsemanship
        if (Keyword.HORSEMANSHIP.name in aKeywords) {
            if (Keyword.HORSEMANSHIP.name !in bKeywords) return false
        }

        // Fear: only blocked by artifact or black creatures
        if (Keyword.FEAR.name in aKeywords) {
            val bColors = projected.getColors(blocker)
            val bTypes = projected.getTypes(blocker)
            if ("BLACK" !in bColors && "ARTIFACT" !in bTypes) return false
        }

        // Intimidate: only blocked by artifact or same-color creatures
        if (Keyword.INTIMIDATE.name in aKeywords) {
            val aColors = projected.getColors(attacker)
            val bColors = projected.getColors(blocker)
            val bTypes = projected.getTypes(blocker)
            val sharesColor = aColors.any { it in bColors }
            if (!sharesColor && "ARTIFACT" !in bTypes) return false
        }

        // Menace: requires 2+ blockers (can't be single-blocked)
        // We return true here — menace is handled at the assignment level
        // since a single canBeBlockedBy check can't express "needs 2 blockers"

        // Landwalk checks (approximate — check if opponent controls matching land)
        val attackerController = projected.getController(attacker)
        val blockerController = projected.getController(blocker)
        if (blockerController != null) {
            val defenderLands = projected.getBattlefieldControlledBy(blockerController)
            if (Keyword.SWAMPWALK.name in aKeywords && defenderLands.any { projected.hasSubtype(it, "Swamp") }) return false
            if (Keyword.FORESTWALK.name in aKeywords && defenderLands.any { projected.hasSubtype(it, "Forest") }) return false
            if (Keyword.ISLANDWALK.name in aKeywords && defenderLands.any { projected.hasSubtype(it, "Island") }) return false
            if (Keyword.MOUNTAINWALK.name in aKeywords && defenderLands.any { projected.hasSubtype(it, "Mountain") }) return false
            if (Keyword.PLAINSWALK.name in aKeywords && defenderLands.any { projected.hasSubtype(it, "Plains") }) return false
        }

        // Blocker-side restrictions (e.g., "can block only creatures with flying")
        if (cardRegistry != null) {
            val blockerCard = state.getEntity(blocker)?.get<CardComponent>()
            if (blockerCard != null) {
                val cardDef = cardRegistry.getCard(blockerCard.cardDefinitionId)
                if (cardDef != null) {
                    for (ability in cardDef.staticAbilities) {
                        when (ability) {
                            is CanOnlyBlockCreaturesWithKeyword -> {
                                if (ability.target == StaticTarget.SourceCreature &&
                                    !projected.hasKeyword(attacker, ability.keyword)) return false
                            }
                            is CantBlockCreaturesWithGreaterPower -> {
                                if (ability.target == StaticTarget.SourceCreature) {
                                    val aPower = projected.getPower(attacker) ?: 0
                                    val bPower = projected.getPower(blocker) ?: 0
                                    if (aPower > bPower) return false
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        return true
    }

    /**
     * Returns all [opponentBlockers] that can legally block [attacker].
     */
    fun getValidBlockersFor(
        state: GameState,
        projected: ProjectedState,
        attacker: EntityId,
        opponentBlockers: List<EntityId>
    ): List<EntityId> {
        return opponentBlockers.filter { canBeBlockedBy(state, projected, attacker, it) }
    }

    /**
     * Returns true if [attacker] cannot be blocked by any creature in [opponentBlockers].
     */
    fun isEvasive(
        state: GameState,
        projected: ProjectedState,
        attacker: EntityId,
        opponentBlockers: List<EntityId>
    ): Boolean {
        // Menace creatures need 2+ blockers, treat as evasive if opponent has ≤1 valid blocker
        val aKeywords = projected.getKeywords(attacker)
        val validBlockers = getValidBlockersFor(state, projected, attacker, opponentBlockers)
        if (Keyword.MENACE.name in aKeywords) return validBlockers.size <= 1
        return validBlockers.isEmpty()
    }

    // ── Combat Outcome Calculations ─────────────────────────────────────

    /**
     * Returns how much damage [attacker] would deal to the defending player
     * if blocked by [blocker]. Accounts for trample; returns 0 if no trample.
     */
    fun damageDealtThrough(
        projected: ProjectedState,
        attacker: EntityId,
        blocker: EntityId
    ): Int {
        val aPower = projected.getPower(attacker) ?: 0
        val aKeywords = projected.getKeywords(attacker)
        val bToughness = projected.getToughness(blocker) ?: 0

        if (Keyword.TRAMPLE.name !in aKeywords) return 0

        // With deathtouch + trample, only 1 damage needed per blocker
        val lethalDamage = if (Keyword.DEATHTOUCH.name in aKeywords) 1 else bToughness
        return (aPower - lethalDamage).coerceAtLeast(0)
    }

    /**
     * Returns how much damage is prevented by blocking [attacker] with [blocker].
     * For tramplers, this is only the blocker's toughness (or 1 with deathtouch).
     * For non-tramplers, this prevents all damage.
     */
    fun damagePreventedByBlock(
        projected: ProjectedState,
        attacker: EntityId,
        blocker: EntityId
    ): Int {
        val aPower = projected.getPower(attacker) ?: 0
        val aKeywords = projected.getKeywords(attacker)

        if (Keyword.TRAMPLE.name !in aKeywords) return aPower

        // With trample, only prevent damage equal to blocker toughness
        val bToughness = projected.getToughness(blocker) ?: 0
        // With deathtouch + trample, attacker only needs to assign 1 to blocker
        val lethalForBlocker = if (Keyword.DEATHTOUCH.name in aKeywords) 1 else bToughness
        return lethalForBlocker.coerceAtMost(aPower)
    }

    /**
     * Would [attacker] kill [defender] in regular combat?
     * Accounts for power, toughness, existing damage, deathtouch, and indestructible.
     */
    fun wouldKillInCombat(
        state: GameState,
        projected: ProjectedState,
        attacker: EntityId,
        defender: EntityId
    ): Boolean {
        val aPower = projected.getPower(attacker) ?: 0
        val aKeywords = projected.getKeywords(attacker)
        val dKeywords = projected.getKeywords(defender)

        if (Keyword.INDESTRUCTIBLE.name in dKeywords) return false
        if (aPower <= 0) return false
        if (Keyword.DEATHTOUCH.name in aKeywords) return true

        val dToughness = projected.getToughness(defender) ?: 0
        val existingDamage = state.getEntity(defender)?.get<DamageComponent>()?.amount ?: 0
        return aPower + existingDamage >= dToughness
    }

    /**
     * Does [blocker] survive being blocked by / blocking [attacker]?
     * Checks if the attacker's power is enough to kill the blocker.
     */
    fun survivesBlock(
        state: GameState,
        projected: ProjectedState,
        attacker: EntityId,
        blocker: EntityId
    ): Boolean {
        return !wouldKillInCombat(state, projected, attacker, blocker)
    }

    /**
     * Does [blocker] survive first-strike damage from [attacker]?
     * If the attacker doesn't have first/double strike, the blocker always survives first-strike.
     * If the attacker has first strike and would kill the blocker, the blocker dies before dealing damage.
     */
    fun survivesFirstStrike(
        state: GameState,
        projected: ProjectedState,
        attacker: EntityId,
        blocker: EntityId
    ): Boolean {
        val aKeywords = projected.getKeywords(attacker)
        val bKeywords = projected.getKeywords(blocker)

        val attackerHasFirstStrike = Keyword.FIRST_STRIKE.name in aKeywords || Keyword.DOUBLE_STRIKE.name in aKeywords
        if (!attackerHasFirstStrike) return true

        // If blocker also has first/double strike, damage is simultaneous
        val blockerHasFirstStrike = Keyword.FIRST_STRIKE.name in bKeywords || Keyword.DOUBLE_STRIKE.name in bKeywords
        if (blockerHasFirstStrike) return true

        // Attacker deals first-strike damage first — does it kill the blocker?
        return !wouldKillInCombat(state, projected, attacker, blocker)
    }

    /**
     * Can [blocker] actually deal damage to [attacker] in combat?
     * Returns false if the blocker dies to first strike before dealing damage.
     */
    fun blockerDealsDamage(
        state: GameState,
        projected: ProjectedState,
        attacker: EntityId,
        blocker: EntityId
    ): Boolean {
        return survivesFirstStrike(state, projected, attacker, blocker)
    }

    /**
     * Effective incoming damage from [attacker], accounting for lifelink.
     * Lifelink damage is worth double: you lose life AND opponent gains life.
     */
    fun effectiveDamage(projected: ProjectedState, attacker: EntityId): Int {
        val power = projected.getPower(attacker) ?: 0
        val keywords = projected.getKeywords(attacker)
        return if (Keyword.LIFELINK.name in keywords) power * 2 else power
    }

    /**
     * Effective damage prevented by blocking [attacker] with [blocker], accounting for lifelink.
     */
    fun effectiveDamagePrevented(projected: ProjectedState, attacker: EntityId, blocker: EntityId): Int {
        val prevented = damagePreventedByBlock(projected, attacker, blocker)
        val keywords = projected.getKeywords(attacker)
        return if (Keyword.LIFELINK.name in keywords) prevented * 2 else prevented
    }

    // ── Lethal Analysis ─────────────────────────────────────────────────

    /**
     * Calculate guaranteed evasive damage — power from creatures that cannot be blocked.
     */
    fun calculateEvasiveDamage(
        state: GameState,
        projected: ProjectedState,
        attackers: List<EntityId>,
        opponentBlockers: List<EntityId>
    ): Int {
        return attackers
            .filter { isEvasive(state, projected, it, opponentBlockers) }
            .sumOf { (projected.getPower(it) ?: 0).coerceAtLeast(0) }
    }

    /**
     * Simulate optimal blocking by the opponent and return how much damage gets through.
     * Uses a greedy assignment: opponent blocks the highest-power non-evasive attackers first
     * using their highest-toughness blockers (to prevent trample overflow).
     */
    fun calculateDamageThroughOptimalBlocking(
        state: GameState,
        projected: ProjectedState,
        attackers: List<EntityId>,
        opponentBlockers: List<EntityId>
    ): Int {
        var totalDamage = 0
        val usedBlockers = mutableSetOf<EntityId>()

        // Separate evasive from blockable
        val evasive = mutableListOf<EntityId>()
        val blockable = mutableListOf<EntityId>()

        for (attacker in attackers) {
            if (isEvasive(state, projected, attacker, opponentBlockers)) {
                evasive.add(attacker)
            } else {
                blockable.add(attacker)
            }
        }

        // Evasive damage goes through unimpeded
        totalDamage += evasive.sumOf { (projected.getPower(it) ?: 0).coerceAtLeast(0) }

        // For blockable attackers: opponent assigns blockers optimally to minimize damage
        // Sort blockable attackers by power descending (opponent blocks biggest threats first)
        val sortedBlockable = blockable.sortedByDescending { projected.getPower(it) ?: 0 }

        for (attacker in sortedBlockable) {
            val aPower = projected.getPower(attacker) ?: 0
            if (aPower <= 0) continue

            val aKeywords = projected.getKeywords(attacker)
            val hasTrample = Keyword.TRAMPLE.name in aKeywords

            // Find best blocker: for tramplers, use highest-toughness blocker to absorb most damage
            // For non-tramplers, any valid blocker prevents all damage — use cheapest
            val validBlockers = getValidBlockersFor(state, projected, attacker, opponentBlockers)
                .filter { it !in usedBlockers }

            // Handle menace: needs 2 valid blockers
            if (Keyword.MENACE.name in aKeywords && validBlockers.size <= 1) {
                totalDamage += aPower
                continue
            }

            if (validBlockers.isEmpty()) {
                totalDamage += aPower
                continue
            }

            if (hasTrample) {
                // Opponent picks the highest-toughness blocker to minimize overflow
                val bestBlocker = validBlockers.maxByOrNull { projected.getToughness(it) ?: 0 }!!
                totalDamage += damageDealtThrough(projected, attacker, bestBlocker)
                usedBlockers.add(bestBlocker)
            } else {
                // Non-trampler: any blocker prevents all damage; opponent uses cheapest
                val cheapestBlocker = validBlockers.minByOrNull { creatureValue(state, projected, it) }!!
                usedBlockers.add(cheapestBlocker)
                // No damage gets through
            }
        }

        return totalDamage
    }

    // ── Race Clock ──────────────────────────────────────────────────────

    /**
     * Estimate how many turns until [attacker] can kill [defender] using damage through optimal blocking.
     * Uses total expected damage (evasive + trample overflow + unblockable surplus) rather than
     * only evasive damage, so the race clock is meaningful even with ground creatures.
     */
    fun turnsToKill(
        state: GameState,
        projected: ProjectedState,
        attackerPlayer: EntityId,
        defenderPlayer: EntityId,
        opponentBlockers: List<EntityId>
    ): Int {
        val defenderLife = state.getEntity(defenderPlayer)
            ?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life ?: 20
        val myCreatures = projected.getBattlefieldControlledBy(attackerPlayer)
            .filter { projected.isCreature(it) && !state.getEntity(it)!!.has<TappedComponent>() }
        val damageThrough = calculateDamageThroughOptimalBlocking(state, projected, myCreatures, opponentBlockers)
        if (damageThrough <= 0) return Int.MAX_VALUE
        return (defenderLife + damageThrough - 1) / damageThrough // ceiling division
    }

    // ── Aggression Analysis ───────────────────────────────────────────

    /**
     * Aggression level (0-5) inspired by Forge's combat AI.
     * Compares how fast each player can kill the other using a life-to-damage ratio.
     *
     * - 5: All-out attack (attritional win or dominant race)
     * - 4: Aggressive (winning the race, willing to trade)
     * - 3: Moderate (slightly ahead or even, attack with expendable creatures)
     * - 2: Cautious (slightly behind, only safe attacks)
     * - 1: Defensive (only evasive/unblockable creatures)
     * - 0: Full defense (hold everything back)
     */
    fun calculateAggressionLevel(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        opponentId: EntityId,
        myCreatures: List<EntityId>,
        opponentBlockers: List<EntityId>
    ): Int {
        val myLife = state.getEntity(playerId)?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life ?: 20
        val opponentLife = state.getEntity(opponentId)?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life ?: 20

        val myDamageThrough = calculateDamageThroughOptimalBlocking(state, projected, myCreatures, opponentBlockers)
        val opponentAttackers = getCreaturesThatCanAttack(state, projected, opponentId)
        val myBlockers = myCreatures // all untapped creatures can block
        val theirDamageThrough = calculateDamageThroughOptimalBlocking(state, projected, opponentAttackers, myBlockers)

        // Evasive-only damage (guaranteed regardless of blocks)
        val myEvasiveDamage = calculateEvasiveDamage(state, projected, myCreatures, opponentBlockers)

        // Race ratio: how many turns until I kill them vs they kill me
        // Higher ratio = safer for me
        val myLifeToTheirDamage = if (theirDamageThrough > 0) myLife.toDouble() / theirDamageThrough else 99.0
        val theirLifeToMyDamage = if (myDamageThrough > 0) opponentLife.toDouble() / myDamageThrough else 99.0
        val ratioDiff = myLifeToTheirDamage - theirLifeToMyDamage

        // Attritional analysis
        val attritionalWin = simulateAttritionalAttack(state, projected, playerId, opponentId, opponentBlockers)

        // Creature count advantage
        val myCount = myCreatures.size
        val theirCount = opponentBlockers.size
        val outnumber = myCount - theirCount

        return when {
            // Level 5: we win the attrition war AND we're not losing the race
            ratioDiff > 0 && attritionalWin -> 5
            // Level 4: winning the race significantly, or opponent is very low
            ratioDiff >= 1.0 || (opponentLife <= 8 && myEvasiveDamage > 0) -> 4
            // Level 3: roughly even or slightly ahead, especially with more creatures
            ratioDiff >= 0 || outnumber >= 2 -> 3
            // Level 2: slightly behind but not desperate
            ratioDiff >= -1.5 || outnumber >= 1 -> 2
            // Level 1: behind on the race, only evasive attacks make sense
            myEvasiveDamage > 0 -> 1
            // Level 0: we're losing badly — hunker down
            else -> 0
        }
    }

    /**
     * Simulate a war of attrition: we repeatedly send our weakest creature to attack,
     * they block and kill it with their best creature. After each round, check if we
     * deal enough evasive + overflow damage to eventually win.
     *
     * Returns true if attritional attacking wins before we run out of creatures.
     */
    fun simulateAttritionalAttack(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        opponentId: EntityId,
        opponentBlockers: List<EntityId>
    ): Boolean {
        val opponentLife = state.getEntity(opponentId)
            ?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life ?: 20

        val myCreatures = projected.getBattlefieldControlledBy(playerId)
            .filter { projected.isCreature(it) && state.getEntity(it)?.has<TappedComponent>() != true }
            .sortedBy { creatureValue(state, projected, it) }
            .toMutableList()
        val theirBlockers = opponentBlockers.toMutableList()

        var remainingLife = opponentLife
        var rounds = 0
        val maxRounds = 10

        while (myCreatures.isNotEmpty() && rounds < maxRounds) {
            rounds++

            // Calculate damage through optimal blocking this round
            val damageThrough = calculateDamageThroughOptimalBlocking(state, projected, myCreatures, theirBlockers)
            remainingLife -= damageThrough

            if (remainingLife <= 0) return true

            // Simulate: opponent kills our weakest non-evasive attacker with their best blocker
            val expendable = myCreatures.firstOrNull { !isEvasive(state, projected, it, theirBlockers) }
            if (expendable != null) {
                myCreatures.remove(expendable)
                // The blocker that killed our creature might also die (mutual trade)
                val killer = theirBlockers.firstOrNull {
                    canBeBlockedBy(state, projected, expendable, it) &&
                        wouldKillInCombat(state, projected, it, expendable)
                }
                if (killer != null && wouldKillInCombat(state, projected, expendable, killer)) {
                    theirBlockers.remove(killer) // mutual kill
                }
            } else {
                // All our creatures are evasive — just check if we win by evasion alone
                break
            }
        }

        // After attrition, check if remaining evasive damage finishes them off
        if (myCreatures.isNotEmpty() && remainingLife > 0) {
            val evasiveDmg = calculateEvasiveDamage(state, projected, myCreatures, theirBlockers)
            if (evasiveDmg > 0) {
                val turnsLeft = (remainingLife + evasiveDmg - 1) / evasiveDmg
                return turnsLeft <= 3
            }
        }

        return remainingLife <= 0
    }

    /**
     * Get creatures controlled by [playerId] that can actually attack
     * (not tapped, no summoning sickness unless haste, no defender).
     */
    fun getCreaturesThatCanAttack(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId
    ): List<EntityId> {
        return projected.getBattlefieldControlledBy(playerId).filter { entityId ->
            projected.isCreature(entityId) &&
                state.getEntity(entityId)?.has<TappedComponent>() != true &&
                !projected.cantAttack(entityId) &&
                Keyword.DEFENDER.name !in projected.getKeywords(entityId) &&
                (state.getEntity(entityId)?.has<com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent>() != true ||
                    Keyword.HASTE.name in projected.getKeywords(entityId))
        }
    }

    /**
     * Life-scaled trade willingness threshold, inspired by Forge's `diff` variable.
     *
     * At high life, we're conservative (only trade when the blocker is significantly
     * more valuable). At low life, we accept even trades or slight downtrades because
     * every point of damage matters more.
     *
     * Returns a ratio: our creature's value must be <= blockerValue * ratio to accept the trade.
     * Higher ratio = more willing to trade (e.g., 1.5 means we'll trade our 6-value creature
     * for their 4-value blocker).
     */
    fun tradeWillingnessRatio(myLife: Int): Double {
        // At 20 life: 0.5 (only accept trades where blocker >= 50% our value)
        // At 10 life: 0.75 (accept trades where blocker >= 75% our value)
        // At 5 life:  1.0 (accept even trades)
        // At 3 life:  1.3 (accept slight downtrades)
        return when {
            myLife >= 20 -> 0.5
            myLife >= 15 -> 0.6
            myLife >= 10 -> 0.75
            myLife >= 7 -> 0.9
            myLife >= 5 -> 1.0
            myLife >= 3 -> 1.3
            else -> 1.5 // desperate — accept bad trades
        }
    }

    // ── Combat Trick Estimation ─────────────────────────────────────────

    /**
     * Estimate the potential pump bonus an opponent could apply based on untapped mana.
     * Returns a (power bonus, toughness bonus) pair.
     */
    fun estimateCombatTrickBonus(
        state: GameState,
        projected: ProjectedState,
        opponentId: EntityId
    ): Pair<Int, Int> {
        val untappedLands = projected.getBattlefieldControlledBy(opponentId).count { entityId ->
            val card = state.getEntity(entityId)?.get<CardComponent>()
            card != null && card.isLand && state.getEntity(entityId)?.has<TappedComponent>() != true
        }
        val cardsInHand = state.getHand(opponentId).size

        if (cardsInHand == 0) return 0 to 0

        // Conservative estimate: most combat tricks cost 1-2 mana and give +2/+2 or +3/+3.
        // But assuming the worst case every time makes the AI too passive — discount by
        // the probability the opponent actually has a trick (roughly 1-in-4 cards).
        return when {
            untappedLands >= 3 && cardsInHand >= 3 -> 2 to 2
            untappedLands >= 2 && cardsInHand >= 2 -> 1 to 1
            untappedLands >= 1 -> 1 to 1
            else -> 0 to 0
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    fun creatureValue(state: GameState, projected: ProjectedState, entityId: EntityId): Double {
        val card = state.getEntity(entityId)?.get<CardComponent>() ?: return 0.0
        return com.wingedsheep.engine.ai.evaluation.BoardPresence.permanentValue(state, projected, entityId, card)
    }

    /**
     * Get opponent's untapped creatures that could block.
     */
    fun getOpponentUntappedCreatures(
        state: GameState,
        projected: ProjectedState,
        opponentId: EntityId
    ): List<EntityId> {
        return projected.getBattlefieldControlledBy(opponentId).filter { entityId ->
            projected.isCreature(entityId) &&
                state.getEntity(entityId)?.has<TappedComponent>() != true
        }
    }
}
