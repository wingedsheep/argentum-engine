package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ChooseCreatureTypeEffect
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.DrawUpToEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.GrantPlayWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.RevealUntilEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.effects.StoreCountEffect
import com.wingedsheep.sdk.scripting.effects.StoreResultEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EffectVariable

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
     *     ifNotPaid = Effects.Discard(1)
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
     * Each opponent discards N cards.
     *
     * Composed as ForEachPlayer(EachOpponent) → Gather(hand) → Select(ChooseExactly(N)) → Move(graveyard, Discard).
     * Handles the MTG rule "as much as possible": if an opponent has fewer than N cards, they discard all of them.
     *
     * For Syphon Mind-style "you draw for each card discarded", use EachOpponentDiscardsEffect directly.
     *
     * Example:
     * ```kotlin
     * eachOpponentDiscards(1)
     * // -> "Each opponent discards a card."
     * ```
     */
    fun eachOpponentDiscards(count: Int): ForEachPlayerEffect = ForEachPlayerEffect(
        players = Player.EachOpponent,
        effects = listOf(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.You),
                storeAs = "hand"
            ),
            SelectFromCollectionEffect(
                from = "hand",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(count)),
                storeSelected = "discarded"
            ),
            MoveCollectionEffect(
                from = "discarded",
                destination = CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = MoveType.Discard
            )
        )
    )

    /**
     * Controller discards N cards of their choice.
     *
     * Composed as Gather(hand) → Select(ChooseExactly(N)) → Move(graveyard, Discard).
     * Handles the MTG "as much as possible" rule: if hand has fewer than N cards, discard all.
     *
     * Example:
     * ```kotlin
     * discardCards(1)
     * // -> "Discard a card."
     * ```
     */
    fun discardCards(count: Int, target: EffectTarget = EffectTarget.Controller): CompositeEffect {
        val player = effectTargetToPlayer(target)
        val chooser = effectTargetToChooser(target)
        return CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, player),
                    storeAs = "hand"
                ),
                SelectFromCollectionEffect(
                    from = "hand",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(count)),
                    chooser = chooser,
                    storeSelected = "discarded"
                ),
                MoveCollectionEffect(
                    from = "discarded",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, player),
                    moveType = MoveType.Discard
                )
            )
        )
    }

    /**
     * Controller discards N cards at random (engine picks, no player choice).
     *
     * Composed as Gather(hand) → Select(Random(N)) → Move(graveyard, Discard).
     *
     * Example:
     * ```kotlin
     * discardRandom(1)
     * // -> "Discard a card at random."
     * ```
     */
    fun discardRandom(count: Int, target: EffectTarget = EffectTarget.Controller): CompositeEffect {
        val player = effectTargetToPlayer(target)
        return CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, player),
                    storeAs = "hand"
                ),
                SelectFromCollectionEffect(
                    from = "hand",
                    selection = SelectionMode.Random(DynamicAmount.Fixed(count)),
                    storeSelected = "discarded"
                ),
                MoveCollectionEffect(
                    from = "discarded",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, player),
                    moveType = MoveType.Discard
                )
            )
        )
    }

    /**
     * Controller may put up to [count] cards matching [filter] from their hand onto the battlefield.
     * Entering tapped if [entersTapped] is true.
     *
     * Composed as Gather(hand, filter) → Select(ChooseUpTo(count)) → Move(battlefield).
     * Selecting 0 cards is equivalent to declining (no separate yes/no prompt needed).
     *
     * Example:
     * ```kotlin
     * putFromHand(GameObjectFilter.BasicLand, entersTapped = true)
     * // -> "You may put a basic land card from your hand onto the battlefield tapped."
     * ```
     */
    fun putFromHand(
        filter: GameObjectFilter = GameObjectFilter.Any,
        count: Int = 1,
        entersTapped: Boolean = false
    ): CompositeEffect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.You, filter),
                storeAs = "put_candidates"
            ),
            SelectFromCollectionEffect(
                from = "put_candidates",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(count)),
                storeSelected = "putting"
            ),
            MoveCollectionEffect(
                from = "putting",
                destination = CardDestination.ToZone(
                    Zone.BATTLEFIELD,
                    Player.You,
                    if (entersTapped) ZonePlacement.Tapped else ZonePlacement.Default
                )
            )
        )
    )

    /**
     * Each opponent may put any number of cards matching [filter] from their hand onto the battlefield.
     *
     * Composed as ForEachPlayer(EachOpponent) → [Gather(hand, filter) → Select(ChooseAnyNumber) → Move(battlefield)].
     * Each opponent independently chooses how many (if any) matching cards to put out.
     *
     * Example:
     * ```kotlin
     * eachOpponentMayPutFromHand(GameObjectFilter.Permanent)
     * // -> "Each opponent may put any number of permanent cards from their hand onto the battlefield."
     * ```
     */
    fun eachOpponentMayPutFromHand(filter: GameObjectFilter = GameObjectFilter.Any): ForEachPlayerEffect =
        ForEachPlayerEffect(
            players = Player.EachOpponent,
            effects = listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.You, filter),
                    storeAs = "put_candidates"
                ),
                SelectFromCollectionEffect(
                    from = "put_candidates",
                    selection = SelectionMode.ChooseAnyNumber,
                    storeSelected = "putting",
                    prompt = "Choose cards to put onto the battlefield"
                ),
                MoveCollectionEffect(
                    from = "putting",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                )
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
     *     Effects.Discard(2)
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
                storeRemainder = "rest",
                selectedLabel = "Put in hand",
                remainderLabel = "Put in graveyard"
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
                storeRemainder = "toTop",
                selectedLabel = "Put on bottom",
                remainderLabel = "Put on top"
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
                storeRemainder = "toTop",
                selectedLabel = "Put in graveyard",
                remainderLabel = "Put on top"
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
        effects.add(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.LIBRARY, Player.You, filter),
                storeAs = "searchable"
            )
        )

        // Player selects up to `count`
        effects.add(
            SelectFromCollectionEffect(
                from = "searchable",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(count)),
                storeSelected = "found"
            )
        )

        // For TOP_OF_LIBRARY: shuffle first, then put on top
        if (destination == SearchDestination.TOP_OF_LIBRARY) {
            if (shuffleAfter) effects.add(ShuffleLibraryEffect())
            effects.add(
                MoveCollectionEffect(
                    from = "found",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top),
                    revealed = reveal
                )
            )
        } else {
            // Move to destination
            val (zone, placement) = when (destination) {
                SearchDestination.HAND -> Zone.HAND to ZonePlacement.Default
                SearchDestination.BATTLEFIELD -> Zone.BATTLEFIELD to
                    if (entersTapped) ZonePlacement.Tapped else ZonePlacement.Default
                SearchDestination.GRAVEYARD -> Zone.GRAVEYARD to ZonePlacement.Default
                else -> error("unreachable")
            }
            effects.add(
                MoveCollectionEffect(
                    from = "found",
                    destination = CardDestination.ToZone(zone, placement = placement),
                    revealed = reveal
                )
            )
            if (shuffleAfter) effects.add(ShuffleLibraryEffect())
        }

        return CompositeEffect(effects)
    }

    /**
     * Search your library for a card, then shuffle and put that card Nth from the top.
     *
     * Uses a purely atomic pipeline: Gather → Select → Shuffle → Move(bottom) →
     * Gather(top N-1) → Move(top) → Move(top). The found card is first parked at the
     * bottom to avoid overlapping with the top-N gather, then moved to top, then the
     * original top cards are placed back on top — pushing the found card to position N-1.
     *
     * @param filter Which cards can be found (default: any card)
     * @param positionFromTop 0-indexed position from top (2 = third from top)
     */
    fun searchLibraryNthFromTop(
        filter: GameObjectFilter = GameObjectFilter.Any,
        positionFromTop: Int = 2
    ): CompositeEffect {
        val effects = mutableListOf<Effect>(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.LIBRARY, Player.You, filter),
                storeAs = "searchable"
            ),
            SelectFromCollectionEffect(
                from = "searchable",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                storeSelected = "found"
            ),
            ShuffleLibraryEffect(),
        )

        if (positionFromTop == 0) {
            // Top of library — just move on top after shuffle
            effects.add(
                MoveCollectionEffect(
                    from = "found",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top)
                )
            )
        } else {
            // Park found card at bottom to avoid overlap with top-N gather
            effects.add(
                MoveCollectionEffect(
                    from = "found",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom)
                )
            )
            // Gather the top N cards (which won't include the found card)
            effects.add(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(positionFromTop)),
                    storeAs = "aboveCards"
                )
            )
            // Move found card to top
            effects.add(
                MoveCollectionEffect(
                    from = "found",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top)
                )
            )
            // Move the original top cards back on top, pushing found card down to positionFromTop
            effects.add(
                MoveCollectionEffect(
                    from = "aboveCards",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top)
                )
            )
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
                storeRemainder = "toTop",
                selectedLabel = "Put in graveyard",
                remainderLabel = "Put on top"
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
     * Search target player's library for up to [count] cards matching [filter] and exile them.
     * Then that player shuffles.
     *
     * The controller of the ability chooses which cards to exile (Chooser.Controller).
     * Target player is resolved from the spell/ability's first target (ContextPlayer(0)).
     *
     * Example:
     * ```kotlin
     * activatedAbility {
     *     target = Targets.Player
     *     effect = EffectPatterns.searchTargetLibraryExile(5)
     * }
     * // -> "Search target player's library for up to five cards and exile them.
     * //     Then that player shuffles."
     * ```
     */
    fun searchTargetLibraryExile(
        count: Int = 1,
        filter: GameObjectFilter = GameObjectFilter.Any
    ): CompositeEffect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.LIBRARY, Player.ContextPlayer(0), filter),
                storeAs = "searchable"
            ),
            SelectFromCollectionEffect(
                from = "searchable",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(count)),
                storeSelected = "exiled",
                chooser = Chooser.Controller
            ),
            MoveCollectionEffect(
                from = "exiled",
                destination = CardDestination.ToZone(Zone.EXILE, Player.ContextPlayer(0))
            ),
            ShuffleLibraryEffect(EffectTarget.ContextTarget(0))
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
                    Zone.LIBRARY,
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
                    Zone.LIBRARY,
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
            ModifyStatsEffect(
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
                destination = CardDestination.ToZone(Zone.BATTLEFIELD)
            ),
            // Step 4: Shuffle the rest back into the library
            ShuffleLibraryEffect()
        )
    )

    /**
     * Head Games — Target opponent puts cards from their hand on top of their library.
     * Search that player's library for that many cards. The player puts those cards
     * into their hand, then shuffles.
     *
     * Creates a Gather(hand) → Move(library top) → Gather(library) →
     * Select(ChooseUpTo(hand count)) → Move(hand) → Shuffle pipeline.
     *
     * @param target The opponent whose hand is being replaced (resolved from spell target)
     */
    fun headGames(target: EffectTarget): CompositeEffect = CompositeEffect(
        listOf(
            // Step 1: Gather opponent's hand (stores count as "opponentHand_count")
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                storeAs = "opponentHand"
            ),
            // Step 2: Move opponent's hand to top of their library
            MoveCollectionEffect(
                from = "opponentHand",
                destination = CardDestination.ToZone(Zone.LIBRARY, Player.ContextPlayer(0), ZonePlacement.Top)
            ),
            // Step 3: Gather all cards from opponent's library for searching
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.LIBRARY, Player.ContextPlayer(0)),
                storeAs = "searchable"
            ),
            // Step 4: Controller selects up to (original hand size) cards
            SelectFromCollectionEffect(
                from = "searchable",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.VariableReference("opponentHand_count")),
                chooser = Chooser.Controller,
                storeSelected = "found"
            ),
            // Step 5: Move selected cards to opponent's hand
            MoveCollectionEffect(
                from = "found",
                destination = CardDestination.ToZone(Zone.HAND, Player.ContextPlayer(0))
            ),
            // Step 6: Shuffle opponent's library
            ShuffleLibraryEffect(target)
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
    fun mill(count: Int, target: EffectTarget = EffectTarget.Controller): CompositeEffect =
        mill(DynamicAmount.Fixed(count), target)

    /**
     * Mill a dynamic number of cards.
     *
     * Example: "That player mills X cards, where X is the number of cards in their hand."
     * ```kotlin
     * Effects.Mill(DynamicAmount.Count(Player.TriggeringPlayer, Zone.HAND), EffectTarget.PlayerRef(Player.TriggeringPlayer))
     * ```
     *
     * @param count Dynamic amount of cards to mill
     * @param target Who gets milled (default: controller)
     */
    fun mill(count: DynamicAmount, target: EffectTarget = EffectTarget.Controller): CompositeEffect {
        val player = when (target) {
            EffectTarget.Controller -> Player.You
            is EffectTarget.ContextTarget -> Player.ContextPlayer(target.index)
            is EffectTarget.BoundVariable -> Player.ContextPlayer(0)
            is EffectTarget.PlayerRef -> target.player
            else -> Player.You
        }
        return CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(count, player),
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
     * Shuffle a player's graveyard into their library.
     *
     * Creates a Gather → Move(Shuffled) pipeline.
     *
     * ```kotlin
     * Effects.ShuffleGraveyardIntoLibrary(EffectTarget.ContextTarget(0))
     * ```
     *
     * @param target Whose graveyard to shuffle in (default: target player)
     */
    fun shuffleGraveyardIntoLibrary(target: EffectTarget = EffectTarget.ContextTarget(0)): CompositeEffect {
        val player = effectTargetToPlayer(target)
        return CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.GRAVEYARD, player),
                    storeAs = "graveyardCards"
                ),
                MoveCollectionEffect(
                    from = "graveyardCards",
                    destination = CardDestination.ToZone(Zone.LIBRARY, player, ZonePlacement.Shuffled)
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

    /**
     * Choose a creature type, then reveal the top card of your library.
     * If it's a creature of the chosen type, put it into your hand.
     * Otherwise, put it into your graveyard.
     *
     * Creates a ChooseCreatureType → Gather(top 1, revealed) → Select(All, creature + matchChosenType)
     * → Move(hand) → Move(graveyard) pipeline.
     *
     * Used for Bloodline Shaman.
     */
    fun chooseCreatureTypeRevealTop(): CompositeEffect = CompositeEffect(
        listOf(
            ChooseCreatureTypeEffect,
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                storeAs = "topCard",
                revealed = true
            ),
            SelectFromCollectionEffect(
                from = "topCard",
                selection = SelectionMode.All,
                filter = GameObjectFilter.Creature,
                matchChosenCreatureType = true,
                storeSelected = "matched",
                storeRemainder = "unmatched"
            ),
            MoveCollectionEffect(
                from = "matched",
                destination = CardDestination.ToZone(Zone.HAND)
            ),
            MoveCollectionEffect(
                from = "unmatched",
                destination = CardDestination.ToZone(Zone.GRAVEYARD)
            )
        )
    )

    /**
     * Each player may search their library for up to N cards matching a filter,
     * reveal those cards, put them into their hand, then shuffle.
     *
     * Creates a ForEachPlayer → searchLibrary pipeline.
     *
     * Used for Weird Harvest.
     *
     * @param filter Which cards qualify (e.g., Creature)
     * @param count How many cards each player may search for
     */
    fun eachPlayerSearchesLibrary(
        filter: GameObjectFilter,
        count: DynamicAmount
    ): ForEachPlayerEffect = ForEachPlayerEffect(
        players = Player.Each,
        effects = listOf(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.LIBRARY, Player.You, filter),
                storeAs = "searchable"
            ),
            SelectFromCollectionEffect(
                from = "searchable",
                selection = SelectionMode.ChooseUpTo(count),
                storeSelected = "found"
            ),
            MoveCollectionEffect(
                from = "found",
                destination = CardDestination.ToZone(Zone.HAND),
                revealed = true
            ),
            ShuffleLibraryEffect()
        )
    )

    /**
     * Each player discards any number of cards, then draws that many cards.
     * Optionally the controller draws additional cards.
     *
     * Creates a ForEachPlayer → Gather(hand) → Select(UpTo) → Move(graveyard, Discard)
     * → Draw(count) pipeline, followed by optional controller bonus draw.
     *
     * Used for Flux.
     *
     * @param controllerBonusDraw Extra cards the controller draws after the effect
     */
    fun eachPlayerDiscardsDraws(
        controllerBonusDraw: Int = 0
    ): CompositeEffect {
        val effects = mutableListOf<Effect>(
            ForEachPlayerEffect(
                players = Player.Each,
                effects = listOf(
                    GatherCardsEffect(
                        source = CardSource.FromZone(Zone.HAND, Player.You),
                        storeAs = "hand"
                    ),
                    SelectFromCollectionEffect(
                        from = "hand",
                        selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(100)),
                        storeSelected = "toDiscard"
                    ),
                    MoveCollectionEffect(
                        from = "toDiscard",
                        destination = CardDestination.ToZone(Zone.GRAVEYARD),
                        moveType = MoveType.Discard
                    ),
                    DrawCardsEffect(DynamicAmount.VariableReference("toDiscard_count"))
                )
            )
        )
        if (controllerBonusDraw > 0) {
            effects.add(DrawCardsEffect(controllerBonusDraw))
        }
        return CompositeEffect(effects)
    }

    /**
     * Discard hand — target player discards their entire hand.
     *
     * Creates a Gather(hand) → Move(graveyard, Discard) pipeline.
     *
     * Used for Wheel and Deal-style effects.
     *
     * @param target Which player's hand to discard (default: controller)
     */
    fun discardHand(target: EffectTarget = EffectTarget.Controller): CompositeEffect {
        val player = effectTargetToPlayer(target)
        return CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, player),
                    storeAs = "discardedHand"
                ),
                MoveCollectionEffect(
                    from = "discardedHand",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, player),
                    moveType = MoveType.Discard
                )
            )
        )
    }

    /**
     * Each player draws X cards, where X is the spell's X value.
     *
     * Creates a ForEachPlayer → DrawCards(XValue) pipeline.
     *
     * Used for Prosperity.
     *
     * @param includeController Whether the controller draws
     * @param includeOpponents Whether opponents draw
     */
    fun eachPlayerDrawsX(
        includeController: Boolean = true,
        includeOpponents: Boolean = true
    ): ForEachPlayerEffect {
        val players = when {
            includeController && includeOpponents -> Player.Each
            includeOpponents -> Player.EachOpponent
            else -> Player.You
        }
        return ForEachPlayerEffect(
            players = players,
            effects = listOf(DrawCardsEffect(DynamicAmount.XValue))
        )
    }

    /**
     * Choose a creature type (at resolution), then select up to N creature cards
     * of that type from your graveyard and return them to your hand.
     *
     * Creates a ChooseCreatureType → Gather(graveyard, creature) →
     * Select(UpTo, matchChosenType) → Move(hand) pipeline.
     *
     * Used for Aphetto Dredging.
     *
     * @param count Maximum number of cards to return
     */
    fun chooseCreatureTypeReturnFromGraveyard(
        count: Int
    ): CompositeEffect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature),
                storeAs = "graveyardCreatures"
            ),
            SelectFromCollectionEffect(
                from = "graveyardCreatures",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(count)),
                matchChosenCreatureType = true,
                storeSelected = "chosen"
            ),
            MoveCollectionEffect(
                from = "chosen",
                destination = CardDestination.ToZone(Zone.HAND)
            )
        )
    )

    /**
     * Choose a creature type (at resolution), then shuffle all creature cards
     * of that type from your graveyard into your library.
     *
     * Creates a ChooseCreatureType → Gather(graveyard, creature) →
     * Select(All, matchChosenType) → Move(library, shuffled) pipeline.
     *
     * Used for Elvish Soultiller.
     */
    fun chooseCreatureTypeShuffleGraveyardIntoLibrary(): CompositeEffect = CompositeEffect(
        listOf(
            ChooseCreatureTypeEffect,
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature),
                storeAs = "graveyardCreatures"
            ),
            SelectFromCollectionEffect(
                from = "graveyardCreatures",
                selection = SelectionMode.All,
                matchChosenCreatureType = true,
                storeSelected = "chosen"
            ),
            MoveCollectionEffect(
                from = "chosen",
                destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Shuffled)
            )
        )
    )

    /**
     * Exile a target until the beginning of the next end step.
     * "Exile [target]. Return it to the battlefield under its owner's control
     * at the beginning of the next end step."
     *
     * Composed as MoveToZoneEffect(EXILE) + CreateDelayedTriggerEffect(END, MoveToZoneEffect(BATTLEFIELD)).
     * The CreateDelayedTriggerEffect executor bakes in the concrete entity ID at creation time.
     *
     * Used by Astral Slide and similar effects.
     */
    fun exileUntilEndStep(target: EffectTarget): Effect = CompositeEffect(
        listOf(
            MoveToZoneEffect(target, Zone.EXILE),
            CreateDelayedTriggerEffect(
                step = Step.END,
                effect = MoveToZoneEffect(target, Zone.BATTLEFIELD)
            )
        )
    )

    /**
     * Each player may reveal any number of creature cards from their hand.
     * Then each player creates a token for each card they revealed this way.
     *
     * Creates a ForEachPlayer → Gather(hand, creature) → Select(ChooseAnyNumber)
     * → CreateToken(revealed_count) pipeline.
     *
     * Edge cases handled by existing pipeline:
     * - Player has no creatures: GatherCards stores empty → Select auto-resolves → tokens with count=0 is no-op
     * - APNAP order: ForEachPlayerEffect with Player.Each uses state.turnOrder
     * - Decision pausing: Pre-push/pop continuation pattern handles Select pausing per player
     *
     * Used for Kamahl's Summons.
     *
     * @param tokenPower Power of the created tokens
     * @param tokenToughness Toughness of the created tokens
     * @param tokenColors Colors of the created tokens
     * @param tokenCreatureTypes Creature types of the created tokens
     * @param tokenImageUri Optional image URI for the token artwork
     */
    fun eachPlayerRevealCreaturesCreateTokens(
        tokenPower: Int,
        tokenToughness: Int,
        tokenColors: Set<com.wingedsheep.sdk.core.Color>,
        tokenCreatureTypes: Set<String>,
        tokenImageUri: String? = null
    ): ForEachPlayerEffect = ForEachPlayerEffect(
        players = Player.Each,
        effects = listOf(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Creature),
                storeAs = "creatures"
            ),
            SelectFromCollectionEffect(
                from = "creatures",
                selection = SelectionMode.ChooseAnyNumber,
                storeSelected = "revealed",
                prompt = "You may reveal any number of creature cards from your hand"
            ),
            CreateTokenEffect(
                count = DynamicAmount.VariableReference("revealed_count"),
                power = tokenPower,
                toughness = tokenToughness,
                colors = tokenColors,
                creatureTypes = tokenCreatureTypes,
                imageUri = tokenImageUri
            )
        )
    )

    /**
     * Each player returns a permanent they control to its owner's hand.
     * Players act in APNAP order (active player first).
     *
     * Auto-selects when a player has exactly 1 permanent, skips when 0.
     * Used by Words of Wind's draw replacement effect.
     */
    fun eachPlayerReturnsPermanentToHand(): ForEachPlayerEffect = ForEachPlayerEffect(
        players = Player.ActivePlayerFirst,
        effects = listOf(
            GatherCardsEffect(
                source = CardSource.ControlledPermanents(Player.You),
                storeAs = "permanents"
            ),
            SelectFromCollectionEffect(
                from = "permanents",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                storeSelected = "chosen",
                prompt = "Choose a permanent to return to its owner's hand"
            ),
            MoveCollectionEffect(
                from = "chosen",
                destination = CardDestination.ToZone(Zone.HAND, Player.You)
            )
        )
    )

    /**
     * Each player may draw up to N cards.
     *
     * Composed as ForEachPlayer(ActivePlayerFirst) → DrawUpTo → (optionally) GainLife.
     * When [lifePerCardNotDrawn] > 0, DrawUpTo stores the cards-not-drawn count and
     * a subsequent GainLife reads it via DynamicAmount.VariableReference.
     *
     * Used for Temporary Truce.
     *
     * @param maxCards Maximum cards each player may draw
     * @param lifePerCardNotDrawn Life gained per card not drawn (0 = no life gain)
     */
    fun eachPlayerMayDraw(maxCards: Int, lifePerCardNotDrawn: Int = 0): ForEachPlayerEffect {
        val effects = mutableListOf<Effect>()
        effects.add(
            DrawUpToEffect(
                maxCards = maxCards,
                target = EffectTarget.Controller,
                storeNotDrawnAs = if (lifePerCardNotDrawn > 0) "cardsNotDrawn" else null
            )
        )
        if (lifePerCardNotDrawn > 0) {
            effects.add(
                GainLifeEffect(
                    amount = DynamicAmount.Multiply(
                        DynamicAmount.VariableReference("cardsNotDrawn"),
                        lifePerCardNotDrawn
                    ),
                    target = EffectTarget.Controller
                )
            )
        }
        return ForEachPlayerEffect(
            players = Player.ActivePlayerFirst,
            effects = effects
        )
    }

    /**
     * Translate an [EffectTarget] to a [Player] reference for use in pipeline effects.
     */
    private fun effectTargetToPlayer(target: EffectTarget): Player = when (target) {
        EffectTarget.Controller -> Player.You
        is EffectTarget.ContextTarget -> Player.ContextPlayer(target.index)
        is EffectTarget.BoundVariable -> Player.ContextPlayer(0) // Named targets resolve to first target player
        is EffectTarget.PlayerRef -> target.player
        else -> Player.You
    }

    /**
     * Translate an [EffectTarget] to a [Chooser] for [SelectFromCollectionEffect].
     * The chooser determines which player makes the selection decision.
     */
    private fun effectTargetToChooser(target: EffectTarget): Chooser = when (target) {
        EffectTarget.Controller -> Chooser.Controller
        is EffectTarget.ContextTarget -> Chooser.TargetPlayer
        is EffectTarget.BoundVariable -> Chooser.TargetPlayer
        is EffectTarget.PlayerRef -> when (target.player) {
            is Player.You -> Chooser.Controller
            is Player.Opponent, is Player.TargetOpponent, is Player.EachOpponent -> Chooser.Opponent
            is Player.TriggeringPlayer -> Chooser.TriggeringPlayer
            else -> Chooser.Controller
        }
        else -> Chooser.Controller
    }

    /**
     * Shuffle your library, then exile the top card. Until end of turn,
     * you may play that card without paying its mana cost.
     *
     * Pipeline: ShuffleLibrary → GatherCards(top 1) → MoveCollection(exile)
     *           → GrantMayPlayFromExile → GrantPlayWithoutPayingCost
     *
     * Used by Mind's Desire.
     */
    fun shuffleAndExileTopPlayFree(): Effect = CompositeEffect(listOf(
        ShuffleLibraryEffect(),
        GatherCardsEffect(
            source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
            storeAs = "exiledCard"
        ),
        MoveCollectionEffect(
            from = "exiledCard",
            destination = CardDestination.ToZone(Zone.EXILE)
        ),
        GrantMayPlayFromExileEffect("exiledCard"),
        GrantPlayWithoutPayingCostEffect("exiledCard")
    ))
}
