package com.wingedsheep.sdk.dsl

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
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
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
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Named MTG keyword-mechanic recipes (Blight, Bolster, Explore, Forage, Gift, Incubate).
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
     * Forage — exile three cards from your graveyard or sacrifice a Food.
     *
     * Returns a [ChooseActionEffect] with feasibility checks so the choice is only
     * offered when the player can actually fulfill at least one option.
     *
     * @param afterEffect optional effect appended to each mode (e.g., add counters)
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
                if (afterEffect != null) add(afterEffect)
            }
        )

        val sacrificeFood = if (afterEffect != null) {
            CompositeEffect(
                listOf(
                    SacrificeEffect(
                        filter = GameObjectFilter.Any.withSubtype("Food"),
                        count = 1
                    ),
                    afterEffect
                )
            )
        } else {
            SacrificeEffect(
                filter = GameObjectFilter.Any.withSubtype("Food"),
                count = 1
            )
        }

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
     */
    fun giftSpell(noGiftMode: Mode, giftMode: Mode): ModalEffect =
        ModalEffect.chooseOne(noGiftMode, giftMode, countsAsModalSpell = false)

    // =========================================================================
    // Incubate Pattern (CR 701.53)
    // =========================================================================

    /**
     * Incubate N (CR 701.53). Atomic composition: create the (DFC) Incubator token, then
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
     * Incubate X (CR 701.53), where X is a [DynamicAmount] resolved at trigger/spell
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
}
