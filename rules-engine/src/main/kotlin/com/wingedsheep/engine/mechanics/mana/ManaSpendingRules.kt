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
import com.wingedsheep.sdk.scripting.references.Player

/** Directional spending permissions; actual mana production and bookkeeping stay unchanged. */
object ManaSpendingRules {
    private val conditions = ConditionEvaluator(PredicateEvaluator(cardRegistry = null))

    /**
     * Required color -> the actual mana that may pay it, `null` standing for colorless mana.
     * Includes chained permissions (CR 609.4a). A required color absent from the map is paid only
     * by mana of that color, exactly as without any permission.
     */
    fun colors(state: GameState, payerId: EntityId): Map<Color, Set<Color?>> {
        var result: MutableMap<Color, MutableSet<Color?>>? = null
        fun add(permission: SpendManaAsColor) {
            val froms: List<Color?> = permission.fromColor?.let { listOf(it) } ?: ANY_MANA
            val tos = permission.toColor?.let { listOf(it) } ?: Color.entries
            val map = result ?: mutableMapOf<Color, MutableSet<Color?>>().also { result = it }
            for (to in tos) {
                val allowed = froms.filter { it != to }
                if (allowed.isEmpty()) continue
                map.getOrPut(to) { mutableSetOf(to) }.addAll(allowed)
            }
        }
        val projected = state.projectedState
        for (id in state.getBattlefield()) {
            val entity = state.getEntity(id) ?: continue
            val grants = entity.get<CardComponent>()?.manaSpendingGrants.orEmpty()
            if (grants.isEmpty()) continue
            val controllerId = projected.getController(id) ?: continue
            if (grants.none { it.permission.players.reaches(payerId, controllerId) }) continue
            if (entity.has<FaceDownComponent>() || projected.hasLostAllAbilities(id)) continue
            val context = EffectContext(sourceId = id, controllerId = controllerId)
            val text = TextChanges.of(state, id)
            for (grant in grants) {
                if (!grant.permission.players.reaches(payerId, controllerId)) continue
                if (grant.conditions.any { !conditions.evaluate(state, if (text == null) it else it.applyTextReplacement(text), context) }) continue
                add(if (text == null) grant.permission else grant.permission.applyTextReplacement(text) as SpendManaAsColor)
            }
        }
        fun collectGranted(ability: StaticAbility, holder: EntityId, controllerId: EntityId) {
            when (ability) {
                is SpendManaAsColor -> if (ability.players.reaches(payerId, controllerId)) add(ability)
                is ConditionalStaticAbility -> {
                    if (conditions.evaluate(state, ability.condition, EffectContext(sourceId = holder, controllerId = controllerId)))
                        collectGranted(ability.ability, holder, controllerId)
                }
                is CompositeStaticAbility -> ability.abilities.forEach { collectGranted(it, holder, controllerId) }
                else -> Unit
            }
        }
        for (grant in state.grantedStaticAbilities) {
            val holder = grant.entityId
            val isPlayer = state.turnOrder.contains(holder)
            val controllerId = if (isPlayer) holder else projected.getController(holder) ?: continue
            if (!isPlayer && holder !in state.getBattlefield()) continue
            if (!isPlayer && (state.getEntity(holder)?.has<FaceDownComponent>() == true || projected.hasLostAllAbilities(holder))) continue
            if (!GrantDurationGate.holds(state, holder, grant.sourceId, grant.duration)) continue
            // Text-changing effects never change externally granted abilities.
            collectGranted(grant.ability, holder, controllerId)
        }
        val map = result ?: return emptyMap()
        // Five colors bound this closure; cycles never duplicate or create mana.
        repeat(Color.entries.size) {
            for (allowed in map.values) {
                allowed.addAll(allowed.toList().flatMap { actual -> actual?.let { map[it] }.orEmpty() })
            }
        }
        return map.mapValues { it.value.toSet() }
    }

    /** Every kind of mana a `fromColor = null` permission speaks of: the five colors and colorless. */
    private val ANY_MANA: List<Color?> = Color.entries + null

    /** Whether a permission scoped to [this] reaches [payerId] when [controllerId] controls its source. */
    private fun Player.reaches(payerId: EntityId, controllerId: EntityId): Boolean = when (this) {
        Player.Each -> true
        Player.EachOpponent -> payerId != controllerId
        Player.You -> payerId == controllerId
        // SpendManaAsColor's init admits only the three above; anything else reaches nobody.
        else -> false
    }
}

fun ManaPool.withSpendingColors(state: GameState, payerId: EntityId): ManaPool =
    copy(spendingColors = ManaSpendingRules.colors(state, payerId))
