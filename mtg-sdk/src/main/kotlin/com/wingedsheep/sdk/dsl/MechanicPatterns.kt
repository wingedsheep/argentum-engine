package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersToCollectionEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicCountersEffect
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.ChooseOpponentForSourceEffect
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.conditions.CollectionContainsMatch
import com.wingedsheep.sdk.scripting.conditions.NotCondition
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Named MTG keyword-mechanic recipes (Blight, Bolster, Explore, Forage, Gift, Incubate, Recruit).
 *
 * Reached through the [Patterns] index — `Patterns.Mechanic.blight(...)`. Each composes existing
 * atomic effects into the printed keyword behaviour; they live here (rather than a zone-based
 * pattern object) because they're identified by their rules-name, not the zone they touch.
 */
object MechanicPatterns {

    /**
     * Blight N — the given player puts N -1/-1 counters on a creature they control.
     *
     * Non-targeting keyword action (the word "target" does not appear in the
     * reminder text, so shroud/hexproof do not interact). If the chooser has
     * no creatures, the effect silently does nothing — `ChooseExactly(1)` on an
     * empty collection auto-selects nothing and the subsequent counter placement
     * is a no-op.
     *
     * @param amount Number of -1/-1 counters to place
     * @param player The player who blights. Defaults to [Player.You] (the controller).
     *               Use [Player.TargetOpponent] / [Player.TargetPlayer] for "target opponent blights N";
     *               the chooser is derived from this reference so the targeted player makes the choice.
     */
    fun blight(amount: Int, player: Player = Player.You): CompositeEffect {
        val chooser = when (player) {
            Player.TargetOpponent, Player.TargetPlayer -> Chooser.TargetPlayer
            else -> Chooser.Controller
        }
        val possessive = if (player == Player.You) "you control" else "they control"
        return CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.ControlledPermanents(player, GameObjectFilter.Creature),
                    storeAs = "blightTargets"
                ),
                SelectFromCollectionEffect(
                    from = "blightTargets",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    chooser = chooser,
                    storeSelected = "blighted",
                    prompt = "Blight $amount — choose a creature $possessive",
                    useTargetingUI = true
                ),
                AddCountersToCollectionEffect("blighted", Counters.MINUS_ONE_MINUS_ONE, amount)
            )
        )
    }

    /**
     * Bolster N (CR 701.36) — "Choose a creature with the least toughness among creatures
     * you control and put N +1/+1 counters on it."
     *
     * Non-targeting keyword action (the reminder text never says "target", so
     * shroud/hexproof do not interact). The controller chooses among creatures they
     * control, restricted to those tied for the least toughness; a single tie-break
     * choice is made via [SelectionMode.ChooseExactly]. If the controller has no
     * creatures, the gather yields nothing and the counter placement is a silent no-op.
     *
     * Toughness is read from projected state in the executor, so continuous effects and
     * counters already in play are respected when determining "least toughness".
     *
     * @param amount Number of +1/+1 counters to place
     */
    fun bolster(amount: Int): CompositeEffect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.ControlledPermanents(Player.You, GameObjectFilter.Creature),
                storeAs = "bolsterCreatures"
            ),
            FilterCollectionEffect(
                from = "bolsterCreatures",
                filter = CollectionFilter.LeastToughness,
                storeMatching = "bolsterLeastToughness"
            ),
            SelectFromCollectionEffect(
                from = "bolsterLeastToughness",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                chooser = Chooser.Controller,
                storeSelected = "bolstered",
                prompt = "Bolster $amount — choose a creature with the least toughness",
                useTargetingUI = true
            ),
            AddCountersToCollectionEffect("bolstered", Counters.PLUS_ONE_PLUS_ONE, amount)
        )
    )

    /**
     * Explore (CR 701.44) — the [explorer] permanent's controller reveals the top card of
     * their library. A land card goes to their hand; otherwise the explorer gets a +1/+1
     * counter and the controller may put the revealed card into their graveyard.
     *
     * Pure pipeline composition — Gather (revealed) → partition by land →
     * move the land to hand → gate the "otherwise" branch on *no land revealed*:
     *
     * - The gate is `Not(CollectionContainsMatch("explored", Land))`, not "a nonland was
     *   revealed": CR 701.44a's "otherwise" also covers an empty library, where nothing is
     *   revealed but the explorer still gets its counter (and the may-discard is simply
     *   moot — selecting from the empty collection is a no-op).
     * - The revealed nonland card never leaves the library top unless the controller puts
     *   it into the graveyard, so "leave it on top" needs no move step.
     * - Counter first, then the graveyard choice, matching the rule's stated order.
     *
     * @param explorer The permanent doing the exploring. Defaults to [EffectTarget.Self]
     *   ("this creature explores" on its own trigger); pass a different target for
     *   "target creature you control explores" (Map tokens).
     */
    fun explore(explorer: EffectTarget = EffectTarget.Self): CompositeEffect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                storeAs = "explored",
                revealed = true
            ),
            FilterCollectionEffect(
                from = "explored",
                filter = CollectionFilter.MatchesFilter(GameObjectFilter.Land),
                storeMatching = "exploredLand",
                storeNonMatching = "exploredNonland"
            ),
            MoveCollectionEffect(
                from = "exploredLand",
                destination = CardDestination.ToZone(Zone.HAND)
            ),
            GatedEffect(
                gate = Gate.WhenCondition(
                    NotCondition(CollectionContainsMatch("explored", GameObjectFilter.Land))
                ),
                then = CompositeEffect(
                    listOf(
                        AddCountersEffect(
                            counterType = Counters.PLUS_ONE_PLUS_ONE,
                            count = 1,
                            target = explorer
                        ),
                        SelectFromCollectionEffect(
                            from = "exploredNonland",
                            selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                            storeSelected = "exploredToGraveyard",
                            prompt = "Explore — you may put the revealed card into your graveyard",
                            selectedLabel = "Put into graveyard",
                            remainderLabel = "Leave on top"
                        ),
                        MoveCollectionEffect(
                            from = "exploredToGraveyard",
                            destination = CardDestination.ToZone(Zone.GRAVEYARD)
                        )
                    )
                ),
                descriptionOverride = "Otherwise, put a +1/+1 counter on ${explorer.description}, " +
                    "then you may put the revealed card into your graveyard"
            )
        )
    )

    /**
     * Forage — CR 701.59a, "Exile three cards from your graveyard or sacrifice a Food."
     *
     * Returns a [ChooseActionEffect] with feasibility checks so the choice is only
     * offered when the player can actually fulfill at least one option.
     *
     * **Each mode ends by emitting the foraged event** ([Effects.Foraged]), which is what makes
     * "Whenever you forage" (`Triggers.WheneverYouForage`) see a forage taken as an *effect*. The
     * three *cost* contexts — an activated-ability cost, a cast-time additional cost, the
     * graveyard-cast permission — emit it from their shared payment implementation instead, because
     * they never come through here. That split is waterbend's: a keyword action that is sometimes a
     * cost and sometimes an effect cannot be observed from one place.
     *
     * The marker sits **inside** each mode rather than after the choice, which is what gives the
     * "only if it actually happened" property for free: forage has no "even if you can't" clause, so
     * a declined forage — or one where neither mode is feasible — runs no mode and emits nothing.
     * It also goes *before* [afterEffect], so the event is emitted the moment the forage completes
     * and an "If you do, …" rider reads as the separate thing it is.
     *
     * @param afterEffect optional effect appended to each mode (e.g., add counters) — the "If you
     *   do, …" half of "you may forage. If you do, …". Note that "**When** you do, …" is a
     *   different card: that one is a reflexive trigger (CR 603.12) and belongs in
     *   `ReflexiveTriggerEffect`, not here.
     */
    fun forage(afterEffect: Effect? = null): ChooseActionEffect {
        val exileFromGraveyard = CompositeEffect(
            buildList {
                add(
                    GatherCardsEffect(
                        source = CardSource.FromZone(Zone.GRAVEYARD, Player.You),
                        storeAs = "graveCards"
                    )
                )
                add(
                    SelectFromCollectionEffect(
                        from = "graveCards",
                        selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(3)),
                        storeSelected = "exileCards",
                        prompt = "Choose 3 cards from your graveyard to exile (forage)"
                    )
                )
                add(
                    MoveCollectionEffect(
                        from = "exileCards",
                        destination = CardDestination.ToZone(Zone.EXILE)
                    )
                )
                add(Effects.Foraged())
                if (afterEffect != null) add(afterEffect)
            }
        )

        // No empty-composite branch any more: every mode now carries the marker, so the sacrifice
        // mode is a composite whether or not there is an `afterEffect`.
        val sacrificeFood = CompositeEffect(
            buildList {
                add(
                    SacrificeEffect(
                        filter = GameObjectFilter.Any.withSubtype("Food"),
                        count = 1
                    )
                )
                add(Effects.Foraged())
                if (afterEffect != null) add(afterEffect)
            }
        )

        return ChooseActionEffect(
            choices = listOf(
                EffectChoice(
                    label = "Exile three cards from your graveyard",
                    effect = exileFromGraveyard,
                    feasibilityCheck = FeasibilityCheck.HasCardsInZone(
                        zone = Zone.GRAVEYARD,
                        count = 3
                    )
                ),
                EffectChoice(
                    label = "Sacrifice a Food",
                    effect = sacrificeFood,
                    feasibilityCheck = FeasibilityCheck.ControlsPermanentMatching(
                        filter = GameObjectFilter.Any.withSubtype("Food")
                    )
                )
            )
        )
    }

    // =========================================================================
    // Gift Pattern (Bloomburrow)
    // =========================================================================

    /**
     * Bloomburrow Gift: "You may promise an opponent a gift as you cast this spell."
     *
     * Modelled as a two-mode [ModalEffect] where mode 0 is "don't promise a gift" and
     * mode 1 is the gift-promised branch (whose effect chain should end in
     * [com.wingedsheep.sdk.scripting.effects.GiftGivenEffect] via
     * [com.wingedsheep.sdk.dsl.Effects.GiftGiven]). The flag `countsAsModalSpell = false`
     * keeps "Whenever you cast a modal spell" triggers from misreading Gift as modal.
     *
     * The gift branch is prefixed with a
     * [com.wingedsheep.sdk.scripting.effects.ChooseOpponentForSourceEffect]: the promising
     * player picks WHICH opponent receives the gift (forced, promptless with a single
     * opponent), and the gift effect addresses the recipient through
     * [com.wingedsheep.sdk.scripting.references.Player.ChosenOpponent].
     *
     * Standard usage:
     * ```kotlin
     * spell {
     *     effect = Patterns.Mechanic.giftSpell(
     *         noGiftMode = Mode.noTarget(baseEffect, "Don't promise a gift — …"),
     *         giftMode = Mode.noTarget(baseEffect.then(opponentDraws).then(Effects.GiftGiven()),
     *                                  "Promise a gift — …")
     *     )
     * }
     * ```
     * where `opponentDraws` targets `Player.ChosenOpponent`.
     */
    fun giftSpell(noGiftMode: Mode, giftMode: Mode): ModalEffect =
        ModalEffect.chooseOne(
            noGiftMode,
            giftMode.copy(
                effect = ChooseOpponentForSourceEffect(prompt = "Choose an opponent to receive the gift")
                    .then(giftMode.effect)
            ),
            countsAsModalSpell = false
        )

    // =========================================================================
    // Incubate Pattern (CR 701.51)
    // =========================================================================

    /**
     * Incubate N (CR 701.51). Atomic composition: create the (DFC) Incubator token, then
     * place N +1/+1 counters on it via the pipeline-published entity ID.
     *
     * The Incubator's `{2}: Transform this token` activated ability is declared on the
     * front-face CardDefinition in `PredefinedTokens.kt`. The transform mechanism is the
     * same one used for ordinary DFCs.
     */
    fun incubate(n: Int): CompositeEffect = CompositeEffect(
        listOf(
            CreatePredefinedTokenEffect(tokenType = "Incubator", count = 1),
            AddCountersEffect(
                counterType = Counters.PLUS_ONE_PLUS_ONE,
                count = n,
                target = EffectTarget.PipelineTarget(CREATED_TOKENS, 0)
            )
        )
    )

    /**
     * Incubate X (CR 701.51), where X is a [DynamicAmount] resolved at trigger/spell
     * resolution time (e.g., the triggering spell's mana value for Chrome Host Seedshark).
     *
     * Same shape as [incubate] but uses [AddDynamicCountersEffect] so the +1/+1 counter
     * count is evaluated against the live effect context.
     */
    fun incubate(amount: DynamicAmount): CompositeEffect = CompositeEffect(
        listOf(
            CreatePredefinedTokenEffect(tokenType = "Incubator", count = 1),
            AddDynamicCountersEffect(
                counterType = Counters.PLUS_ONE_PLUS_ONE,
                amount = amount,
                target = EffectTarget.PipelineTarget(CREATED_TOKENS, 0)
            )
        )
    )

    // =========================================================================
    // Recruit Pattern (The Hobbit)
    // =========================================================================

    /**
     * Recruit (The Hobbit) — "Draw a card, then discard a card. If you discarded a nonland card,
     * create a 1/1 white Human Soldier creature token."
     *
     * A keyword *action* with fixed reminder text (like explore or amass), not a keyword ability,
     * so it needs no `Keyword` entry. Mechanically it is connive (CR 701.50) with a token payoff
     * instead of a +1/+1 counter, and it reuses that proven pipeline shape exactly: Draw →
     * Gather(hand) → Select(1) → Move(Discard) → ConditionalOnCollection(Nonland). See
     * [HandPatterns.connive].
     *
     * Two details the shape is load-bearing for:
     *
     * - The discard is a real discard ([MoveType.Discard]), not a bare move to the graveyard, so
     *   madness and "whenever you discard a card" triggers see it.
     * - The payoff is gated on the discarded card being a *nonland*, never on "no land was
     *   discarded": with an empty hand nothing is discarded at all, and CR's "if you discarded a
     *   nonland card" is then false. `ChooseExactly(1)` auto-resolves on an empty or single-card
     *   hand, leaving the collection empty so no token is created.
     *
     * The draw comes first and is unconditional, so an empty library still sets up the usual
     * draw-from-empty loss at the next state-based check.
     *
     * Token art is left to the set-scoped resolver (`MtgSet.tokenArt`, keyed off the creating
     * card's printing) rather than baked in here — ten HOB cards share this facade.
     */
    fun recruit(): CompositeEffect = CompositeEffect(
        listOf(
            DrawCardsEffect(1, EffectTarget.Controller),
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.You),
                storeAs = "recruit_hand"
            ),
            SelectFromCollectionEffect(
                from = "recruit_hand",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                chooser = Chooser.Controller,
                storeSelected = "recruit_discarded",
                prompt = "Recruit — choose a card to discard"
            ),
            MoveCollectionEffect(
                from = "recruit_discarded",
                destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.You),
                moveType = MoveType.Discard
            ),
            ConditionalOnCollectionEffect(
                collection = "recruit_discarded",
                filter = GameObjectFilter.Nonland,
                ifNotEmpty = CreateTokenEffect(
                    count = DynamicAmount.Fixed(1),
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Human", "Soldier")
                )
            )
        ),
        descriptionOverride = "Recruit"
    )
}
