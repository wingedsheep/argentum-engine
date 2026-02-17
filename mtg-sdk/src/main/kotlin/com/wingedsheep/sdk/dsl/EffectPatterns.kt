package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Zone
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
     * @param filter What to sacrifice (e.g., GameObjectFilter.Land)
     * @param countName Variable name to store the count
     * @param thenEffect Effect that uses the stored count
     */
    fun sacrificeFor(
        filter: GameObjectFilter,
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
     * sacrifice(GameObjectFilter.Creature, then = DrawCardsEffect(2))
     * // -> "Sacrifice a creature. Draw two cards."
     * ```
     */
    fun sacrifice(
        filter: GameObjectFilter,
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
     *     whenYouDo = DealDamageEffect(3, EffectTarget.ContextTarget(0))
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
     *     effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.Exile),
     *     as = "exiledCard"
     * )
     * // Later: return from exile using StoredEntityTarget("exiledCard")
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

    // =========================================================================
    // Pipeline Patterns
    // =========================================================================

    /**
     * Look at the top N cards of your library, keep some, send the rest elsewhere.
     *
     * This creates a Gather → Select → Move pipeline using CompositeEffect.
     *
     * Example: Ancestral Memories — "Look at the top 7 cards of your library.
     * Put 2 of them into your hand and the rest into your graveyard."
     * ```kotlin
     * lookAtTopAndKeep(count = 7, keepCount = 2)
     * ```
     *
     * @param count How many cards to look at from the top of the library
     * @param keepCount How many cards the player keeps
     * @param keepDestination Where kept cards go (default: hand)
     * @param restDestination Where remaining cards go (default: graveyard)
     * @param revealed Whether the gathered cards are revealed to all players
     */
    fun lookAtTopAndKeep(
        count: Int,
        keepCount: Int,
        keepDestination: CardDestination = CardDestination.ToZone(Zone.HAND),
        restDestination: CardDestination = CardDestination.ToZone(Zone.GRAVEYARD),
        revealed: Boolean = false
    ): CompositeEffect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(count)),
                storeAs = "looked",
                revealed = revealed
            ),
            SelectFromCollectionEffect(
                from = "looked",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(keepCount)),
                storeSelected = "kept",
                storeRemainder = "rest"
            ),
            MoveCollectionEffect(
                from = "kept",
                destination = keepDestination
            ),
            MoveCollectionEffect(
                from = "rest",
                destination = restDestination
            )
        )
    )

    /**
     * Look at the top N cards of your library, then put them back in any order.
     *
     * This creates a Gather → Move(ControllerChooses) pipeline.
     *
     * Example: Sage Aven — "Look at the top four cards of your library,
     * then put them back in any order."
     * ```kotlin
     * lookAtTopAndReorder(4)
     * ```
     *
     * @param count How many cards to look at
     */
    fun lookAtTopAndReorder(count: Int): CompositeEffect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(count)),
                storeAs = "looked"
            ),
            MoveCollectionEffect(
                from = "looked",
                destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top),
                order = CardOrder.ControllerChooses
            )
        )
    )

    /**
     * Look at the top X cards of your library (dynamic count), then put them back in any order.
     *
     * @param count Dynamic amount for how many cards to look at
     */
    fun lookAtTopAndReorder(count: DynamicAmount): CompositeEffect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(count),
                storeAs = "looked"
            ),
            MoveCollectionEffect(
                from = "looked",
                destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top),
                order = CardOrder.ControllerChooses
            )
        )
    )

    /**
     * Scry N — Look at the top N cards of your library, then put any number of them
     * on the bottom of your library and the rest on top in any order.
     *
     * Creates a Gather → Select → Move pipeline.
     *
     * @param count How many cards to scry
     */
    fun scry(count: Int): CompositeEffect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(count)),
                storeAs = "scried"
            ),
            SelectFromCollectionEffect(
                from = "scried",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(count)),
                storeSelected = "toBottom",
                storeRemainder = "toTop"
            ),
            MoveCollectionEffect(
                from = "toBottom",
                destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom)
            ),
            MoveCollectionEffect(
                from = "toTop",
                destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top),
                order = CardOrder.ControllerChooses
            )
        )
    )

    /**
     * Surveil N — Look at the top N cards of your library, then put any number of them
     * into your graveyard and the rest on top of your library in any order.
     *
     * Creates a Gather → Select → Move pipeline.
     *
     * @param count How many cards to surveil
     */
    fun surveil(count: Int): CompositeEffect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(count)),
                storeAs = "surveiled"
            ),
            SelectFromCollectionEffect(
                from = "surveiled",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(count)),
                storeSelected = "toGraveyard",
                storeRemainder = "toTop"
            ),
            MoveCollectionEffect(
                from = "toGraveyard",
                destination = CardDestination.ToZone(Zone.GRAVEYARD)
            ),
            MoveCollectionEffect(
                from = "toTop",
                destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top),
                order = CardOrder.ControllerChooses
            )
        )
    )

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
        effect = MoveToZoneEffect(exileTarget, Zone.EXILE),
        storeAs = EffectVariable.EntityRef(variableName)
    )
}
