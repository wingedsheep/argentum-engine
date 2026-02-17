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
     * // Later: Effects.SearchLibrary with count = VariableReference("sacrificedLands")
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
     * Search your library for cards matching a filter and put them in a destination zone.
     *
     * Creates a Gather → Select → Move pipeline with optional shuffle.
     *
     * @param filter Which cards can be found
     * @param count How many cards can be selected
     * @param destination Where to put the found cards
     * @param entersTapped Whether cards enter the battlefield tapped
     * @param shuffleAfter Whether to shuffle after searching
     * @param reveal Whether to reveal the found cards
     */
    fun searchLibrary(
        filter: GameObjectFilter = GameObjectFilter.Any,
        count: Int = 1,
        destination: SearchDestination = SearchDestination.HAND,
        entersTapped: Boolean = false,
        shuffleAfter: Boolean = true,
        reveal: Boolean = false
    ): CompositeEffect {
        val effects = mutableListOf<Effect>()

        // Gather matching cards from library
        effects.add(GatherCardsEffect(
            source = CardSource.FromZone(Zone.LIBRARY, Player.You, filter),
            storeAs = "searchable"
        ))

        // Player selects up to `count`
        effects.add(SelectFromCollectionEffect(
            from = "searchable",
            selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(count)),
            storeSelected = "found"
        ))

        // For TOP_OF_LIBRARY: shuffle first, then put on top
        if (destination == SearchDestination.TOP_OF_LIBRARY) {
            if (shuffleAfter) effects.add(ShuffleLibraryEffect())
            effects.add(MoveCollectionEffect(
                from = "found",
                destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top),
                revealed = reveal
            ))
        } else {
            // Move to destination
            val (zone, placement) = when (destination) {
                SearchDestination.HAND -> Zone.HAND to ZonePlacement.Default
                SearchDestination.BATTLEFIELD -> Zone.BATTLEFIELD to
                    if (entersTapped) ZonePlacement.Tapped else ZonePlacement.Default
                SearchDestination.GRAVEYARD -> Zone.GRAVEYARD to ZonePlacement.Default
                else -> error("unreachable")
            }
            effects.add(MoveCollectionEffect(
                from = "found",
                destination = CardDestination.ToZone(zone, placement = placement),
                revealed = reveal
            ))
            if (shuffleAfter) effects.add(ShuffleLibraryEffect())
        }

        return CompositeEffect(effects)
    }

    /**
     * Look at the top N cards of target player's library, put some in their graveyard,
     * rest on top of their library in any order.
     *
     * The target player is resolved from the spell's target via ContextPlayer(0).
     *
     * Example: Cruel Fate — "Look at the top five cards of target opponent's library.
     * Put one of them into that player's graveyard and the rest on top of their
     * library in any order."
     * ```kotlin
     * spell {
     *     target = TargetOpponent()
     *     effect = EffectPatterns.lookAtTargetLibraryAndDiscard(count = 5, toGraveyard = 1)
     * }
     * ```
     *
     * @param count How many cards to look at from the top
     * @param toGraveyard How many cards must be put into the graveyard
     */
    fun lookAtTargetLibraryAndDiscard(
        count: Int,
        toGraveyard: Int = 1
    ): CompositeEffect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(count), Player.ContextPlayer(0)),
                storeAs = "looked"
            ),
            SelectFromCollectionEffect(
                from = "looked",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(toGraveyard)),
                storeSelected = "toGraveyard",
                storeRemainder = "toTop"
            ),
            MoveCollectionEffect(
                from = "toGraveyard",
                destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0))
            ),
            MoveCollectionEffect(
                from = "toTop",
                destination = CardDestination.ToZone(Zone.LIBRARY, Player.ContextPlayer(0), ZonePlacement.Top),
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
    // =========================================================================
    // Reveal Until Patterns
    // =========================================================================

    /**
     * Reveal cards from the top of your library until you reveal a nonland card.
     * Deal damage equal to that card's mana value to a target.
     * Put all revealed cards on the bottom of your library in any order.
     *
     * Used for Erratic Explosion.
     *
     * @param target The target to deal damage to
     */
    fun revealUntilNonlandDealDamage(target: EffectTarget): CompositeEffect = CompositeEffect(
        listOf(
            // Step 1: Reveal cards until a nonland is found
            RevealUntilEffect(
                matchFilter = GameObjectFilter.Nonland,
                storeMatch = "nonland",
                storeRevealed = "allRevealed"
            ),
            // Step 2: Deal damage equal to the nonland card's mana value
            DealDamageEffect(
                amount = DynamicAmount.StoredCardManaValue("nonland"),
                target = target
            ),
            // Step 3: Put all revealed cards on the bottom in any order
            MoveCollectionEffect(
                from = "allRevealed",
                destination = CardDestination.ToZone(
                    com.wingedsheep.sdk.core.Zone.LIBRARY,
                    placement = ZonePlacement.Bottom
                ),
                order = CardOrder.ControllerChooses
            )
        )
    )

    /**
     * For each target, reveal cards from the top of your library until you reveal
     * a nonland card. Deal damage equal to that card's mana value to that target.
     * Put all revealed cards on the bottom of your library in any order.
     *
     * Used for Kaboom!
     */
    fun revealUntilNonlandDealDamageEachTarget(): ForEachTargetEffect = ForEachTargetEffect(
        listOf(
            RevealUntilEffect(
                matchFilter = GameObjectFilter.Nonland,
                storeMatch = "nonland",
                storeRevealed = "allRevealed"
            ),
            DealDamageEffect(
                amount = DynamicAmount.StoredCardManaValue("nonland"),
                target = EffectTarget.ContextTarget(0)
            ),
            MoveCollectionEffect(
                from = "allRevealed",
                destination = CardDestination.ToZone(
                    com.wingedsheep.sdk.core.Zone.LIBRARY,
                    placement = ZonePlacement.Bottom
                ),
                order = CardOrder.ControllerChooses
            )
        )
    )

    /**
     * Reveal cards from the top of your library until you reveal a nonland card.
     * This creature gets +X/+0 until end of turn, where X is that card's mana value.
     * Put the revealed cards on the bottom of your library in any order.
     *
     * Used for Goblin Machinist.
     */
    fun revealUntilNonlandModifyStats(): CompositeEffect = CompositeEffect(
        listOf(
            // Step 1: Reveal cards until a nonland is found
            RevealUntilEffect(
                matchFilter = GameObjectFilter.Nonland,
                storeMatch = "nonland",
                storeRevealed = "allRevealed"
            ),
            // Step 2: Buff self +X/+0 where X is the nonland's mana value
            DynamicModifyStatsEffect(
                powerModifier = DynamicAmount.StoredCardManaValue("nonland"),
                toughnessModifier = DynamicAmount.Fixed(0),
                target = EffectTarget.Self
            ),
            // Step 3: Put all revealed cards on the bottom in any order
            MoveCollectionEffect(
                from = "allRevealed",
                destination = CardDestination.ToZone(
                    Zone.LIBRARY,
                    placement = ZonePlacement.Bottom
                ),
                order = CardOrder.ControllerChooses
            )
        )
    )

    /**
     * Choose a creature type. Reveal cards from the top of your library until you
     * reveal a creature card of that type. Put that card onto the battlefield and
     * shuffle the rest into your library.
     *
     * Used for Riptide Shapeshifter.
     */
    fun revealUntilCreatureTypeToBattlefield(): CompositeEffect = CompositeEffect(
        listOf(
            // Step 1: Choose a creature type
            ChooseCreatureTypeEffect,
            // Step 2: Reveal cards until a creature of the chosen type is found
            RevealUntilEffect(
                matchFilter = GameObjectFilter.Creature,
                storeMatch = "found",
                storeRevealed = "allRevealed",
                matchChosenCreatureType = true
            ),
            // Step 3: Put the matched creature onto the battlefield
            MoveCollectionEffect(
                from = "found",
                destination = CardDestination.ToZone(com.wingedsheep.sdk.core.Zone.BATTLEFIELD)
            ),
            // Step 4: Shuffle the rest back into the library
            ShuffleLibraryEffect()
        )
    )

    fun exileUntilLeaves(
        exileTarget: EffectTarget,
        variableName: String = "exiledCard"
    ): StoreResultEffect = StoreResultEffect(
        effect = MoveToZoneEffect(exileTarget, Zone.EXILE),
        storeAs = EffectVariable.EntityRef(variableName)
    )

    /**
     * Mill N — Put the top N cards of a player's library into their graveyard.
     *
     * Creates a Gather → Move pipeline.
     *
     * Example: "Mill 3" or "Target player mills 3 cards"
     * ```kotlin
     * Effects.Mill(3)
     * Effects.Mill(3, EffectTarget.ContextTarget(0))
     * ```
     *
     * @param count How many cards to mill
     * @param target Who gets milled (default: controller)
     */
    fun mill(count: Int, target: EffectTarget = EffectTarget.Controller): CompositeEffect {
        val player = when (target) {
            EffectTarget.Controller -> Player.You
            is EffectTarget.ContextTarget -> Player.ContextPlayer(target.index)
            is EffectTarget.PlayerRef -> target.player
            else -> Player.You
        }
        return CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(count), player),
                    storeAs = "milled"
                ),
                MoveCollectionEffect(
                    from = "milled",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, player)
                )
            )
        )
    }

    /**
     * Look at the top X cards of your library, put any number matching a filter
     * onto the battlefield, then shuffle the rest back.
     *
     * Creates a Gather → Select → Move → Shuffle pipeline.
     *
     * Example: Ajani's ultimate — "Look at the top X cards of your library,
     * where X is your life total. You may put any number of nonland permanent cards
     * with mana value 3 or less from among them onto the battlefield. Then shuffle."
     * ```kotlin
     * EffectPatterns.lookAtTopXAndPutOntoBattlefield(
     *     countSource = DynamicAmount.YourLifeTotal,
     *     filter = GameObjectFilter.NonlandPermanent.manaValueAtMost(3)
     * )
     * ```
     *
     * @param countSource Dynamic amount determining how many cards to look at
     * @param filter Which cards may be put onto the battlefield
     * @param shuffleAfter Whether to shuffle the library after (default: true)
     */
    fun lookAtTopXAndPutOntoBattlefield(
        countSource: DynamicAmount,
        filter: GameObjectFilter,
        shuffleAfter: Boolean = true
    ): CompositeEffect {
        val restPlacement = if (shuffleAfter) ZonePlacement.Shuffled else ZonePlacement.Default
        return CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(countSource),
                    storeAs = "looked"
                ),
                SelectFromCollectionEffect(
                    from = "looked",
                    selection = SelectionMode.ChooseUpTo(countSource),
                    filter = filter,
                    storeSelected = "toBattlefield",
                    storeRemainder = "rest"
                ),
                MoveCollectionEffect(
                    from = "toBattlefield",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                ),
                MoveCollectionEffect(
                    from = "rest",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = restPlacement)
                )
            )
        )
    }

    /**
     * Reveal the top N cards of your library. An opponent chooses a card matching
     * the filter from among them. Put that card onto the battlefield and the rest
     * into your graveyard.
     *
     * Creates a Gather (revealed) → Select (OpponentChooses with filter) → Move pipeline.
     *
     * Example: Animal Magnetism — "Reveal the top five cards of your library.
     * An opponent chooses a creature card from among them. Put that card onto
     * the battlefield and the rest into your graveyard."
     * ```kotlin
     * EffectPatterns.revealAndOpponentChooses(count = 5, filter = GameObjectFilter.Creature)
     * ```
     *
     * @param count How many cards to reveal from the top of the library
     * @param filter Which cards the opponent can choose from (e.g., Creature)
     */
    /**
     * Wheel effect — each affected player shuffles their hand into their library,
     * then draws that many cards.
     *
     * Creates a ForEachPlayer → Gather → Move(Shuffled) → Draw pipeline.
     *
     * Used for Winds of Change, Wheel of Fortune-style effects.
     *
     * @param players Which players are affected (default: Player.Each)
     */
    fun wheelEffect(players: Player = Player.Each): ForEachPlayerEffect = ForEachPlayerEffect(
        players = players,
        effects = listOf(
            GatherCardsEffect(CardSource.FromZone(Zone.HAND, Player.You), storeAs = "wheelHand"),
            MoveCollectionEffect("wheelHand", CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Shuffled)),
            DrawCardsEffect(DynamicAmount.VariableReference("wheelHand_count"))
        )
    )

    fun revealAndOpponentChooses(
        count: Int,
        filter: GameObjectFilter
    ): CompositeEffect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(count)),
                storeAs = "revealed",
                revealed = true
            ),
            SelectFromCollectionEffect(
                from = "revealed",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                chooser = Chooser.Opponent,
                filter = filter,
                storeSelected = "chosen",
                storeRemainder = "rest"
            ),
            MoveCollectionEffect(
                from = "chosen",
                destination = CardDestination.ToZone(Zone.BATTLEFIELD)
            ),
            MoveCollectionEffect(
                from = "rest",
                destination = CardDestination.ToZone(Zone.GRAVEYARD)
            )
        )
    )
}
