package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
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
import com.wingedsheep.sdk.scripting.effects.CLASH_WON
import com.wingedsheep.sdk.scripting.effects.ClashEffect
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
import com.wingedsheep.sdk.scripting.effects.StorePlayerEffect
import com.wingedsheep.sdk.scripting.effects.RepeatDynamicTimesEffect
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Named MTG keyword-mechanic recipes (Blight, Bolster, Empower Jace, Explore, Forage, Gift, Incubate,
 * Learn, Recruit, Tempting offer).
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
                AddCountersToCollectionEffect("blighted", CounterType.MINUS_ONE_MINUS_ONE, amount)
            )
        )
    }

    /**
     * Tempting offer (an ability word, CR 207.2c) — "[offer]. Then each opponent may [offer]. For each
     * opponent who does, [offer] again for you." Tempt with Bunnies, Tempt with Discovery.
     *
     * Follows the cycle's ruling: you do [offer]; then every opponent decides, in turn order and
     * knowing the earlier answers, whether to accept; *then* [offer] happens for each opponent who
     * accepted (in APNAP order, each as that opponent's own "you"); then it happens again for you
     * once per accepting opponent. No opponent acts before every opponent has answered.
     *
     * Composed from the player-recording primitives: a `ForEachPlayerCollecting` over
     * [Player.EachOpponent] whose body is a yes/no that records the opponent
     * ([StorePlayerEffect]), a `ForEachPlayer` over the recorded players
     * ([Player.InCollection]), and a [RepeatDynamicTimesEffect] sized by the recorded count.
     *
     * @param offer What "you" do — written from the doer's point of view ([Player.You] / the
     *   controller), since each accepting opponent runs it as themselves.
     * @param description The printed text of the whole ability, which the composite's derived text
     *   can't reproduce.
     */
    fun temptingOffer(offer: Effect, description: String): CompositeEffect {
        val accepter = "temptingOfferAccepter"
        val accepted = "temptingOfferAccepted"
        return CompositeEffect(
            listOf(
                offer,
                com.wingedsheep.sdk.scripting.effects.ForEachPlayerCollectingEffect(
                    players = Player.EachOpponent,
                    effects = listOf(
                        Effects.May(
                            StorePlayerEffect(storeAs = accepter),
                            prompt = "Accept the tempting offer? (${offer.description})",
                            descriptionOverride = "You may accept the tempting offer",
                        )
                    ),
                    collectCollections = mapOf(accepter to accepted),
                ),
                com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect(Player.InCollection(accepted), listOf(offer)),
                RepeatDynamicTimesEffect(DynamicAmount.DistinctEntitiesInCollections(listOf(accepted)), offer),
            ),
            descriptionOverride = description,
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
                collectionFilter = CollectionFilter.LeastToughness,
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
            AddCountersToCollectionEffect("bolstered", CounterType.PLUS_ONE_PLUS_ONE, amount)
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
                filter = GameObjectFilter.Land,
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
                            counterType = CounterType.PLUS_ONE_PLUS_ONE,
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
     * "Whenever you forage" (`Triggers.you.forages()`) see a forage taken as an *effect*. The
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
    // Clash Pattern (Lorwyn, CR 701.30)
    // =========================================================================

    /** Clash without an immediate win rider; each clash replaces the CLASH_WON collection. */
    fun clash(): CompositeEffect = CompositeEffect(
        listOf(
            ChooseOpponentForSourceEffect(prompt = "Choose an opponent to clash with"),
            ClashEffect()
        )
    )

    /**
     * "Clash with an opponent. If you win, [ifYouWin]." — the whole printed Lorwyn template
     * (CR 701.30b). Use the no-argument overload for a standalone clash inside a repeat loop.
     *
     * Three existing pieces, composed:
     *
     *  1. [ChooseOpponentForSourceEffect] fixes *which* opponent clashes with you. Clash chooses an
     *     opponent rather than targeting one, and the same player must both reveal and decide, so
     *     the choice is written to the source's durable `OPPONENT` slot and read back by
     *     [Player.ChosenOpponent]. A bare `Chooser.Opponent` would re-pick per step and could split
     *     a multiplayer clash across two different opponents. With one opponent the choice is
     *     forced and promptless.
     *  2. [com.wingedsheep.sdk.scripting.effects.ClashEffect] performs the clash: reveal, decide,
     *     move, score, and fire "Whenever you clash" for both participants.
     *  3. A [Gate.DoAction] gate scored by [SuccessCriterion.CollectionNonEmpty] reads the win.
     *     "If you win" is an action-*outcome* rider, which is exactly what that gate models — the
     *     clash writes your revealed card into a collection only on a win, so winning is an
     *     ordinary pipeline result and clash needs no gate kind of its own. The gate's existing
     *     continuation plumbing is what carries the two top-or-bottom pauses.
     *
     * [otherwise] supplies the nonwinning branch, such as Captivating Glance or Whirlpool Whelm.
     *
     * ```kotlin
     * // Adder-Staff Boggart — "When this creature enters, clash with an opponent.
     * //                        If you win, put a +1/+1 counter on this creature."
     * Patterns.Mechanic.clash(Effects.AddCounters(Counters.plusOnePlusOne(1)))
     * ```
     */
    fun clash(ifYouWin: Effect, otherwise: Effect? = null): GatedEffect = GatedEffect(
        gate = Gate.DoAction(
            action = clash(),
            successCriterion = SuccessCriterion.CollectionNonEmpty(CLASH_WON)
        ),
        then = ifYouWin,
        otherwise = otherwise,
        descriptionOverride = buildString {
            append("Clash with an opponent. If you win, ")
            append(ifYouWin.description.replaceFirstChar { it.lowercase() })
            if (otherwise != null && otherwise.description.isNotBlank()) {
                append(". Otherwise, ${otherwise.description.replaceFirstChar { it.lowercase() }}")
            }
        }
    )

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
     *         giftMode = Mode.noTarget(baseEffect then opponentDraws then Effects.GiftGiven(),
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
                effect = ChooseOpponentForSourceEffect(prompt = "Choose an opponent to receive the gift") then
                    giftMode.effect
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
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
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
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                amount = amount,
                target = EffectTarget.PipelineTarget(CREATED_TOKENS, 0)
            )
        )
    )

    // =========================================================================
    // Empower Jace (Reality Fracture)
    // =========================================================================

    /**
     * The permanents empower Jace can choose among: Jace planeswalker tokens (CR 701.71a). A
     * nontoken Jace planeswalker card doesn't qualify, and neither would a token that is a Jace
     * but not a planeswalker.
     */
    val JACE_PLANESWALKER_TOKEN: GameObjectFilter =
        GameObjectFilter.Planeswalker.withSubtype("Jace").token()

    /**
     * Empower Jace N (CR 701.71a) — "If you don't control a Jace planeswalker token, create a
     * blue Jace planeswalker token with 0 loyalty, '[−1]: Surveil 1,' and '[−3]: Draw a card.'
     * Choose a Jace planeswalker token you control. Put N loyalty counters on it."
     *
     * Atomic composition, same find-or-create shape as amass:
     * 1. create the predefined `Jace` token only when no Jace planeswalker token is controlled;
     * 2. gather the Jace planeswalker tokens controlled *now* (which includes the one just made);
     * 3. choose exactly one — auto-picked when there is only one, a battlefield choice otherwise
     *    (non-targeting: the reminder text never says "target");
     * 4. put N loyalty counters on it.
     *
     * The token enters with 0 loyalty and gets its counters in the same resolution, so the
     * zero-loyalty state-based action (CR 704.5i) only sees it at 0 when N is 0.
     *
     * @param amount N, evaluated when the counters are placed. Creating a noncreature
     *   planeswalker token first can't change a creature- or damage-based N.
     */
    fun empowerJace(amount: DynamicAmount): CompositeEffect = CompositeEffect(
        listOf(
            Effects.If(
                condition = Conditions.YouControl(JACE_PLANESWALKER_TOKEN, negate = true),
                then = CreatePredefinedTokenEffect(tokenType = "Jace")
            ),
            GatherCardsEffect(
                source = CardSource.BattlefieldMatching(JACE_PLANESWALKER_TOKEN, Player.You),
                storeAs = "empower_jaces"
            ),
            SelectFromCollectionEffect(
                from = "empower_jaces",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                chooser = Chooser.Controller,
                storeSelected = "empower_jace",
                prompt = "Empower Jace ${amount.description} — choose a Jace token you control",
                useTargetingUI = true
            ),
            AddCountersToCollectionEffect(
                collectionName = "empower_jace",
                counterType = CounterType.LOYALTY,
                amount = amount
            )
        ),
        descriptionOverride = "Empower Jace ${amount.description}"
    )

    /** Empower Jace N with a fixed N — see [empowerJace]. */
    fun empowerJace(amount: Int): CompositeEffect = empowerJace(DynamicAmount.Fixed(amount))

    // =========================================================================
    // Recruit Pattern (The Hobbit)
    // =========================================================================

    /**
     * **Learn** (CR 701.48) — the Strixhaven keyword action, printed as a bare "Learn." with
     * reminder text.
     *
     * CR 701.48a spells it out, and the order is load-bearing rather than a "choose one":
     *
     * > "Learn" means "You may discard a card. If you do, draw a card. If you didn't discard a
     * > card, you may reveal a Lesson card you own from outside the game and put it into your
     * > hand."
     *
     * So the discard is offered *first*, and taking it forecloses the Lesson — a player who
     * discards never gets the sideboard option. The composition follows that shape literally:
     *
     * 1. Gather the controller's hand and let them choose **up to one** card to discard. The
     *    `ChooseUpTo(1)` is where the printed "you may" lives: choosing none is declining, and an
     *    empty hand auto-resolves to none without a prompt.
     * 2. If they discarded, draw a card; if they didn't, run a Lesson-restricted
     *    [SideboardPatterns.wish], whose own `ChooseUpTo(1)` carries the second "may" — declining,
     *    or owning no Lesson at all, simply does nothing.
     *
     * The discard is a real discard ([MoveType.Discard]), so madness and "whenever you discard a
     * card" triggers see it.
     *
     * Collection names are `learn_`-prefixed so a Learn nested inside another pipeline can't
     * collide with that pipeline's own `hand` / `discarded` collections.
     */
    fun learn(): CompositeEffect = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.You),
                storeAs = "learn_hand"
            ),
            SelectFromCollectionEffect(
                from = "learn_hand",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                chooser = Chooser.Controller,
                storeSelected = "learn_discarded",
                prompt = "Learn — you may discard a card to draw a card"
            ),
            MoveCollectionEffect(
                from = "learn_discarded",
                destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.You),
                moveType = MoveType.Discard
            ),
            ConditionalOnCollectionEffect(
                collection = "learn_discarded",
                ifNotEmpty = DrawCardsEffect(1, EffectTarget.Controller),
                ifEmpty = SideboardPatterns.wish(
                    filter = GameObjectFilter.Any.withSubtype(Subtype.LESSON),
                    storeAs = "learn_lessons"
                )
            )
        ),
        descriptionOverride = "Learn"
    )

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
