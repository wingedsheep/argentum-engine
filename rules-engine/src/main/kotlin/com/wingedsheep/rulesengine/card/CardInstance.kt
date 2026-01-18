package com.wingedsheep.rulesengine.card

import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import kotlinx.serialization.Serializable

/**
 * Legacy card instance representation.
 *
 * @deprecated Use ECS components instead:
 * - CardComponent for card definition and identity
 * - ControllerComponent for controller tracking
 * - TappedComponent for tap state
 * - SummoningSicknessComponent for summoning sickness
 * - CountersComponent for counters
 * - DamageComponent for damage marked
 * - AttachedToComponent for attachment tracking
 *
 * For read queries, use StateProjector to get GameObjectView which provides
 * the calculated state after all continuous effects are applied.
 *
 * @see com.wingedsheep.rulesengine.ecs.EcsGameState
 * @see com.wingedsheep.rulesengine.ecs.layers.GameObjectView
 * @see com.wingedsheep.rulesengine.ecs.layers.StateProjector
 */
@Deprecated(
    message = "Use ECS components (CardComponent, CountersComponent, etc.) and StateProjector/GameObjectView instead",
    replaceWith = ReplaceWith("GameObjectView", "com.wingedsheep.rulesengine.ecs.layers.GameObjectView")
)
@Serializable
data class CardInstance(
    val id: CardId,
    val definition: CardDefinition,
    val ownerId: String,
    val controllerId: String = ownerId,
    val isTapped: Boolean = false,
    val summoningSickness: Boolean = true,
    val counters: Map<CounterType, Int> = emptyMap(),
    val damageMarked: Int = 0,
    val powerModifier: Int = 0,
    val toughnessModifier: Int = 0,
    val additionalKeywords: Set<Keyword> = emptySet(),
    val removedKeywords: Set<Keyword> = emptySet(),
    val attachedTo: CardId? = null,
    val equipCost: ManaCost? = null,  // Override equip cost if different from definition
    val temporaryKeywords: Set<Keyword> = emptySet(),  // Keywords granted until end of turn
    val mustBeBlocked: Boolean = false  // Flag for "must be blocked" effects
) {
    val name: String get() = definition.name

    val currentPower: Int?
        get() = definition.creatureStats?.let { it.basePower + powerModifier + plusPlusCounters }

    val currentToughness: Int?
        get() = definition.creatureStats?.let { it.baseToughness + toughnessModifier + plusPlusCounters - minusMinusCounters }

    val effectiveToughness: Int?
        get() = currentToughness?.let { it - damageMarked }

    val isCreature: Boolean get() = definition.isCreature
    val isLand: Boolean get() = definition.isLand
    val isPermanent: Boolean get() = definition.isPermanent
    val isEnchantment: Boolean get() = definition.typeLine.isEnchantment
    val isArtifact: Boolean get() = definition.typeLine.isArtifact
    val isAura: Boolean get() = definition.typeLine.isAura
    val isEquipment: Boolean get() = definition.typeLine.isEquipment
    val isAttached: Boolean get() = attachedTo != null

    val keywords: Set<Keyword>
        get() = (definition.keywords + additionalKeywords + temporaryKeywords) - removedKeywords

    val hasLethalDamage: Boolean
        get() = effectiveToughness?.let { it <= 0 } ?: false

    val canAttack: Boolean
        get() = isCreature && !isTapped && (!summoningSickness || hasKeyword(Keyword.HASTE))

    val canBlock: Boolean
        get() = isCreature && !isTapped && !hasKeyword(Keyword.DEFENDER).let { !it || hasKeyword(Keyword.DEFENDER) }
        // Note: DEFENDER creatures CAN block (that's their purpose), they just can't attack

    private val plusPlusCounters: Int
        get() = counters[CounterType.PLUS_ONE_PLUS_ONE] ?: 0

    private val minusMinusCounters: Int
        get() = counters[CounterType.MINUS_ONE_MINUS_ONE] ?: 0

    fun hasKeyword(keyword: Keyword): Boolean = keyword in keywords

    fun tap(): CardInstance = copy(isTapped = true)

    fun untap(): CardInstance = copy(isTapped = false)

    fun removeSummoningSickness(): CardInstance = copy(summoningSickness = false)

    fun dealDamage(amount: Int): CardInstance = copy(damageMarked = damageMarked + amount)

    fun clearDamage(): CardInstance = copy(damageMarked = 0)

    fun addCounter(type: CounterType, amount: Int = 1): CardInstance =
        copy(counters = counters + (type to (counters[type] ?: 0) + amount))

    fun removeCounter(type: CounterType, amount: Int = 1): CardInstance {
        val current = counters[type] ?: 0
        val newAmount = (current - amount).coerceAtLeast(0)
        return if (newAmount == 0) {
            copy(counters = counters - type)
        } else {
            copy(counters = counters + (type to newAmount))
        }
    }

    fun addKeyword(keyword: Keyword): CardInstance =
        copy(additionalKeywords = additionalKeywords + keyword)

    fun removeKeyword(keyword: Keyword): CardInstance =
        copy(removedKeywords = removedKeywords + keyword)

    fun modifyPower(amount: Int): CardInstance =
        copy(powerModifier = powerModifier + amount)

    fun modifyToughness(amount: Int): CardInstance =
        copy(toughnessModifier = toughnessModifier + amount)

    fun changeController(newControllerId: String): CardInstance =
        copy(controllerId = newControllerId)

    fun attachTo(targetId: CardId): CardInstance =
        copy(attachedTo = targetId)

    fun detach(): CardInstance =
        copy(attachedTo = null)

    fun addTemporaryKeyword(keyword: Keyword): CardInstance =
        copy(temporaryKeywords = temporaryKeywords + keyword)

    fun withMustBeBlocked(value: Boolean): CardInstance =
        copy(mustBeBlocked = value)

    /**
     * Clear all temporary effects at end of turn.
     */
    fun clearTemporaryEffects(): CardInstance =
        copy(temporaryKeywords = emptySet(), mustBeBlocked = false)

    companion object {
        fun create(definition: CardDefinition, ownerId: String): CardInstance =
            CardInstance(
                id = CardId.generate(),
                definition = definition,
                ownerId = ownerId
            )
    }
}

@Serializable
enum class CounterType {
    PLUS_ONE_PLUS_ONE,
    MINUS_ONE_MINUS_ONE,
    LOYALTY,
    CHARGE,
    POISON
}
