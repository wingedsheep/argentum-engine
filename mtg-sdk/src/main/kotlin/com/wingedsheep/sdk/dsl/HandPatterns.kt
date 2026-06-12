package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.DrawUpToEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.RepeatDynamicTimesEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Effect patterns for hand manipulation: discard, draw, loot, wheel,
 * and hand-to-zone operations.
 */
object HandPatterns {

    fun eachOpponentDiscards(count: Int, controllerDrawsPerDiscard: Int = 0): Effect {
        if (controllerDrawsPerDiscard > 0) {
            val drawCount: DynamicAmount = if (controllerDrawsPerDiscard == 1) {
                DynamicAmount.VariableReference("discarded_count")
            } else {
                DynamicAmount.Multiply(DynamicAmount.VariableReference("discarded_count"), controllerDrawsPerDiscard)
            }
            return CompositeEffect(listOf(
                GatherCardsEffect(
                    // TODO(multiplayer Phase 1, backlog/multiplayer.md): "each opponent discards,
                    //  you draw per discard" needs cross-iteration count accumulation; until then
                    //  this hits one opponent (identical in two-player games).
                    source = CardSource.FromZone(Zone.HAND, Player.AnOpponent),
                    storeAs = "hand"
                ),
                SelectFromCollectionEffect(
                    from = "hand",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(count)),
                    chooser = Chooser.Opponent,
                    storeSelected = "discarded",
                    prompt = "Choose ${if (count == 1) "a card" else "$count cards"} to discard"
                ),
                MoveCollectionEffect(
                    from = "discarded",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, player = Player.AnOpponent),
                    moveType = MoveType.Discard
                ),
                DrawCardsEffect(count = drawCount)
            ))
        }

        return ForEachPlayerEffect(
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
    }

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
                    storeSelected = "discarded",
                    prompt = "Choose $count card${if (count != 1) "s" else ""} to discard"
                ),
                MoveCollectionEffect(
                    from = "discarded",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, player),
                    moveType = MoveType.Discard
                )
            )
        )
    }

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

    fun eachOpponentMayPutFromHand(filter: GameObjectFilter = GameObjectFilter.Any): ForEachEffect =
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

    fun wheelEffect(players: Player = Player.Each): ForEachEffect = ForEachPlayerEffect(
        players = players,
        effects = listOf(
            GatherCardsEffect(CardSource.FromZone(Zone.HAND, Player.You), storeAs = "wheelHand"),
            MoveCollectionEffect("wheelHand", CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Shuffled)),
            DrawCardsEffect(DynamicAmount.VariableReference("wheelHand_count"))
        )
    )

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

    fun eachPlayerDrawsX(
        includeController: Boolean = true,
        includeOpponents: Boolean = true
    ): ForEachEffect {
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

    fun eachPlayerMayDraw(maxCards: Int, lifePerCardNotDrawn: Int = 0): ForEachEffect {
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

    fun loot(draw: Int = 1, discard: Int = 1): CompositeEffect = CompositeEffect(
        listOf(
            DrawCardsEffect(draw, EffectTarget.Controller),
            discardCards(discard)
        )
    )

    /**
     * Read the Runes-style "draw X, then for each card drawn discard a card unless
     * you sacrifice a permanent" pipeline. Loops X times via [RepeatDynamicTimesEffect]
     * (iteration count = the X paid for the spell); each iteration presents a
     * [ChooseActionEffect] whose feasibility checks auto-skip the sacrifice option
     * when no permanents are controlled and the discard option when the hand is
     * empty, with the choice itself auto-resolving when only one of the two is feasible.
     */
    fun readTheRunes(): CompositeEffect = CompositeEffect(
        listOf(
            DrawCardsEffect(DynamicAmount.XValue, EffectTarget.Controller),
            RepeatDynamicTimesEffect(
                amount = DynamicAmount.XValue,
                body = ChooseActionEffect(
                    choices = listOf(
                        EffectChoice(
                            label = "Sacrifice a permanent",
                            effect = CompositeEffect(
                                listOf(
                                    GatherCardsEffect(
                                        source = CardSource.ControlledPermanents(Player.You),
                                        storeAs = "rtr_perms"
                                    ),
                                    SelectFromCollectionEffect(
                                        from = "rtr_perms",
                                        selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                                        chooser = Chooser.Controller,
                                        storeSelected = "rtr_sacrificed",
                                        prompt = "Choose a permanent to sacrifice",
                                        useTargetingUI = true
                                    ),
                                    MoveCollectionEffect(
                                        from = "rtr_sacrificed",
                                        destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.You),
                                        moveType = MoveType.Sacrifice
                                    )
                                )
                            ),
                            feasibilityCheck = FeasibilityCheck.ControlsPermanentMatching(GameObjectFilter.Permanent)
                        ),
                        EffectChoice(
                            label = "Discard a card",
                            effect = discardCards(1, EffectTarget.Controller),
                            feasibilityCheck = FeasibilityCheck.HasCardsInZone(Zone.HAND)
                        )
                    )
                )
            )
        ),
        descriptionOverride = "Draw X cards. For each card drawn this way, discard a card unless you sacrifice a permanent."
    )

    /**
     * Connive (CR 702.166): draw a card, then discard a card. If the discarded card
     * is a nonland, put a +1/+1 counter on [target].
     *
     * Pipeline: Draw → Gather(hand) → Select(1) → Move(Discard) → ConditionalOnCollection(Nonland).
     * SelectFromCollection auto-resolves on empty / single-card hands, matching the
     * old monolithic executor's short-circuit behavior.
     */
    fun connive(target: EffectTarget = EffectTarget.Self): CompositeEffect = CompositeEffect(
        listOf(
            DrawCardsEffect(1, EffectTarget.Controller),
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.You),
                storeAs = "connive_hand"
            ),
            SelectFromCollectionEffect(
                from = "connive_hand",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                chooser = Chooser.Controller,
                storeSelected = "connive_discarded",
                prompt = "Choose a card to discard"
            ),
            MoveCollectionEffect(
                from = "connive_discarded",
                destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.You),
                moveType = MoveType.Discard
            ),
            ConditionalOnCollectionEffect(
                collection = "connive_discarded",
                filter = GameObjectFilter.Nonland,
                ifNotEmpty = AddCountersEffect(
                    counterType = Counters.PLUS_ONE_PLUS_ONE,
                    count = 1,
                    target = target
                )
            )
        ),
        descriptionOverride = "Connive"
    )

    /**
     * Rummage: "Discard a card. If you do, draw a card." — discard first, then draw
     * only as many cards as were actually discarded. When the hand is empty the
     * discard resolves to zero and no card is drawn.
     */
    fun rummage(count: Int = 1): CompositeEffect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.You),
                storeAs = "hand"
            ),
            SelectFromCollectionEffect(
                from = "hand",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(count)),
                chooser = Chooser.Controller,
                storeSelected = "discarded",
                prompt = "Choose a card to discard"
            ),
            MoveCollectionEffect(
                from = "discarded",
                destination = CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = MoveType.Discard
            ),
            DrawCardsEffect(
                count = DynamicAmount.VariableReference("discarded_count"),
                target = EffectTarget.Controller
            )
        )
    )

    /**
     * Target player exiles cards from their hand.
     * "Target opponent exiles a card from their hand."
     */
    fun exileFromHand(count: Int = 1, target: EffectTarget = EffectTarget.ContextTarget(0)): CompositeEffect {
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
                    storeSelected = "exiled",
                    prompt = "Choose ${if (count == 1) "a card" else "$count cards"} to exile"
                ),
                MoveCollectionEffect(
                    from = "exiled",
                    destination = CardDestination.ToZone(Zone.EXILE, player)
                )
            )
        )
    }

}
