package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ChoosePileEffect
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.EmitScriedEventEffect
import com.wingedsheep.sdk.scripting.effects.EmitSurveiledEventEffect
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GatherUntilMatchEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.effects.ScryEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.effects.SurveilEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Effect patterns for library manipulation: search, scry, surveil, mill,
 * reveal-until, look-at-top, shuffle-graveyard, and reorder operations.
 */
object LibraryPatterns {

    fun lookAtTopAndKeep(
        count: Int,
        keepCount: Int,
        keepDestination: CardDestination = CardDestination.ToZone(Zone.HAND),
        restDestination: CardDestination = CardDestination.ToZone(Zone.GRAVEYARD),
        revealed: Boolean = false
    ): CompositeEffect = lookAtTopAndKeep(
        count = DynamicAmount.Fixed(count),
        keepCount = DynamicAmount.Fixed(keepCount),
        keepDestination = keepDestination,
        restDestination = restDestination,
        revealed = revealed
    )

    /**
     * "Look at the top [count], put [keepCount] in [keepDestination], rest to [restDestination]"
     * with optional [restOrder] (e.g. [CardOrder.Random] for "in a random order").
     *
     * Labels are auto-derived from the destinations; override [selectedLabel]/[remainderLabel]
     * if a card's oracle text uses different wording.
     */
    fun lookAtTopAndKeep(
        count: DynamicAmount,
        keepCount: DynamicAmount,
        keepDestination: CardDestination = CardDestination.ToZone(Zone.HAND),
        restDestination: CardDestination = CardDestination.ToZone(Zone.GRAVEYARD),
        revealed: Boolean = false,
        restOrder: CardOrder = CardOrder.Preserve,
        selectedLabel: String = defaultDestinationLabel(keepDestination),
        remainderLabel: String = defaultDestinationLabel(restDestination)
    ): CompositeEffect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(count),
                storeAs = "looked",
                revealed = revealed
            ),
            SelectFromCollectionEffect(
                from = "looked",
                selection = SelectionMode.ChooseExactly(keepCount),
                storeSelected = "kept",
                storeRemainder = "rest",
                selectedLabel = selectedLabel,
                remainderLabel = remainderLabel
            ),
            MoveCollectionEffect(
                from = "kept",
                destination = keepDestination
            ),
            MoveCollectionEffect(
                from = "rest",
                destination = restDestination,
                order = restOrder
            )
        )
    )

    /**
     * Manifest the top [count] cards of the library (CR 701.40): each is put onto the battlefield
     * face down as a 2/2 creature. Manifested cards are turned face up by paying their mana cost,
     * only if the card is a creature card (the engine derives that turn-up cost at entry).
     *
     * Per CR 701.40e the cards are manifested one at a time — the move executor loops per card, so
     * each becomes its own face-down permanent.
     */
    fun manifest(count: DynamicAmount = DynamicAmount.Fixed(1)): CompositeEffect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(count),
                storeAs = "manifested",
                revealed = false
            ),
            MoveCollectionEffect(
                from = "manifested",
                destination = CardDestination.ToZone(Zone.BATTLEFIELD),
                faceDown = FaceDownMode.MANIFEST
            )
        )
    )

    /**
     * "Manifest dread" (CR 701.62): look at the top two cards of your library, manifest one of
     * them (your choice), then put the other into your graveyard.
     *
     * The player looks at both cards privately, chooses which one to manifest (it becomes a
     * face-down 2/2), and the remainder is put into the graveyard. With fewer than two cards in
     * the library the gather simply yields what's there (one card → manifest it, nothing to the
     * graveyard; empty library → nothing happens).
     */
    fun manifestDread(): CompositeEffect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(2)),
                storeAs = "manifestDreadLooked",
                revealed = false
            ),
            SelectFromCollectionEffect(
                from = "manifestDreadLooked",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                storeSelected = "manifestDreadManifested",
                storeRemainder = "manifestDreadGraveyard",
                selectedLabel = "Manifest (put onto the battlefield face down)",
                remainderLabel = "Put into your graveyard"
            ),
            MoveCollectionEffect(
                from = "manifestDreadManifested",
                destination = CardDestination.ToZone(Zone.BATTLEFIELD),
                faceDown = FaceDownMode.MANIFEST
            ),
            MoveCollectionEffect(
                from = "manifestDreadGraveyard",
                destination = CardDestination.ToZone(Zone.GRAVEYARD)
            )
        )
    )

    private fun defaultDestinationLabel(destination: CardDestination): String = when (destination) {
        is CardDestination.ToZone -> when (destination.zone) {
            Zone.HAND -> "Put in hand"
            Zone.GRAVEYARD -> "Put in graveyard"
            Zone.EXILE -> "Exile"
            Zone.LIBRARY -> when (destination.placement) {
                ZonePlacement.Bottom -> "Put on bottom"
                ZonePlacement.Top -> "Put on top"
                else -> "Put in library"
            }
            Zone.BATTLEFIELD -> "Put onto the battlefield"
            else -> "Move"
        }
    }

    /**
     * "Look at the top [count] cards of your library. You may reveal a card matching [filter] from
     * among them and put it into your hand. Put the rest [restDestination] (defaults to the bottom
     * of your library) [restOrder] (defaults to a random order)." — Radagast the Brown / Star
     * Charter shape. The reveal is optional ([SelectionMode.ChooseUpTo] of 1), filtered, and the
     * selected card is revealed as it moves to hand.
     */
    fun lookAtTopRevealMatchingToHand(
        count: DynamicAmount,
        filter: GameObjectFilter,
        prompt: String,
        restDestination: CardDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
        restOrder: CardOrder = CardOrder.Random
    ): CompositeEffect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(count),
                storeAs = "looked"
            ),
            SelectFromCollectionEffect(
                from = "looked",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                filter = filter,
                storeSelected = "kept",
                storeRemainder = "rest",
                prompt = prompt,
                showAllCards = true
            ),
            MoveCollectionEffect(
                from = "kept",
                destination = CardDestination.ToZone(Zone.HAND),
                revealed = true
            ),
            MoveCollectionEffect(
                from = "rest",
                destination = restDestination,
                order = restOrder
            )
        )
    )

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
     * "Scry [count]" (CR 701.18). Returns the compact [ScryEffect] macro node; the engine expands
     * it to [scryPipeline] at execution time. A card whose effect *is* scry therefore serializes as
     * a single `{"type":"Scry"}` node rather than the unrolled pipeline (see [ScryEffect]).
     */
    fun scry(count: Int): ScryEffect = ScryEffect(count)

    /**
     * "Surveil [count]" (CR 701.42). Returns the compact [SurveilEffect] macro node; the engine
     * expands it to [surveilPipeline] at execution time (see [SurveilEffect]).
     */
    fun surveil(count: Int): SurveilEffect = SurveilEffect(count)

    /**
     * Expand a library *macro effect* ([ScryEffect] / [SurveilEffect]) to its underlying
     * Gather → Select → Move pipeline, or return `null` if [effect] is not a library macro.
     *
     * Single source of truth for the macro → pipeline mapping. The engine's `ScryExecutor` /
     * `SurveilExecutor` use it to execute the macro, and any effect-tree walker that needs to see
     * the inner gather/select nodes (e.g. `TriggerProcessor`'s selection-amount probe) expands
     * through here rather than re-deriving the pipeline.
     */
    fun expandMacro(effect: Effect): CompositeEffect? = when (effect) {
        is ScryEffect -> scryPipeline(effect.count)
        is SurveilEffect -> surveilPipeline(effect.count)
        else -> null
    }

    /**
     * The expanded scry pipeline (Gather → Select → Move top/bottom → emit `ScriedEvent`). Public
     * so the engine's scry macro executor can build and delegate to it; card definitions should use
     * [scry] / [com.wingedsheep.sdk.dsl.Effects.Scry] instead.
     */
    fun scryPipeline(count: Int): CompositeEffect = CompositeEffect(
        listOfNotNull(
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
            ),
            // Fire "Whenever you scry" triggers (CR 701.18) after the pipeline finishes.
            // The event count is the actual size of the "scried" gather collection at
            // resolution time, not the literal N (handles library-smaller-than-N). Per
            // CR 701.18d the trigger still fires when the library was empty and zero
            // cards were looked at, so the tail emits unconditionally — it is only
            // omitted for a literal "scry 0" (CR 701.18b: no scry event occurs).
            if (count > 0) EmitScriedEventEffect() else null
        )
    )

    /**
     * The expanded surveil pipeline (Gather → Select → Move graveyard/top → emit `SurveiledEvent`).
     * Public so the engine's surveil macro executor can build and delegate to it; card definitions
     * should use [surveil] / [com.wingedsheep.sdk.dsl.Effects.Surveil] instead.
     */
    fun surveilPipeline(count: Int): CompositeEffect = CompositeEffect(
        listOfNotNull(
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
            ),
            // Fire "Whenever you surveil" / "scry or surveil" triggers (CR 701.42) after the
            // pipeline finishes. The event count is the actual size of the "surveiled" gather
            // collection at resolution time, not the literal N (handles library-smaller-than-N).
            // Per CR 701.42d the trigger still fires when the library was empty and zero cards
            // were looked at, so the tail emits unconditionally — it is only omitted for a literal
            // "surveil 0" (CR 701.42c: no surveil event occurs).
            if (count > 0) EmitSurveiledEventEffect() else null
        )
    )

    fun searchLibrary(
        filter: GameObjectFilter = GameObjectFilter.Any,
        count: Int = 1,
        destination: SearchDestination = SearchDestination.HAND,
        entersTapped: Boolean = false,
        shuffleAfter: Boolean = true,
        reveal: Boolean = false
    ): CompositeEffect = searchLibrary(filter, DynamicAmount.Fixed(count), destination, entersTapped, shuffleAfter, reveal)

    fun searchLibrary(
        filter: GameObjectFilter = GameObjectFilter.Any,
        count: DynamicAmount,
        destination: SearchDestination = SearchDestination.HAND,
        entersTapped: Boolean = false,
        shuffleAfter: Boolean = true,
        reveal: Boolean = false
    ): CompositeEffect {
        val effects = mutableListOf<Effect>()

        effects.add(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.LIBRARY, Player.You, filter),
                storeAs = "searchable"
            )
        )

        effects.add(
            SelectFromCollectionEffect(
                from = "searchable",
                selection = SelectionMode.ChooseUpTo(count),
                storeSelected = "found"
            )
        )

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
            val (zone, placement) = when (destination) {
                SearchDestination.HAND -> Zone.HAND to ZonePlacement.Default
                SearchDestination.BATTLEFIELD -> Zone.BATTLEFIELD to
                    if (entersTapped) ZonePlacement.Tapped else ZonePlacement.Default
                SearchDestination.GRAVEYARD -> Zone.GRAVEYARD to ZonePlacement.Default
                SearchDestination.TOP_OF_LIBRARY -> error("unreachable")
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

    fun searchMultipleZones(
        zones: List<Zone>,
        filter: GameObjectFilter = GameObjectFilter.Any,
        count: Int = 1,
        destination: SearchDestination = SearchDestination.BATTLEFIELD,
        entersTapped: Boolean = false
    ): CompositeEffect {
        val effects = mutableListOf<Effect>()

        effects.add(
            GatherCardsEffect(
                source = CardSource.FromMultipleZones(zones, Player.You, filter),
                storeAs = "searchable"
            )
        )

        effects.add(
            SelectFromCollectionEffect(
                from = "searchable",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(count)),
                storeSelected = "found"
            )
        )

        val (zone, placement) = when (destination) {
            SearchDestination.HAND -> Zone.HAND to ZonePlacement.Default
            SearchDestination.BATTLEFIELD -> Zone.BATTLEFIELD to
                if (entersTapped) ZonePlacement.Tapped else ZonePlacement.Default
            SearchDestination.GRAVEYARD -> Zone.GRAVEYARD to ZonePlacement.Default
            SearchDestination.TOP_OF_LIBRARY -> Zone.LIBRARY to ZonePlacement.Top
        }

        effects.add(
            MoveCollectionEffect(
                from = "found",
                destination = CardDestination.ToZone(zone, placement = placement)
            )
        )

        if (zones.contains(Zone.LIBRARY)) {
            effects.add(ShuffleLibraryEffect())
        }

        return CompositeEffect(effects)
    }

    fun revealUntilNonlandDealDamage(target: EffectTarget): CompositeEffect = CompositeEffect(
        listOf(
            GatherUntilMatchEffect(
                filter = GameObjectFilter.Nonland,
                storeMatch = "nonland",
                storeRevealed = "allRevealed"
            ),
            RevealCollectionEffect(from = "allRevealed"),
            DealDamageEffect(
                amount = DynamicAmount.StoredCardManaValue("nonland"),
                target = target
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

    fun revealUntilNonlandDealDamageEachTarget(): ForEachEffect = ForEachTargetEffect(
        listOf(
            GatherUntilMatchEffect(
                filter = GameObjectFilter.Nonland,
                storeMatch = "nonland",
                storeRevealed = "allRevealed"
            ),
            RevealCollectionEffect(from = "allRevealed"),
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
     * "Divvy" pattern (Fact or Fiction shape, CR 700.3): reveal cards from the top
     * of the controller's library, **an opponent partitions them into two piles**,
     * then **the controller chooses which pile is the "keep" pile**; that pile
     * goes to [keepZone] and the other to [otherZone].
     *
     * Two pauses, two players:
     *  1. Opponent — `SelectFromCollection` over `revealed` → `pileA` + `pileB`.
     *     Empty piles are legal (CR 700.3d).
     *  2. Controller — `ChoosePile` between `pileA` and `pileB` → `keepPile` +
     *     `otherPile`.
     *
     * Then `MoveCollection` routes each pile to its zone.
     */
    fun factOrFiction(
        count: Int = 5,
        keepZone: Zone = Zone.HAND,
        otherZone: Zone = Zone.GRAVEYARD,
        keepLabel: String = "Hand",
        otherLabel: String = "Graveyard"
    ): CompositeEffect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(count)),
                storeAs = "revealed",
                revealed = true
            ),
            SelectFromCollectionEffect(
                from = "revealed",
                selection = SelectionMode.ChooseAnyNumber,
                chooser = Chooser.Opponent,
                storeSelected = "pileA",
                storeRemainder = "pileB",
                selectedLabel = "Pile 1",
                remainderLabel = "Pile 2",
                prompt = "Separate the revealed cards into two piles. The cards you select form Pile 1; the rest form Pile 2.",
                alwaysPrompt = true
            ),
            ChoosePileEffect(
                pileA = "pileA",
                pileB = "pileB",
                pileALabel = "Pile 1",
                pileBLabel = "Pile 2",
                chooser = Chooser.Controller,
                storeChosenAs = "keepPile",
                storeOtherAs = "otherPile",
                prompt = "Choose which pile goes to your $keepLabel; the other goes to your $otherLabel."
            ),
            MoveCollectionEffect(
                from = "keepPile",
                destination = CardDestination.ToZone(keepZone)
            ),
            MoveCollectionEffect(
                from = "otherPile",
                destination = CardDestination.ToZone(otherZone)
            )
        )
    )

    fun mill(count: Int, target: EffectTarget = EffectTarget.Controller): CompositeEffect =
        mill(DynamicAmount.Fixed(count), target)

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
     * "Each player searches their library for [count] card(s) matching [filter], reveals them,
     * puts them into their hand, then shuffles." Per-player Gather → Select → Move → Shuffle.
     */
    fun eachPlayerSearchesLibrary(
        filter: GameObjectFilter,
        count: DynamicAmount
    ): ForEachEffect = ForEachPlayerEffect(
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
}
