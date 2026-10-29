package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.durations.GrantDurationGate
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AttackTax
import com.wingedsheep.sdk.scripting.BlockTax
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.sdk.scripting.CantAttackOrBlockUnlessPay
import com.wingedsheep.engine.state.components.identity.faceUpCardDefinition

/**
 * What a proposed attack or block costs in generic mana before it may be declared — Ghostly Prison,
 * Windborn Muse, Baird, Archangel of Tithes, Whipgrass Entangler.
 *
 * The tax is part of the *cost* of the declaration (CR 508.1a / 509.1a), so it is not only
 * [AttackPhaseManager]'s and [BlockPhaseManager]'s business: anyone deciding whether a declaration
 * is worth proposing — most importantly the AI, which otherwise proposes attacks it cannot pay for
 * and then has nothing to do with the payment prompt but decline it — has to be able to price one
 * first. Hence a public object rather than a private method on each phase manager (which is also
 * where the per-creature half of it used to live twice, verbatim).
 *
 * The tax is monotone in the declared set: dropping a creature never raises it. That is what lets a
 * caller trim an unaffordable declaration one creature at a time until it can pay.
 */
object CombatTaxes {

    /** [total] generic mana, the shape every combat tax is paid in. */
    fun genericCost(total: Int): ManaCost = ManaCost(List(total) { ManaSymbol.generic(1) })

    /**
     * The generic-mana tax owed for declaring [attackers] (creature → defender), without paying it.
     *
     * [AttackTax] is a per-defender restriction: only permanents controlled by the player being
     * attacked tax, and only for the attackers aimed at them — at that player, or at a planeswalker
     * they control when the tax says "or planeswalkers you control" ([AttackTax.coversPlaneswalkers]).
     * An attack on a battle is taxed by nobody: no printed tax names battles, and the battle's
     * controller (who may be the attacker, for a Siege) is certainly not the one taxing it.
     */
    fun attackTax(
        state: GameState,
        cardRegistry: CardRegistry,
        attackers: Map<EntityId, EntityId>,
        projected: ProjectedState,
        predicateEvaluator: PredicateEvaluator
    ): Int {
        if (attackers.isEmpty()) return 0
        // Per taxing player: attackers aimed at them directly, and attackers aimed at a planeswalker
        // they control. Battles fall in neither bucket.
        val attackersAtPlayer = mutableMapOf<EntityId, Int>()
        val attackersAtPlaneswalkers = mutableMapOf<EntityId, Int>()
        for ((_, defenderId) in attackers) {
            if (state.turnOrder.contains(defenderId)) {
                attackersAtPlayer[defenderId] = (attackersAtPlayer[defenderId] ?: 0) + 1
            } else if (projected.isPlaneswalker(defenderId)) {
                val controller = projected.getController(defenderId) ?: continue
                attackersAtPlaneswalkers[controller] = (attackersAtPlaneswalkers[controller] ?: 0) + 1
            }
        }

        // One tax ability held on behalf of [you], the player it protects, with [holder] as its source.
        fun taxFrom(ability: AttackTax, holder: EntityId, you: EntityId): Int {
            val attackerCount = (attackersAtPlayer[you] ?: 0) +
                if (ability.coversPlaneswalkers) attackersAtPlaneswalkers[you] ?: 0 else 0
            if (attackerCount == 0) return 0
            val ctx = EffectContext(sourceId = holder, controllerId = you)
            // Gate on the source's state (e.g. Archangel of Tithes — only while untapped).
            val condition = ability.condition
            if (condition != null && !predicateEvaluator.conditions.evaluate(state, condition, ctx)) return 0
            val taxPerAttacker =
                maxOf(0, predicateEvaluator.amounts.evaluate(state, ability.amountPerAttacker, ctx, projected))
            return taxPerAttacker * attackerCount
        }

        var totalGenericTax = 0
        for (defenderId in attackersAtPlayer.keys + attackersAtPlaneswalkers.keys) {
            val defenderPermanents = projected.getBattlefieldControlledBy(defenderId)
            for (entityId in defenderPermanents) {
                val container = state.getEntity(entityId) ?: continue
                val cardDef = container.faceUpCardDefinition(cardRegistry) ?: continue
                for (ability in cardDef.staticAbilities) {
                    if (ability !is AttackTax) continue
                    totalGenericTax += taxFrom(ability, entityId, defenderId)
                }
            }
        }

        // Granted taxes. A resolved effect can make the tax a rule about a *player* rather than
        // about a permanent — Summon: Yojimbo's "until your next turn, creatures can't attack you
        // unless their controller pays {2} for each of those creatures" — so a grant held by the
        // defending player taxes exactly like a printed one on a permanent they control, and keeps
        // taxing after the Saga that granted it is gone. A grant held by a permanent taxes for that
        // permanent's controller while it stays on the battlefield.
        for (grant in state.grantedStaticAbilities) {
            val ability = grant.ability as? AttackTax ?: continue
            val holder = grant.entityId
            val you = when {
                state.turnOrder.contains(holder) -> holder
                holder in state.getBattlefield() -> projected.getController(holder) ?: continue
                else -> continue
            }
            if (!GrantDurationGate.holds(state, holder, grant.sourceId, grant.duration)) continue
            totalGenericTax += taxFrom(ability, holder, you)
        }

        return totalGenericTax + perCreatureTax(state, attackers.keys, projected) +
            selfTax(state, cardRegistry, attackers.keys, projected, taxingBlockers = false, predicateEvaluator = predicateEvaluator)
    }

