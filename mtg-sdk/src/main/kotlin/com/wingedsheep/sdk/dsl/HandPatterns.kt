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
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectTargetEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
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

    /**
     * "Each player discards N cards" — the symmetric twin of [eachOpponentDiscards], including the
     * spell's controller. One [ForEachPlayerEffect] iteration per player in **APNAP order**
     * ([Player.ActivePlayerFirst], per CR 101.4): `Player.You` rebinds to the iterated player, so
     * each player gathers *their own* hand, chooses their own cards, and moves them to *their own*
     * graveyard. Rankle's Prank's first mode.
     *
     * Deviation to be aware of: the iterations run one after another, so a later player's choice is
     * made after an earlier player's cards have already hit the graveyard. The rules have every
     * player choose face-down (CR 101.4a) and then discard simultaneously (CR 101.4). This matches
     * how every other symmetric hand effect in the engine already behaves.
     *
     * @param count how many cards each player discards.
     */
    fun eachPlayerDiscards(count: Int): Effect =
        ForEachPlayerEffect(
            players = Player.ActivePlayerFirst,
            effects = listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.You),
                    storeAs = "hand"
                ),
                SelectFromCollectionEffect(
                    from = "hand",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(count)),
                    storeSelected = "discarded",
                    prompt = "Choose ${if (count == 1) "a card" else "$count cards"} to discard"
                ),
                MoveCollectionEffect(
                    from = "discarded",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD),
                    moveType = MoveType.Discard
                )
            )
        )

    /**
     * "Each opponent exiles a card from their hand" — Mindleech Ghoul's exploit payoff. Mirrors
     * [eachOpponentDiscards]'s [ForEachPlayerEffect] shape: one iteration per opponent with
     * `Player.You` rebound to the iterated opponent, so each opponent gathers *their own* hand,
     * that opponent (the iteration's controller/chooser) picks the card, and it moves to *their*
     * exile. Unlike a targeted single-player exile, this hits every opponent and needs no target.
     *
     * @param count how many cards each opponent exiles (default 1).
     */
    fun eachOpponentExilesFromHand(count: Int = 1): Effect =
        ForEachPlayerEffect(
            players = Player.EachOpponent,
            effects = listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.You),
                    storeAs = "hand"
                ),
                SelectFromCollectionEffect(
                    from = "hand",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(count)),
                    storeSelected = "exiled",
                    prompt = "Choose ${if (count == 1) "a card" else "$count cards"} to exile"
                ),
                MoveCollectionEffect(
                    from = "exiled",
                    destination = CardDestination.ToZone(Zone.EXILE)
                )
            )
        )

    fun discardCards(
        count: Int,
        target: EffectTarget = EffectTarget.Controller,
        filter: GameObjectFilter = GameObjectFilter.Any,
    ): CompositeEffect =
        discardCards(
            DynamicAmount.Fixed(count),
            target,
            prompt = "Choose $count card${if (count != 1) "s" else ""} to discard",
            filter = filter,
        )

    /**
     * Discard a [DynamicAmount] of cards (e.g. "discard X cards, where X is the number of colors
     * of mana spent to cast this spell" — SOS Converge's Arcane Omens). Same Gather → Select →
     * Move pipeline as the fixed-count overload; the count is resolved at the SelectFromCollection
     * step. When [filter] is set, only matching hand cards are gathered for discard (e.g. a
     * filtered ward-discard cost — Saruman of Many Colors).
     */
    fun discardCards(
        count: DynamicAmount,
        target: EffectTarget = EffectTarget.Controller,
        prompt: String = "Choose cards to discard",
        filter: GameObjectFilter = GameObjectFilter.Any,
    ): CompositeEffect {
        val player = effectTargetToPlayer(target)
        val chooser = effectTargetToChooser(target)
        return CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, player, filter),
                    storeAs = "hand"
                ),
                SelectFromCollectionEffect(
                    from = "hand",
                    selection = SelectionMode.ChooseExactly(count),
                    chooser = chooser,
                    storeSelected = "discarded",
                    prompt = prompt
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
     * "Discard any number of cards" — the controller chooses any subset of their hand (including
     * none) to discard. The selected cards are stored under [storeAs], so the count is readable
     * downstream as `DynamicAmount.VariableReference("${storeAs}_count")` — e.g. Miasma Demon's
     * "you may discard any number of cards. When you do, up to that many target creatures each get
     * -2/-2" wires this as the [com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect]
     * action and reads `discarded_count` as the reflexive targets' `dynamicMaxCount`.
     *
     * Same Gather → Select → Move pipeline as the fixed-count [discardCards], but with
     * [SelectionMode.ChooseAnyNumber] (no minimum). [filter] restricts which hand cards are
     * eligible to discard.
     */
    fun discardAnyNumber(
        target: EffectTarget = EffectTarget.Controller,
        filter: GameObjectFilter = GameObjectFilter.Any,
        storeAs: String = "discarded",
        prompt: String = "Choose any number of cards to discard",
    ): CompositeEffect {
        val player = effectTargetToPlayer(target)
        val chooser = effectTargetToChooser(target)
        return CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, player, filter),
                    storeAs = "${storeAs}_candidates"
                ),
                SelectFromCollectionEffect(
                    from = "${storeAs}_candidates",
                    selection = SelectionMode.ChooseAnyNumber,
                    chooser = chooser,
                    storeSelected = storeAs,
                    prompt = prompt
                ),
                MoveCollectionEffect(
                    from = storeAs,
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, player),
                    moveType = MoveType.Discard
                )
            )
        )
    }

    /**
     * Discard [count] cards, or satisfy the instruction by discarding fewer cards if
     * the selection includes [requiredMatches] cards matching [unlessFilter].
     *
     * Models "discard two cards unless you discard a creature/basic land/artifact card"
     * as one card-selection decision instead of a prior modal choice.
     */
    fun discardCardsUnlessMatching(
        count: Int,
        unlessFilter: GameObjectFilter,
        target: EffectTarget = EffectTarget.Controller,
        reducedCount: Int = 1,
        requiredMatches: Int = 1,
        prompt: String = "Choose $count cards to discard, or $reducedCount ${unlessFilter.description} card${if (reducedCount != 1) "s" else ""}"
    ): CompositeEffect {
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
                    prompt = prompt,
                    restrictions = listOf(
                        SelectionRestriction.ReducedMinimumIfMatches(
                            reducedMinimum = reducedCount,
                            filter = unlessFilter,
                            requiredMatches = requiredMatches
                        )
                    )
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

    /**
     * "Put up to [count] cards from your hand onto the battlefield" — Gather (hand, matching
     * [filter]) → Select up to [count] → Move to the battlefield. The `ChooseUpTo` selection models
     * the "you may" (Elvish Pioneer, Kaalia of the Vast, Spelunking).
     *
     * [anyNumber] swaps the selection for [SelectionMode.ChooseAnyNumber], i.e. the unbounded
     * "put **any number** of … cards from your hand onto the battlefield" wording (Redshift,
     * Rocketeer Chief's exhaust ability) — [count] is then ignored.
     */
    fun putFromHand(
        filter: GameObjectFilter = GameObjectFilter.Any,
        count: Int = 1,
        entersTapped: Boolean = false,
        entersAttacking: Boolean = false,
        anyNumber: Boolean = false,
        prompt: String? = null
    ): CompositeEffect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.You, filter),
                storeAs = "put_candidates"
            ),
            SelectFromCollectionEffect(
                from = "put_candidates",
                selection = if (anyNumber) {
                    SelectionMode.ChooseAnyNumber
                } else {
                    SelectionMode.ChooseUpTo(DynamicAmount.Fixed(count))
                },
                storeSelected = "putting",
                prompt = prompt
            ),
            MoveCollectionEffect(
                from = "putting",
                destination = CardDestination.ToZone(
                    Zone.BATTLEFIELD,
                    Player.You,
                    when {
                        entersAttacking -> ZonePlacement.TappedAndAttacking
                        entersTapped -> ZonePlacement.Tapped
                        else -> ZonePlacement.Default
                    }
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
     * Fixed-ceiling [discardUpToThenDraw] — "discard up to [max] cards, then draw that many cards",
     * the wording Tersa Lightshatter, Sokka, Bold Boomeranger and Greasewrench Goblin all print
     * verbatim. Only the prompt differs from the [DynamicAmount] overload, which can name the number.
     */
    fun discardUpToThenDraw(
        max: Int,
        draw: DynamicAmount? = null,
        storeAs: String = "discarded",
        prompt: String = "Discard up to $max card${if (max != 1) "s" else ""}",
    ): CompositeEffect = discardUpToThenDraw(DynamicAmount.Fixed(max), draw, storeAs, prompt)

    /**
     * "Discard up to [max] cards, then draw that many cards" — loot run backwards.
     *
     * The bound matters twice: the selection is *up to* [max], so declining entirely is legal and
     * then nothing is drawn, and the number drawn is the number **actually** discarded rather than
     * [max] — read off the pipeline collection's `${storeAs}_count`, so a player holding one card
     * discards one and draws one. Same Gather → Select → Move spine as [discardAnyNumber], with the
     * ceiling applied at the select step.
     *
     * Both halves are [DynamicAmount]s. [max] takes a resolution-time ceiling ("discard up to X
     * cards" off a cast X, or a count of permanents). [draw] defaults to `null`, meaning the printed
     * "that many"; pass one to decouple the draw from the discard — a cost-shaped discard whose
     * payoff is a flat or separately-scaled draw ("discard up to two cards, then draw three").
     */
    fun discardUpToThenDraw(
        max: DynamicAmount,
        draw: DynamicAmount? = null,
        storeAs: String = "discarded",
        prompt: String = "Choose cards to discard",
    ): CompositeEffect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.You),
                storeAs = "${storeAs}_candidates"
            ),
            SelectFromCollectionEffect(
                from = "${storeAs}_candidates",
                selection = SelectionMode.ChooseUpTo(max),
                storeSelected = storeAs,
                prompt = prompt
            ),
            MoveCollectionEffect(
                from = storeAs,
                destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.You),
                moveType = MoveType.Discard
            ),
            DrawCardsEffect(draw ?: DynamicAmount.VariableReference("${storeAs}_count"))
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
     * Shared connive pipeline (CR 701.50): draw a card, then discard a card; if the discard was a
     * nonland, run [onNonland]. Draw → Gather(hand) → Select(1) → Move(Discard) →
     * ConditionalOnCollection(Nonland). SelectFromCollection auto-resolves on empty / single-card
     * hands, matching the old monolithic executor's short-circuit behavior. [connive] and
     * [conniveTargeting] differ only in [onNonland], so they share this body.
     */
    private fun connivePipeline(onNonland: Effect): CompositeEffect = CompositeEffect(
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
                ifNotEmpty = onNonland
            )
        ),
        descriptionOverride = "Connive"
    )

    /**
     * Connive (CR 701.50): draw a card, then discard a card. If the discarded card
     * is a nonland, put a +1/+1 counter on [target].
     */
    fun connive(target: EffectTarget = EffectTarget.Self): CompositeEffect = connivePipeline(
        AddCountersEffect(
            counterType = Counters.PLUS_ONE_PLUS_ONE,
            count = 1,
            target = target
        )
    )

    /**
     * Connive variant whose +1/+1 counter lands on a *chosen target* rather than the conniving
     * permanent itself — the "When you discard a nonland card this way, put a +1/+1 counter on
     * target creature you control" shape (Teo, Spirited Glider).
     *
     * Unlike [connive] (counter goes on a fixed [EffectTarget], default Self), the recipient is a
     * reflexive target: it is selected at resolution via [SelectTargetEffect] *inside* the nonland
     * gate, so the player only picks a creature once a nonland card has actually been discarded —
     * never up front, and never when the discard turns out to be a land (or the hand was empty). The
     * counter then lands on that [EffectTarget.PipelineTarget].
     *
     * @param requirement what the chosen counter recipient must satisfy (e.g.
     *   `Targets.CreatureYouControl`).
     */
    fun conniveTargeting(
        requirement: TargetRequirement,
        storeAs: String = "connive_counter_target",
    ): CompositeEffect = connivePipeline(
        CompositeEffect(
            listOf(
                SelectTargetEffect(requirement = requirement, storeAs = storeAs),
                AddCountersEffect(
                    counterType = Counters.PLUS_ONE_PLUS_ONE,
                    count = 1,
                    target = EffectTarget.PipelineTarget(storeAs)
                )
            )
        )
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
     * "Target opponent reveals their hand. You choose a [filter] card from it. Exile that card."
     * — the Thoughtseize shape with exile instead of discard (Cruelclaw's Heist, Soul Search).
     *
     * The chooser is always the *controller*, not the revealing player: that asymmetry is what the
     * pattern is, so it is deliberately not derived from [target] the way [exileFromHand] derives
     * its chooser.
     *
     * @param storeChosenAs pipeline key holding the chosen card *before* the move.
     * @param storeExiledAs when non-null, pipeline key holding the cards that actually reached
     *   exile — the key to read for any rider that keys off the exiled card ("if the card's mana
     *   value is 1 or less …"), since it is empty when nothing was chosen or nothing moved.
     */
    fun revealHandAndExileChosen(
        target: EffectTarget = EffectTarget.ContextTarget(0),
        filter: GameObjectFilter = GameObjectFilter.Nonland,
        prompt: String = "Choose a nonland card to exile",
        storeChosenAs: String = "chosenCard",
        storeExiledAs: String? = null,
    ): CompositeEffect {
        val player = effectTargetToPlayer(target)
        return CompositeEffect(
            listOf(
                RevealHandEffect(target),
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, player, filter),
                    storeAs = "${storeChosenAs}Candidates"
                ),
                SelectFromCollectionEffect(
                    from = "${storeChosenAs}Candidates",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Controller,
                    storeSelected = storeChosenAs,
                    prompt = prompt
                ),
                MoveCollectionEffect(
                    from = storeChosenAs,
                    destination = CardDestination.ToZone(Zone.EXILE, player),
                    storeMovedAs = storeExiledAs
                )
            )
        )
    }

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
