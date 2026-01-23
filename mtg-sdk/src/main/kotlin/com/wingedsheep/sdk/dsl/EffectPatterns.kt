package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.*

/**
 * Helper object for creating common effect patterns.
 *
 * These patterns reduce boilerplate for frequently-used effect combinations
 * while keeping the underlying effect types composable.
 *
 * Usage:
 * ```kotlin
 * // Instead of:
 * OptionalCostEffect(
 *     cost = PayLifeEffect(2),
 *     ifPaid = DrawCardsEffect(1),
 *     ifNotPaid = null
 * )
 *
 * // You can write:
 * EffectPatterns.mayPay(PayLifeEffect(2), DrawCardsEffect(1))
 * ```
 */
object EffectPatterns {

    // =========================================================================
    // Optional Cost Patterns
    // =========================================================================

    /**
     * Create an optional cost effect: "You may [cost]. If you do, [effect]."
     *
     * Example:
     * ```kotlin
     * mayPay(PayLifeEffect(2), DrawCardsEffect(1))
     * // -> "You may pay 2 life. If you do, draw a card."
     * ```
     */
    fun mayPay(cost: Effect, effect: Effect): OptionalCostEffect =
        OptionalCostEffect(cost, effect)

    /**
     * Create an optional cost with an "otherwise" clause.
     *
     * Example:
     * ```kotlin
     * mayPayOrElse(
     *     PayLifeEffect(3),
     *     ifPaid = DrawCardsEffect(2),
     *     ifNotPaid = DiscardCardsEffect(1)
     * )
     * ```
     */
    fun mayPayOrElse(cost: Effect, ifPaid: Effect, ifNotPaid: Effect): OptionalCostEffect =
        OptionalCostEffect(cost, ifPaid, ifNotPaid)

    // =========================================================================
    // Sacrifice Patterns
    // =========================================================================

    /**
     * Create a sacrifice-for-effect pattern with variable binding.
     *
     * Used for Scapeshift-style effects where the count matters:
     * "Sacrifice any number of lands. Search for that many lands."
     *
     * @param filter What to sacrifice (e.g., CardFilter.LandCard)
     * @param countName Variable name to store the count
     * @param thenEffect Effect that uses the stored count
     */
    fun sacrificeFor(
        filter: CardFilter,
        countName: String,
        thenEffect: Effect
    ): CompositeEffect = CompositeEffect(
        listOf(
            StoreCountEffect(
                effect = SacrificeEffect(filter, any = true),
                storeAs = EffectVariable.Count(countName)
            ),
            thenEffect
        )
    )

    /**
     * Create a sacrifice pattern with a fixed count.
     *
     * Example:
     * ```kotlin
     * sacrifice(CardFilter.CreatureCard, then = DrawCardsEffect(2))
     * // -> "Sacrifice a creature. Draw two cards."
     * ```
     */
    fun sacrifice(
        filter: CardFilter,
        count: Int = 1,
        then: Effect
    ): CompositeEffect = CompositeEffect(
        listOf(
            SacrificeEffect(filter, count),
            then
        )
    )

    // =========================================================================
    // Reflexive Trigger Patterns
    // =========================================================================

    /**
     * Create a reflexive trigger: "[action]. When you do, [effect]."
     *
     * Used for Heart-Piercer Manticore style abilities where an optional
     * action triggers a follow-up effect.
     *
     * Example:
     * ```kotlin
     * reflexiveTrigger(
     *     action = SacrificeEffect(CardFilter.CreatureCard),
     *     whenYouDo = DealDamageEffect(3, EffectTarget.AnyTarget)
     * )
     * ```
     */
    fun reflexiveTrigger(
        action: Effect,
        whenYouDo: Effect,
        optional: Boolean = true
    ): ReflexiveTriggerEffect = ReflexiveTriggerEffect(action, optional, whenYouDo)

    // =========================================================================
    // Store and Reference Patterns
    // =========================================================================

    /**
     * Create an effect that stores its result for later reference.
     *
     * Used for Oblivion Ring style effects where you need to remember
     * what was exiled to return it later.
     *
     * Example:
     * ```kotlin
     * storeEntity(
     *     effect = ExileEffect(EffectTarget.ContextTarget(0)),
     *     as = "exiledCard"
     * )
     * // Later: ReturnFromExileEffect using StoredEntityTarget("exiledCard")
     * ```
     */
    fun storeEntity(effect: Effect, `as`: String): StoreResultEffect =
        StoreResultEffect(effect, EffectVariable.EntityRef(`as`))

    /**
     * Create an effect that stores a count for later reference.
     *
     * Example:
     * ```kotlin
     * storeCount(
     *     effect = SacrificeEffect(CardFilter.LandCard, any = true),
     *     as = "sacrificedLands"
     * )
     * // Later: SearchLibraryEffect with count = VariableReference("sacrificedLands")
     * ```
     */
    fun storeCount(effect: Effect, `as`: String): StoreCountEffect =
        StoreCountEffect(effect, EffectVariable.Count(`as`))

    // =========================================================================
    // Composite Patterns
    // =========================================================================

    /**
     * Chain multiple effects together.
     *
     * Example:
     * ```kotlin
     * sequence(
     *     DrawCardsEffect(3),
     *     DiscardCardsEffect(2)
     * )
     * ```
     */
    fun sequence(vararg effects: Effect): CompositeEffect =
        CompositeEffect(effects.toList())

    /**
     * Create an exile-and-return pattern used by O-Ring style cards.
     *
     * This creates the appropriate variable binding so the second trigger
     * can return the exact card that was exiled.
     *
     * @param exileTarget What to exile initially
     * @param variableName Name to store the exiled card under
     */
    fun exileUntilLeaves(
        exileTarget: EffectTarget,
        variableName: String = "exiledCard"
    ): StoreResultEffect = StoreResultEffect(
        effect = ExileEffect(exileTarget),
        storeAs = EffectVariable.EntityRef(variableName)
    )
}
