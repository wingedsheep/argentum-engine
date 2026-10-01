package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.SpendManaAsColor
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.engine.mechanics.durations.GrantDurationGate
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TextChanges
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId

/** Directional spending permissions; actual mana production and bookkeeping stay unchanged. */
object ManaSpendingRules {
    private val conditions = ConditionEvaluator(PredicateEvaluator(cardRegistry = null))

    /** Required color -> actual colors that may pay it. Includes chained permissions (CR 609.4a). */
    fun colors(state: GameState, payerId: EntityId): Map<Color, Set<Color>> {
        var result: MutableMap<Color, MutableSet<Color>>? = null
        val projected = state.projectedState
        for (id in state.controlledBattlefield(payerId)) {
            val entity = state.getEntity(id) ?: continue
            val grants = entity.get<CardComponent>()?.manaSpendingGrants.orEmpty()
            if (grants.isEmpty()) continue
            if (entity.has<FaceDownComponent>() || projected.hasLostAllAbilities(id)) continue
            val context = EffectContext(sourceId = id, controllerId = payerId)
            val text = TextChanges.of(state, id)
            for (grant in grants) {
                if (grant.conditions.any { !conditions.evaluate(state, if (text == null) it else it.applyTextReplacement(text), context) }) continue
                val permission = if (text == null) grant.permission
                    else grant.permission.applyTextReplacement(text) as SpendManaAsColor
                if (permission.fromColor == permission.toColor) continue
                val map = result ?: mutableMapOf<Color, MutableSet<Color>>().also { result = it }
                map.getOrPut(permission.toColor) { mutableSetOf(permission.toColor) }.add(permission.fromColor)
            }
        }
        fun collectGranted(ability: StaticAbility, holder: EntityId) {
            when (ability) {
                is SpendManaAsColor -> {
                    if (ability.fromColor != ability.toColor) {
                        val map = result ?: mutableMapOf<Color, MutableSet<Color>>().also { result = it }
                        map.getOrPut(ability.toColor) { mutableSetOf(ability.toColor) }.add(ability.fromColor)
                    }
                }
                is ConditionalStaticAbility -> {
                    if (conditions.evaluate(state, ability.condition, EffectContext(sourceId = holder, controllerId = payerId)))
                        collectGranted(ability.ability, holder)
                }
                is CompositeStaticAbility -> ability.abilities.forEach { collectGranted(it, holder) }
                else -> Unit
            }
        }
        for (grant in state.grantedStaticAbilities) {
            val holder = grant.entityId
            val isPlayer = holder == payerId
            if (!isPlayer && (projected.getController(holder) != payerId || holder !in state.getBattlefield())) continue
            if (!isPlayer && (state.getEntity(holder)?.has<FaceDownComponent>() == true || projected.hasLostAllAbilities(holder))) continue
            if (!GrantDurationGate.holds(state, holder, grant.sourceId, grant.duration)) continue
            // Text-changing effects never change externally granted abilities.
            collectGranted(grant.ability, holder)
        }
        val map = result ?: return emptyMap()
        // Five colors bound this closure; cycles never duplicate or create mana.
        repeat(Color.entries.size) {
            for (allowed in map.values) {
                allowed.addAll(allowed.toList().flatMap { map[it].orEmpty() })
            }
        }
        return map.mapValues { it.value.toSet() }
    }
}

fun ManaPool.withSpendingColors(state: GameState, payerId: EntityId): ManaPool =
    copy(spendingColors = ManaSpendingRules.colors(state, payerId))
