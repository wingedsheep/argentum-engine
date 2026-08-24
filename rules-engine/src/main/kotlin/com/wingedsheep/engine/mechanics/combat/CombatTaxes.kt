package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
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

    private val dynamicAmountEvaluator = DynamicAmountEvaluator()
    private val conditionEvaluator = ConditionEvaluator()

    /** [total] generic mana, the shape every combat tax is paid in. */
    fun genericCost(total: Int): ManaCost = ManaCost(List(total) { ManaSymbol.generic(1) })

    /**
     * The generic-mana tax owed for declaring [attackers] (creature → defender), without paying it.
     *
     * [AttackTax] is a per-defender restriction: only permanents controlled by the player being
     * attacked (or protecting the attacked planeswalker/battle) tax, and only for the attackers
     * aimed at them.
     */
    fun attackTax(
        state: GameState,
        cardRegistry: CardRegistry,
        attackers: Map<EntityId, EntityId>,
        projected: ProjectedState,
    ): Int {
        if (attackers.isEmpty()) return 0
        val attackersPerDefender = mutableMapOf<EntityId, Int>()
        for ((_, defenderId) in attackers) {
            val defenderPlayerId = if (state.turnOrder.contains(defenderId)) {
                defenderId
            } else {
                projected.getController(defenderId)
            }
            if (defenderPlayerId != null) {
                attackersPerDefender[defenderPlayerId] = (attackersPerDefender[defenderPlayerId] ?: 0) + 1
            }
        }

        var totalGenericTax = 0
        for ((defenderId, attackerCount) in attackersPerDefender) {
            val defenderPermanents = projected.getBattlefieldControlledBy(defenderId)
            for (entityId in defenderPermanents) {
                val container = state.getEntity(entityId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue
                val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue
                for (ability in cardDef.staticAbilities) {
                    if (ability !is AttackTax) continue
                    val ctx = EffectContext(sourceId = entityId, controllerId = defenderId)
                    // Gate on the source's state (e.g. Archangel of Tithes — only while untapped).
                    val condition = ability.condition
                    if (condition != null && !conditionEvaluator.evaluate(state, condition, ctx)) {
                        continue
                    }
                    val taxPerAttacker =
                        maxOf(0, dynamicAmountEvaluator.evaluate(state, ability.amountPerAttacker, ctx, projected))
                    totalGenericTax += taxPerAttacker * attackerCount
                }
            }
        }

        return totalGenericTax + perCreatureTax(state, attackers.keys, projected) +
            selfTax(state, cardRegistry, attackers.keys, projected, taxingBlockers = false)
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
    ): Int {
        if (blockerIds.isEmpty()) return 0
        var totalTax = 0
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue
            for (ability in cardDef.staticAbilities) {
                if (ability !is BlockTax) continue
                val controllerId = projected.getController(entityId) ?: continue
                val ctx = EffectContext(sourceId = entityId, controllerId = controllerId)
                val condition = ability.condition
                if (condition != null && !conditionEvaluator.evaluate(state, condition, ctx)) {
                    continue
                }
                val taxPerBlocker =
                    maxOf(0, dynamicAmountEvaluator.evaluate(state, ability.amountPerBlocker, ctx, projected))
                totalTax += taxPerBlocker * blockerIds.size
            }
        }
        return totalTax + perCreatureTax(state, blockerIds, projected) +
            selfTax(state, cardRegistry, blockerIds, projected, taxingBlockers = true)
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
                totalTax += maxOf(0, dynamicAmountEvaluator.evaluate(state, ability.amount, ctx, projected))
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