    /**
     * The generic-mana tax owed for declaring [blockerIds] as blockers, without paying it.
     *
     * Unlike [AttackTax], [BlockTax] is a global restriction: every permanent on the battlefield
     * with one (whose optional condition holds, e.g. "as long as this creature is attacking") taxes
     * each declared blocker by its per-blocker amount. Multiple sources stack.
     */
    fun blockTax(
        state: GameState,
        cardRegistry: CardRegistry,
        blockerIds: Set<EntityId>,
        projected: ProjectedState,
        predicateEvaluator: PredicateEvaluator
    ): Int {
        if (blockerIds.isEmpty()) return 0
        var totalTax = 0
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardDef = container.faceUpCardDefinition(cardRegistry) ?: continue
            for (ability in cardDef.staticAbilities) {
                if (ability !is BlockTax) continue
                val controllerId = projected.getController(entityId) ?: continue
                val ctx = EffectContext(sourceId = entityId, controllerId = controllerId)
                val condition = ability.condition
                if (condition != null && !predicateEvaluator.conditions.evaluate(state, condition, ctx)) {
                    continue
                }
                val taxPerBlocker =
                    maxOf(0, predicateEvaluator.amounts.evaluate(state, ability.amountPerBlocker, ctx, projected))
                totalTax += taxPerBlocker * blockerIds.size
            }
        }
        return totalTax + perCreatureTax(state, blockerIds, projected) +
            selfTax(state, cardRegistry, blockerIds, projected, taxingBlockers = true, predicateEvaluator = predicateEvaluator)
    }

    /**
     * Tax a creature charges for *itself* — [CantAttackOrBlockUnlessPay], Myr Prototype's "can't
     * attack or block unless you pay {1} for each +1/+1 counter on it".
     *
     * Unlike [AttackTax]/[BlockTax] this doesn't scan the battlefield: only the creatures actually
     * being declared can charge, and each charges once for itself. That is also what keeps the
     * whole tax monotone in the declared set — dropping a creature drops exactly its own charge.
     *
     * The amount is evaluated with the taxed creature as the source, so a counter-counting amount
     * reads that creature's counters rather than the declaring player's board. Taxes attackers and
     * blockers identically, hence one implementation for both.
     */
    private fun selfTax(
        state: GameState,
        cardRegistry: CardRegistry,
        creatureIds: Set<EntityId>,
        projected: ProjectedState,
        taxingBlockers: Boolean,
        predicateEvaluator: PredicateEvaluator
    ): Int {
        var totalTax = 0
        for (creatureId in creatureIds) {
            val container = state.getEntity(creatureId) ?: continue
            val controllerId = projected.getController(creatureId) ?: continue

            // The creature's own printed statics, plus those on the Auras/Equipment attached to it —
            // Brainwash charges for its host, not for itself. Walking the creature's *own*
            // attachments (rather than scanning the battlefield) is what keeps this whole tax
            // monotone in the declared set: the charge still depends only on the creature being
            // declared, so dropping a creature drops exactly its own charge.
            val ownStatics = container.get<CardComponent>()
                ?.let { cardRegistry.getCard(it.cardDefinitionId) }
                ?.staticAbilities
                .orEmpty()
            val attachedStatics = container.get<AttachmentsComponent>()?.attachedIds.orEmpty()
                .flatMap { attachmentId ->
                    state.getEntity(attachmentId)
                        ?.get<CardComponent>()
                        ?.let { cardRegistry.getCard(it.cardDefinitionId) }
                        ?.staticAbilities
                        .orEmpty()
                }

            for (ability in ownStatics + attachedStatics) {
                if (ability !is CantAttackOrBlockUnlessPay) continue
                // "Can't attack unless …" (Brainwash) charges nothing to block.
                if (taxingBlockers && !ability.appliesToBlocking) continue
                val ctx = EffectContext(sourceId = creatureId, controllerId = controllerId)
                totalTax += maxOf(0, predicateEvaluator.amounts.evaluate(state, ability.amount, ctx, projected))
            }
        }
        return totalTax
    }

    /**
     * Per-creature tax from `AttackBlockTaxPerCreatureType` floating effects (Whipgrass Entangler —
     * "creatures can't attack or block unless their controller pays {1} for each creature of the
     * chosen type"). Taxes attackers and blockers identically, hence one implementation.
     */
    private fun perCreatureTax(
        state: GameState,
        creatureIds: Set<EntityId>,
        projected: ProjectedState,
    ): Int {
        var totalTax = 0
        for (creatureId in creatureIds) {
            for (floatingEffect in state.floatingEffects) {
                val mod = floatingEffect.effect.modification
                if (mod !is SerializableModification.AttackBlockTaxPerCreatureType) continue
                if (creatureId !in floatingEffect.effect.affectedEntities) continue

                val creatureTypeCount = state.getBattlefield().count { entityId ->
                    projected.isCreature(entityId) && projected.hasSubtype(entityId, mod.creatureType)
                }
                val costPerCreature = ManaCost.parse(mod.manaCostPer).cmc
                totalTax += costPerCreature * creatureTypeCount
            }
        }
        return totalTax
    }
}
