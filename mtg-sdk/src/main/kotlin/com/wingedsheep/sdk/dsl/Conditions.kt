package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Speed
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.conditions.AllConditions
import com.wingedsheep.sdk.scripting.conditions.AnyCondition
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.conditions.NotCondition
import com.wingedsheep.sdk.scripting.conditions.NumberMatches
import com.wingedsheep.sdk.scripting.conditions.NumberProperty
import com.wingedsheep.sdk.scripting.conditions.WasCast as WasCastCondition
import com.wingedsheep.sdk.scripting.conditions.NoManaSpentToCast as NoManaSpentToCastCondition
import com.wingedsheep.sdk.scripting.conditions.NoManaSpentToCastEntered as NoManaSpentToCastEnteredCondition
import com.wingedsheep.sdk.scripting.conditions.WasCastFromHand as WasCastFromHandCondition
import com.wingedsheep.sdk.scripting.conditions.WasCastFromZone as WasCastFromZoneCondition
import com.wingedsheep.sdk.scripting.conditions.SourceInZone as SourceInZoneCondition
import com.wingedsheep.sdk.scripting.conditions.WasKicked as WasKickedCondition
import com.wingedsheep.sdk.scripting.conditions.BlightWasPaid as BlightWasPaidCondition
import com.wingedsheep.sdk.scripting.conditions.WaterbendWasPaid as WaterbendWasPaidCondition
import com.wingedsheep.sdk.scripting.conditions.SneakCostWasPaid as SneakCostWasPaidCondition
import com.wingedsheep.sdk.scripting.conditions.WebSlungCostWasPaid as WebSlungCostWasPaidCondition
import com.wingedsheep.sdk.scripting.conditions.MayhemCostWasPaid as MayhemCostWasPaidCondition
import com.wingedsheep.sdk.scripting.conditions.CastChoiceMade as CastChoiceMadeCondition
import com.wingedsheep.sdk.scripting.conditions.CastChoiceIs as CastChoiceIsCondition
import com.wingedsheep.sdk.scripting.conditions.CastTimeFlagSet as CastTimeFlagSetCondition
import com.wingedsheep.sdk.scripting.conditions.EntityMatches
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.conditions.SourceIsRingBearer as SourceIsRingBearerCondition
import com.wingedsheep.sdk.scripting.conditions.YouChoseOtherCreatureAsRingBearer as YouChoseOtherCreatureAsRingBearerCondition
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn as IsYourTurnCondition
import com.wingedsheep.sdk.scripting.conditions.IsNotYourTurn as IsNotYourTurnCondition
import com.wingedsheep.sdk.scripting.conditions.IsPlayersTurn as IsPlayersTurnCondition
import com.wingedsheep.sdk.scripting.conditions.IsInPhase as IsInPhaseCondition
import com.wingedsheep.sdk.scripting.conditions.PlayerAttackedWithCreaturesThisTurn
import com.wingedsheep.sdk.scripting.conditions.PlayerCastSpellsThisTurn
import com.wingedsheep.sdk.scripting.conditions.PlayerCommittedCrimeThisTurn
import com.wingedsheep.sdk.scripting.conditions.PlayerHasCitysBlessing
import com.wingedsheep.sdk.scripting.conditions.PlayerHasEnduringStory
import com.wingedsheep.sdk.scripting.conditions.RingHasTemptedPlayerAtLeast
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.sdk.scripting.conditions.Condition as ConditionInterface

/**
 * Facade object providing convenient access to Condition types.
 *
 * Usage:
 * ```kotlin
 * Conditions.OpponentControlsMoreLands
 * Conditions.LifeAtMost(5)
 * Conditions.ControlCreature
 * ```
 */
object Conditions {

    // =========================================================================
    // The Ring (CR 701.54)
    // =========================================================================

    /**
     * If the source permanent is your Ring-bearer (CR 701.54e).
     */
    val SourceIsRingBearer: ConditionInterface = SourceIsRingBearerCondition

    /**
     * If you chose a creature other than this as your Ring-bearer (CR 701.54a). Intervening-if
     * for `Triggers.RingTemptsYou` payoffs that fire only when the player picked someone else.
     */
    val YouChoseOtherCreatureAsRingBearer: ConditionInterface = YouChoseOtherCreatureAsRingBearerCondition

    /**
     * If a counter was put on this creature this turn (Secrets of Strixhaven — Fractal Tender).
     * True while the source permanent carries the per-turn "received counters" marker, which the
     * counter-placement path stamps and cleanup clears each turn.
     *
     * Narrow it when the printed text does. [counterType] scopes it to one kind, and [placedByYou]
     * to counters *you* put on — Beast, Erudite Aerialist ("as long as you've put one or more +1/+1
     * counters on Beast this turn") needs both:
     * `SourceReceivedCounterThisTurn(Counters.PLUS_ONE_PLUS_ONE, placedByYou = true)`.
     *
     * The self-scoped view of the general [StatePredicate.ReceivedCounterThisTurn]: this is
     * [SourceMatches] over that predicate, so the source-scoped and filter-scoped readings ("each
     * creature you control that you've put …" — Kid Loki) share one evaluator instead of running
     * as parallel paths.
     */
    fun SourceReceivedCounterThisTurn(
        counterType: String? = null,
        placedByYou: Boolean = false
    ): ConditionInterface =
        SourceMatches(
            com.wingedsheep.sdk.scripting.GameObjectFilter.Any
                .receivedCounterThisTurn(counterType, placedByYou)
        )

    /**
     * If a permanent entered the battlefield face down under your control this turn (Duskmourn —
     * Oblivious Bookworm). Reads the per-player face-down-entered tracker, cleared each turn.
     */
    val PermanentEnteredFaceDownThisTurn: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.PermanentEnteredFaceDownThisTurn()

    /**
     * If you turned a permanent face up this turn (Duskmourn — Oblivious Bookworm). Reads the
     * per-player turned-face-up tracker, cleared each turn.
     */
    val YouTurnedPermanentFaceUpThisTurn: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.PlayerTurnedPermanentFaceUpThisTurn()

    /**
     * If the Ring has tempted you [times] or more times this game (CR 701.54). Reads the cumulative
     * tempt count on your The Ring emblem; a player never tempted counts as 0.
     */
    fun RingHasTemptedYouAtLeast(times: Int): ConditionInterface =
        RingHasTemptedPlayerAtLeast(times, Player.You)

    // =========================================================================
    // Battlefield Conditions (via Exists / Compare)
    // =========================================================================

    /**
     * Generic numeric comparison of two [DynamicAmount]s with a [ComparisonOperator] — the
     * facade entry point for any "if amount X (</≤/=/≠/>/≥) amount Y" intervening-if or static
     * condition. Composes the underlying [Compare] condition.
     *
     * Example (Taii Wakeen, Perfect Shot intervening-if — "damage equal to that creature's
     * toughness"):
     * ```
     * Conditions.CompareAmounts(
     *     DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT),
     *     ComparisonOperator.EQ,
     *     DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_RECIPIENT_TOUGHNESS),
     * )
     * ```
     */
    fun CompareAmounts(
        left: DynamicAmount,
        operator: ComparisonOperator,
        right: DynamicAmount,
    ): ConditionInterface = Compare(left, operator, right)

    /**
     * If [amount] is a prime number (2, 3, 5, 7, …). 0 and 1 are not prime.
     *
     * The unary counterpart to [CompareAmounts] — used for "if you control a prime number of
     * lands" (Zimone, All-Questioning). Compose the count with any [DynamicAmount], e.g.
     * `Conditions.AmountIsPrime(DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land))`.
     */
    fun AmountIsPrime(amount: DynamicAmount): ConditionInterface =
        NumberMatches(amount, NumberProperty.Prime)

    /** If [amount] is even (0 counts as even). */
    fun AmountIsEven(amount: DynamicAmount): ConditionInterface =
        NumberMatches(amount, NumberProperty.Even)

    /** If [amount] is odd. */
    fun AmountIsOdd(amount: DynamicAmount): ConditionInterface =
        NumberMatches(amount, NumberProperty.Odd)

    /** If [amount] is a multiple of [divisor] (must be non-zero). */
    fun AmountIsMultipleOf(amount: DynamicAmount, divisor: Int): ConditionInterface =
        NumberMatches(amount, NumberProperty.MultipleOf(divisor))

    /**
     * If you have at least [amount] unspent mana in your mana pool (Ozai, the Phoenix King —
     * "as long as you have six or more unspent mana"). Reads the pool's total via
     * [DynamicAmount.UnspentMana].
     */
    fun YouHaveUnspentManaAtLeast(amount: Int): ConditionInterface =
        Compare(
            DynamicAmount.UnspentMana(Player.You),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(amount)
        )

    /**
     * If an opponent controls more lands than you.
     */
    val OpponentControlsMoreLands: ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.EachOpponent, GameObjectFilter.Land),
            ComparisonOperator.GT,
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land)
        )

    /**
     * If an opponent controls more creatures than you.
     */
    val OpponentControlsMoreCreatures: ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.EachOpponent, GameObjectFilter.Creature),
            ComparisonOperator.GT,
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature)
        )

    /**
     * If an opponent has more cards in hand than you.
     *
     * Compares opponents' hand size to yours (CR — "an opponent has more cards in hand than you").
     * Used by Beza, the Bounding Spring and Joined Researchers (Secrets of Strixhaven).
     */
    val OpponentHasMoreCardsInHand: ConditionInterface =
        Compare(
            DynamicAmount.Count(Player.EachOpponent, Zone.HAND),
            ComparisonOperator.GT,
            DynamicAmount.Count(Player.You, Zone.HAND)
        )

    /**
     * If you control more creatures than opponent.
     * Used for CantAttackUnless / CantBlockUnless (e.g. Goblin Goon).
     */
    val ControlMoreCreatures: ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature),
            ComparisonOperator.GT,
            DynamicAmount.AggregateBattlefield(Player.EachOpponent, GameObjectFilter.Creature)
        )

    /**
     * If the defending player controls a land of a specific subtype (resolved per
     * attack declaration via the defender bound into the evaluation context). Used
     * for CantAttackUnless (e.g. Deep-Sea Serpent, Slipstream Eel, Dandân) — oracle
     * for these reads "unless defending player controls an Island", so in multiplayer
     * the check is against the player being attacked, not any opponent.
     */
    fun DefendingPlayerControlsLandType(landType: String): ConditionInterface =
        Exists(Player.DefendingPlayer, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype(landType))

    /**
     * If an opponent controls a creature.
     */
    val OpponentControlsCreature: ConditionInterface =
        Exists(Player.EachOpponent, Zone.BATTLEFIELD, GameObjectFilter.Creature)

    /**
     * If you control a creature.
     */
    val ControlCreature: ConditionInterface =
        Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature)

    /**
     * If there are no creatures anywhere on the battlefield (either player). Global scope —
     * `Player.Each` checks every player's battlefield, negated. Used by Drop of Honey's
     * "when there are no creatures on the battlefield, sacrifice this enchantment".
     */
    val NoCreaturesOnBattlefield: ConditionInterface =
        Exists(Player.Each, Zone.BATTLEFIELD, GameObjectFilter.Creature, negate = true)

    /**
     * If you control at least one permanent matching [filter].
     * General-purpose battlefield existence check — pass any [GameObjectFilter]
     * (e.g. `GameObjectFilter.Creature.copy(statePredicates = listOf(StatePredicate.HasAnyCounter))`
     * for "a creature with a counter on it").
     *
     * Set [excludeSelf] for "another …" wording — the resolving source is excluded from the
     * search (e.g. Splitskin Doll's "another creature with power 2 or less").
     */
    fun YouControl(
        filter: GameObjectFilter,
        negate: Boolean = false,
        excludeSelf: Boolean = false
    ): ConditionInterface =
        Exists(Player.You, Zone.BATTLEFIELD, filter, negate = negate, excludeSelf = excludeSelf)

    /**
     * If **an** opponent controls at least one permanent matching [filter] — the opponent-side
     * mirror of [YouControl], generalizing [OpponentControlsCreature] to any filter.
     *
     * `Player.EachOpponent` is an existential across opponents, not a universal: the condition
     * holds when *any single* opponent controls a match, which is what "as long as an opponent
     * controls a planeswalker" (Syr Ginger, the Meal Ender) means in multiplayer.
     */
    fun OpponentControls(
        filter: GameObjectFilter,
        negate: Boolean = false
    ): ConditionInterface =
        Exists(Player.EachOpponent, Zone.BATTLEFIELD, filter, negate = negate)

    /**
     * If you control an enchantment.
     */
    val ControlEnchantment: ConditionInterface =
        Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Enchantment)

    /**
     * If you control an artifact.
     */
    val ControlArtifact: ConditionInterface =
        Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Artifact)

    /**
     * If you control a legendary creature or planeswalker.
     * Used as the cast restriction for legendary sorceries.
     */
    val ControlLegendaryCreatureOrPlaneswalker: ConditionInterface =
        Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.CreatureOrPlaneswalker.legendary())

    /**
     * If you control N or more lands.
     */
    fun ControlLandsAtLeast(count: Int): ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * If [count] or more unlocked doors are among Rooms [player] controls (CR 709.5).
     * A Room with both doors unlocked counts as two. Evaluates under both resolution and
     * projection, so it gates "as long as" static buffs (Rampaging Soulrager: +3/+0 while two
     * or more unlocked doors).
     */
    fun UnlockedDoorsAtLeast(count: Int, player: Player = Player.You): ConditionInterface =
        Compare(
            DynamicAmount.UnlockedDoors(player),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * Domain threshold: if [count] or more basic land types are among lands you control.
     * Reads via projected state, so type-changed lands and dual lands count.
     */
    fun BasicLandTypesAtLeast(count: Int): ConditionInterface =
        Compare(
            DynamicAmounts.domain(Player.You),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * If you control N or more permanents (any type).
     * Used as the intervening-if for Ascend triggers (10+ permanents → city's blessing).
     */
    fun ControlPermanentsAtLeast(count: Int): ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Any),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * If you control N or more creatures.
     */
    fun ControlCreaturesAtLeast(count: Int): ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * If you control [count] or more permanents matching [filter].
     *
     * The general-purpose filtered-count form of [ControlCreaturesAtLeast] /
     * [ControlLandsAtLeast] — pass any [GameObjectFilter] (e.g.
     * `GameObjectFilter.Creature.attacking()` for "three or more attacking creatures",
     * Stormbeacon Blade). [YouControl] checks mere existence; this counts the group.
     */
    fun YouControlAtLeast(count: Int, filter: GameObjectFilter): ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.You, filter),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * If you control [count] or more **other** permanents matching [filter] — the source itself is
     * left out of the tally.
     *
     * The "other" of "this land enters tapped unless you control two or more other lands" (Deserted
     * Beach and the rest of the slow lands). It is [DynamicAmount.AggregateBattlefield.excludeSelf],
     * not a predicate on [filter], because self-exclusion is a property of the count rather than of
     * the permanents counted.
     *
     * Write this rather than counting the whole group against one more. The two agree only while the
     * source itself matches [filter] and is already on the battlefield when the condition is
     * checked — true of a land testing lands, and of nothing the signature promises.
     */
    fun YouControlOtherAtLeast(count: Int, filter: GameObjectFilter): ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.You, filter, excludeSelf = true),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * If you control [count] or fewer **other** permanents matching [filter] — the fast lands'
     * "unless you control two or fewer other lands", and [YouControlOtherAtLeast]'s mirror.
     */
    fun YouControlOtherAtMost(count: Int, filter: GameObjectFilter): ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.You, filter, excludeSelf = true),
            ComparisonOperator.LTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * If [count] or more different kinds of counters are among permanents you control matching
     * [filter] (default: creatures). Counts distinct counter kinds across the whole group — a
     * +1/+1 and a finality counter on two creatures is two kinds; the same kind on several
     * permanents counts once. Used for Hundred-Battle Veteran ("three or more different kinds of
     * counters among creatures you control").
     */
    fun DifferentCounterKindsAtLeast(
        count: Int,
        filter: GameObjectFilter = GameObjectFilter.Creature
    ): ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(Player.You, filter, Aggregation.DISTINCT_COUNTER_TYPES),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * If the total number of [counterType] counters among permanents you control matching [filter]
     * is at least [count]. Sums that counter kind across the whole group — three Sagas with one,
     * two, and one lore counter total four. Used for Tom Bombadil ("As long as there are four or
     * more lore counters among Sagas you control"). Pass [CounterTypeFilter.Any] to total every kind.
     */
    fun CounterKindAmongYouControlAtLeast(
        count: Int,
        counterType: CounterTypeFilter,
        filter: GameObjectFilter
    ): ConditionInterface =
        Compare(
            DynamicAmount.AggregateBattlefield(
                player = Player.You,
                filter = filter,
                aggregation = Aggregation.SUM,
                counterType = counterType
            ),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * If you control a creature with a specific keyword.
     */
    fun ControlCreatureWithKeyword(keyword: Keyword): ConditionInterface =
        Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.withKeyword(keyword))

    /**
     * If you control a creature of a specific type.
     */
    fun ControlCreatureOfType(subtype: Subtype): ConditionInterface =
        Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.withSubtype(subtype))

    /**
     * If you control a *permanent* of a specific type — what "if you control a **Rabbit**" asks.
     *
     * The bare tribal noun means any permanent with the subtype, not only a creature with it;
     * [ControlCreatureOfType] is the counterpart for the adjectival "a Rabbit creature". Two
     * facades because Oracle spells the two differently and means two different things.
     */
    fun ControlPermanentOfType(subtype: Subtype): ConditionInterface =
        Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Permanent.withSubtype(subtype))

    /**
     * If a player controls more creatures of the given subtype than each other player.
     */
    fun APlayerControlsMostOfSubtype(subtype: Subtype): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.APlayerControlsMostOfSubtype(subtype)

    /**
     * If the target creature's power is at most the given dynamic amount.
     * Used for cards like Unified Strike.
     */
    fun TargetPowerAtMost(amount: DynamicAmount, targetIndex: Int = 0): ConditionInterface =
        Compare(DynamicAmount.EntityProperty(EntityReference.Target(targetIndex), EntityNumericProperty.Power), ComparisonOperator.LTE, amount)

    /**
     * If the target spell's mana value is at most the given dynamic amount.
     * Used for conditional counterspells like Dispersal Shield.
     */
    fun TargetSpellManaValueAtMost(amount: DynamicAmount, targetIndex: Int = 0): ConditionInterface =
        Compare(DynamicAmount.EntityProperty(EntityReference.Target(targetIndex), EntityNumericProperty.ManaValue), ComparisonOperator.LTE, amount)

    /**
     * If the target permanent has at least one counter of the given type.
     * Used for cards like Bring Low: "If that creature has a +1/+1 counter on it"
     */
    fun TargetHasCounter(counterType: CounterTypeFilter, targetIndex: Int = 0): ConditionInterface =
        Compare(DynamicAmount.EntityProperty(EntityReference.Target(targetIndex), EntityNumericProperty.CounterCount(counterType)), ComparisonOperator.GTE, DynamicAmount.Fixed(1))

    /**
     * If the chosen target at [targetIndex] matches a GameObjectFilter. Resolution-only; a player
     * target never matches a game-object filter (use [TargetIsPlayer] for that). Used for cards
     * like Blessing of Belzenlok: "If it's legendary, it also gains lifelink."
     */
    fun TargetMatchesFilter(filter: GameObjectFilter, targetIndex: Int = 0): ConditionInterface =
        EntityMatches(EffectTarget.ContextTarget(targetIndex), filter)

    /**
     * If the chosen target at [targetIndex] is a creature *card*, tested by the underlying card's
     * printed types rather than projected state. The right test for a face-down permanent (which
     * projects as a typeless 2/2 Creature regardless of what it really is) — e.g. "Reveal target
     * face-down permanent. If it's a creature card, you may turn it face up." (Hauntwoods Shrieker).
     * Also handles a card target in another zone (a graveyard/exile card target), whose printed type
     * survives the zone change — e.g. "Exile target card from a graveyard. When a creature card is
     * exiled this way, …" (Agatha's Soul Cauldron). Resolution-only.
     */
    fun TargetIsCreatureCard(targetIndex: Int = 0): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TargetIsCreatureCard(targetIndex)

    /**
     * If the chosen target at [targetIndex] is a spell on the stack (rather than a permanent).
     * Branches a single "target creature or spell" target — see
     * [com.wingedsheep.sdk.scripting.conditions.TargetIsSpellOnStack] (Aang, Swift Savior). Resolution-only.
     */
    fun TargetIsSpellOnStack(targetIndex: Int = 0): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TargetIsSpellOnStack(targetIndex)

    /**
     * If the context target at [targetIndex] is a player (not a permanent/spell/card).
     * Used for "any target" effects with a player-only follow-up — e.g. Sonic Shrieker's
     * "If a player is dealt damage this way, they discard a card."
     */
    fun TargetIsPlayer(targetIndex: Int = 0): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TargetIsPlayer(targetIndex)

    /**
     * The player who triggered this ability is [player]. Narrows a broad "whenever a player …"
     * trigger to a specific player — e.g. Shinryu, Transcendent Rival gates "When the chosen
     * player loses the game, you win the game" with `TriggeringPlayerIs(Player.ChosenOpponent)`.
     */
    fun TriggeringPlayerIs(player: Player): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TriggeringPlayerIs(player)

    /**
     * [player] has the most life, or is tied for the most life, among all players. The "most life"
     * check a binary comparison can't express. Preacher of the Schism gates its attack triggers with
     * `PlayerHasMostLife(Player.DefendingPlayer)` (the attacked player) and
     * `PlayerHasMostLife(Player.You)`.
     */
    fun PlayerHasMostLife(player: Player): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.PlayerHasMostLife(player)

    /**
     * [player] controls the most permanents matching [filter], or is tied for the most, among all
     * players. The board-count sibling of [PlayerHasMostLife] — same "max over every player" shape
     * a binary comparison can't express. Counts come from the projected battlefield.
     *
     * Wrap it in a per-player loop for "each player who controls the most X" (No Witnesses):
     * `ForEachPlayerEffect(Player.Each, ConditionalEffect(PlayerControlsMostPermanents(Player.You,
     * GameObjectFilter.Creature), …))` — inside the loop `Player.You` is the iterated player.
     */
    fun PlayerControlsMostPermanents(
        player: Player,
        filter: GameObjectFilter = GameObjectFilter.Creature,
    ): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.PlayerControlsMostPermanents(player, filter)

    /**
     * If the context target at [targetIndex] is a tapped battlefield permanent. Branch on a
     * target's tapped state at resolution — e.g. Shackle Slinger's "If it's tapped, put a stun
     * counter on it. Otherwise, tap it."
     */
    fun TargetIsTapped(targetIndex: Int = 0): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TargetIsTapped(targetIndex)

    /**
     * If the context target at [targetIndex] is this permanent (the ability's source). Wrap in
     * [Not] for "another"/"a different permanent" wordings — e.g. Arid Archway's "If another
     * Desert was returned this way".
     */
    fun TargetIsSource(targetIndex: Int = 0): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TargetIsSource(targetIndex)

    /**
     * If the target shares a color with the most common color among all permanents
     * (or a color tied for most common). Used by Tsabo's Assassin.
     */
    fun TargetSharesMostCommonColor(targetIndex: Int = 0): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TargetSharesMostCommonColor(targetIndex)

    /**
     * "If excess damage was dealt this way" — true post-damage when the target creature's
     * marked damage strictly exceeds its (projected) toughness. Chain after `DealDamage`
     * in a composite to fire a payoff on lethal-exceeding damage. Used by Orbital Plunge:
     * `Composite(DealDamage(6, t), Conditional(IfTargetTookExcessDamage(), CreateLander()))`.
     *
     * Semantics caveat: the condition reads marked-damage > toughness on the target as it
     * stands when the chain reaches this step, regardless of which preceding effect dealt
     * the damage. CompositeEffect resolves sub-effects sequentially without an interleaved
     * SBA pass and without firing other triggered abilities mid-chain, so for a "deal N
     * to a target, then check" pipeline the only marked damage in play is the damage just
     * dealt — making this read equivalent to "did the source effect deal excess to the
     * target". A future card that deals damage in multiple steps within the same composite
     * (or chains past SBA somehow) would see cumulative marked damage instead, so prefer
     * a different condition for those shapes.
     */
    fun IfTargetTookExcessDamage(targetIndex: Int = 0): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TargetMarkedDamageExceedsToughness(targetIndex)

    /**
     * If another permanent with the same name as the target is on the battlefield.
     * The target permanent itself is excluded from the comparison. Used by Winnow.
     */
    fun AnotherPermanentWithSameNameAsTarget(targetIndex: Int = 0): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.AnotherPermanentWithSameNameAsTarget(targetIndex)

    /**
     * If [color] is the most common color among all permanents on the battlefield, or is tied
     * for most common. Board-derived, so it works as a `ConditionalStaticAbility` gate. Used by
     * the Invasion djinn cycle (Goham/Halam/Ruham/Sulam/Zanam).
     */
    fun ColorIsMostCommon(color: Color): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.ColorIsMostCommon(color)

    /**
     * If the creature enchanted by the source Aura is legendary.
     */
    fun EnchantedCreatureIsLegendary(): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.EnchantedCreatureIsLegendary

    /**
     * If the permanent enchanted by the source Aura matches [filter] (color, type, etc.).
     * General-purpose; e.g. `EnchantedPermanentMatches(GameObjectFilter.Permanent.anyColorOf(Color.RED, Color.GREEN))`
     * for Essence Leak's "as long as enchanted permanent is red or green".
     */
    fun EnchantedPermanentMatches(filter: com.wingedsheep.sdk.scripting.GameObjectFilter): ConditionInterface =
        EntityMatches(EffectTarget.EnchantedPermanent, filter)

    // =========================================================================
    // Life Total Conditions (via Compare)
    // =========================================================================

    /**
     * If your life total is N or less.
     */
    fun LifeAtMost(threshold: Int): ConditionInterface =
        Compare(DynamicAmount.LifeTotal(Player.You), ComparisonOperator.LTE, DynamicAmount.Fixed(threshold))

    /**
     * If *some* player in the game has [threshold] or less life. Existential — true
     * as soon as any player (you or any opponent, including in multiplayer) matches.
     *
     * Used by cards like Razortrap Gorge ("enters tapped unless a player has 13 or
     * less life"). Distinct from [LifeAtMost], which is `Player.You` only.
     */
    fun APlayerLifeAtMost(threshold: Int): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.APlayerLifeAtMost(threshold)

    /** If every player in the game has [threshold] or less life. */
    fun EachPlayerLifeAtMost(threshold: Int): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.EachPlayerLifeAtMost(threshold)

    /** If at least one opponent has [threshold] or less life. */
    fun AnOpponentLifeAtMost(threshold: Int): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.AnOpponentLifeAtMost(threshold)

    /**
     * If your life total is N or more.
     */
    fun LifeAtLeast(threshold: Int): ConditionInterface =
        Compare(DynamicAmount.LifeTotal(Player.You), ComparisonOperator.GTE, DynamicAmount.Fixed(threshold))

    /**
     * If the controller has taken at most [threshold] turns so far — i.e. it's
     * one of their first [threshold] turns of the game. The counter increments at
     * turn start (so during their first turn it reads 1).
     *
     * Used by Starting Town: "enters tapped unless it's your first, second, or
     * third turn of the game" — pass `threshold = 3`.
     */
    fun ControllerTurnsTakenAtMost(threshold: Int): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.ControllerTurnsTakenAtMost(threshold)

    /**
     * If your life total is at least [amount] greater than your *starting* life total — the
     * "as long as your life total is greater than your starting life total" family. Reads the
     * player's actual starting total (20 / 30 / 40 / 2HG), never a hardcoded 20.
     *
     * `LifeAboveStartingBy(1)` is the plain "greater than your starting life total" reading;
     * Elenda, Saint of Dusk pairs it with `LifeAboveStartingBy(10)` for her second tier.
     */
    fun LifeAboveStartingBy(amount: Int): ConditionInterface =
        Compare(
            DynamicAmount.LifeTotal(Player.You),
            ComparisonOperator.GTE,
            DynamicAmount.Add(DynamicAmount.StartingLifeTotal(Player.You), DynamicAmount.Fixed(amount))
        )

    /**
     * If you have more life than an opponent.
     */
    val MoreLifeThanOpponent: ConditionInterface =
        Compare(DynamicAmount.LifeTotal(Player.You), ComparisonOperator.GT, DynamicAmount.LifeTotal(Player.EachOpponent))

    /**
     * If you have less life than an opponent.
     */
    val LessLifeThanOpponent: ConditionInterface =
        Compare(DynamicAmount.LifeTotal(Player.You), ComparisonOperator.LT, DynamicAmount.LifeTotal(Player.EachOpponent))

    // =========================================================================
    // Hand Conditions (via Compare / Exists)
    // =========================================================================

    /**
     * If you have no cards in hand.
     */
    val EmptyHand: ConditionInterface =
        Exists(Player.You, Zone.HAND, negate = true)

    /**
     * If you have N or more cards in hand.
     */
    fun CardsInHandAtLeast(count: Int): ConditionInterface =
        Compare(DynamicAmount.Count(Player.You, Zone.HAND), ComparisonOperator.GTE, DynamicAmount.Fixed(count))

    /**
     * If you have N or fewer cards in hand.
     */
    fun CardsInHandAtMost(count: Int): ConditionInterface =
        Compare(DynamicAmount.Count(Player.You, Zone.HAND), ComparisonOperator.LTE, DynamicAmount.Fixed(count))

    /**
     * If an opponent has N or fewer cards in hand.
     */
    fun OpponentCardsInHandAtMost(count: Int): ConditionInterface =
        Compare(DynamicAmount.Count(Player.EachOpponent, Zone.HAND), ComparisonOperator.LTE, DynamicAmount.Fixed(count))

    // =========================================================================
    // Graveyard Conditions (via Compare / Exists)
    // =========================================================================

    /**
     * If there are N or more cards matching [filter] in your graveyard. The general form behind
     * [CreatureCardsInGraveyardAtLeast]; use for any "N or more <kind> cards in your graveyard"
     * gate — e.g. Ran and Shaw's "three or more Dragon and/or Lesson cards"
     * (`GameObjectFilter.Any.withAnySubtype("Dragon", "Lesson")`). A card matching the filter in
     * more than one way is still counted once.
     */
    fun CardsInGraveyardMatchingAtLeast(count: Int, filter: GameObjectFilter): ConditionInterface =
        Compare(
            DynamicAmount.Count(Player.You, Zone.GRAVEYARD, filter),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * If there are N or more creature cards in your graveyard.
     */
    fun CreatureCardsInGraveyardAtLeast(count: Int): ConditionInterface =
        CardsInGraveyardMatchingAtLeast(count, GameObjectFilter.Creature)

    /**
     * If there are N or more cards in your graveyard.
     */
    fun CardsInGraveyardAtLeast(count: Int): ConditionInterface =
        Compare(DynamicAmount.Count(Player.You, Zone.GRAVEYARD), ComparisonOperator.GTE, DynamicAmount.Fixed(count))

    /**
     * If there is a card of a specific subtype in your graveyard.
     */
    fun GraveyardContainsSubtype(subtype: Subtype): ConditionInterface =
        Exists(Player.You, Zone.GRAVEYARD, GameObjectFilter.Any.withSubtype(subtype))

    /**
     * If there is at least one card matching [filter] in your graveyard.
     * Compose with [All]/[Any] for multi-type checks, e.g. "an instant card and a sorcery card
     * in your graveyard" (Flow State).
     */
    fun GraveyardContains(filter: GameObjectFilter): ConditionInterface =
        Exists(Player.You, Zone.GRAVEYARD, filter)

    /**
     * Delirium (ability word) — if there are [count] or more card types among cards in your
     * graveyard. The count uses the card types (artifact, battle, creature, enchantment,
     * instant, land, planeswalker, sorcery); a single card with multiple types (e.g. an
     * artifact creature) contributes each of its types once, and the same type across many
     * cards still counts once. The printed threshold is always four, but [count] is
     * parameterized for "N or more card types" variants.
     * Used by Delirium cards (Spineseeker Centipede, Balustrade Wurm).
     */
    fun Delirium(count: Int = 4): ConditionInterface =
        graveyardTypeThreshold(count, Aggregation.DISTINCT_TYPES)

    /**
     * If there are [count] or more distinct *permanent* types (CR 110.4: artifact, battle, creature,
     * enchantment, land, planeswalker) among the cards in your graveyard — Matzalantli, the Great
     * Door's transform gate. This is [Delirium]'s permanent-only sibling: non-permanent card types
     * never count. Instants and sorceries have no permanent type, and a kindred card contributes only
     * its *other* (permanent) type, not "kindred" itself (CR 300.2b). Unlike counting distinct card
     * types among permanent cards, this does not miscount a kindred permanent. A single card with
     * several permanent types (an artifact creature) contributes each once; the same type across many
     * cards still counts once.
     */
    fun DistinctPermanentTypesInGraveyard(count: Int): ConditionInterface =
        graveyardTypeThreshold(count, Aggregation.DISTINCT_PERMANENT_TYPES)

    /** Shared shape for the graveyard type-count gates ([Delirium], [DistinctPermanentTypesInGraveyard]). */
    private fun graveyardTypeThreshold(count: Int, aggregation: Aggregation): ConditionInterface =
        Compare(
            DynamicAmount.AggregateZone(
                player = Player.You,
                zone = Zone.GRAVEYARD,
                aggregation = aggregation
            ),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    // =========================================================================
    // Source Conditions
    // =========================================================================

    /**
     * If you cast this permanent (from any zone). False if it was put onto the
     * battlefield by another effect (reanimation, tokens, "put onto the battlefield").
     * Used for ETB triggers gated on "if you cast it" (e.g., Sunderflock).
     */
    val WasCast: ConditionInterface =
        WasCastCondition

    /**
     * "if it wasn't cast or no mana was spent to cast it" — the free-cast payoff clause.
     * True when no mana at all was spent to put the source onto the battlefield (it wasn't
     * cast, or it was cast for free / for {0}); false if any mana was spent, including mana
     * for additional costs or cost increases on an otherwise-free cast. Used for the OTJ
     * free-cast payoffs (Freestrider Commando, Satoru, the Infiltrator). Compose
     * `All(WasCast, NoManaSpentToCast)` for the narrower "cast, but for free" sense.
     */
    val NoManaSpentToCast: ConditionInterface =
        NoManaSpentToCastCondition

    /**
     * "if none of them were cast or no mana was spent to cast them" — the batch-enters variant of
     * [NoManaSpentToCast]. True iff **every** permanent a batch trigger captured (the
     * `Triggers.OneOrMorePermanentsEnter` batch, exposed at resolution as the `trigger.captured`
     * collection) had no mana spent to cast it; an empty capture is vacuously true. Use as a
     * resolution-time [com.wingedsheep.sdk.dsl.Effects] `ConditionalEffect` gate on the payoff —
     * Satoru, the Infiltrator.
     */
    val NoManaSpentToCastEntered: ConditionInterface =
        NoManaSpentToCastEnteredCondition

    /**
     * "If one or more of them entered from exile or was cast from exile" — the batch-enters,
     * any-of exile counterpart of [TriggeringEntityEnteredOrWasCastFromGraveyard]. Evaluated over
     * the permanents a `Triggers.OneOrMorePermanentsEnter` batch captured; works as a real
     * intervening-"if" (`interveningIf`) as well as a resolution-time gate. Extraordinary
     * Journey.
     */
    val AnyEnteredOrWasCastFromExile: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.AnyEnteredOrWasCastFromExile

    /**
     * If this permanent was cast from your hand.
     * Used for Phage the Untouchable.
     */
    val WasCastFromHand: ConditionInterface =
        WasCastFromHandCondition

    /**
     * If this spell was cast from the specified zone.
     * Used for flashback spells and other zone-dependent effects.
     */
    fun WasCastFromZone(zone: Zone): ConditionInterface =
        WasCastFromZoneCondition(zone)

    /**
     * If the ability's source is *currently* in one of [zones] — a live lookup, unlike
     * [WasCastFromZone]'s frozen cast-time answer.
     *
     * The eminence shape is `SourceInZone(Zone.BATTLEFIELD, Zone.COMMAND)`, gating the ability's
     * effect so CR 603.4's resolution-time re-check holds: an Edgar Markov that leaves both zones
     * after the trigger fires produces nothing.
     */
    fun SourceInZone(vararg zones: Zone): ConditionInterface =
        SourceInZoneCondition(zones.toSet())

    /**
     * If this spell was cast from a graveyard.
     * Used for flashback bonus effects.
     */
    val WasCastFromGraveyard: ConditionInterface =
        WasCastFromZoneCondition(Zone.GRAVEYARD)

    /**
     * If this spell was kicked.
     * Used for kicker spells like Shivan Fire.
     */
    val WasKicked: ConditionInterface =
        WasKickedCondition

    /**
     * If this spell was **bargained** (CR 702.166b, Wilds of Eldraine) — its optional "sacrifice an
     * artifact, enchantment, or token" additional cost was declared as it was cast.
     *
     * A facade over the durable choice-slot read ([com.wingedsheep.sdk.scripting.ChoiceSlot.BARGAINED]),
     * so bargain needs no condition type of its own. Works in both directions of the mechanic:
     * - on a **spell**, as a rider inside the spell's own effect ("If this spell was bargained, that
     *   creature also gains flying and lifelink until end of turn"), read from the declaration the
     *   spell carries on the stack;
     * - on a **permanent**, as an intervening-if on an enters-the-battlefield trigger ("When this
     *   creature enters, if it was bargained, …"), read from the flag stamped durably on the
     *   permanent as it resolved;
     * - as a **cost gate** — `CostGating.OnlyIf(Conditions.WasBargained)` on a `SelfCast`
     *   [com.wingedsheep.sdk.scripting.ModifySpellCost] for "This spell costs {2} less to cast if
     *   it's bargained", where it is evaluated against the cast branch being priced.
     *
     * Never true for a merely *kicked* spell: bargain and kicker are separate facts (CR 702.166c).
     * Pairs with the `bargain()` DSL helper on [CardBuilder].
     */
    val WasBargained: ConditionInterface =
        CastChoiceMadeCondition(com.wingedsheep.sdk.scripting.ChoiceSlot.BARGAINED)

    /**
     * If **evidence was collected** for this spell (CR 701.59c, Murders at Karlov Manor) — its
     * optional "you may collect evidence N" additional cost was declared as it was cast.
     *
     * A facade over the durable choice-slot read
     * ([com.wingedsheep.sdk.scripting.ChoiceSlot.EVIDENCE_COLLECTED]), so the linkage needs no
     * condition type of its own. CR 701.59c makes this a *linked* ability (CR 607): it reads only
     * the declaration made for **this** object's own collect-evidence ability, so a copy or a
     * granted instance answers for itself. Works in all three directions the printed cards use:
     * - on a **spell**, as a rider inside the spell's own effect ("Each opponent sacrifices a
     *   creature of their choice. If evidence was collected, instead …" — Extract a Confession),
     *   read from the declaration the spell carries on the stack;
     * - on a **permanent**, as an intervening-if on an enters-the-battlefield trigger ("When this
     *   creature enters, if evidence was collected, …" — Vitu-Ghazi Inspector), read from the flag
     *   stamped durably on the permanent as it resolved;
     * - as a **cost gate** — `CostGating.OnlyIf(Conditions.WasEvidenceCollected)` on a `SelfCast`
     *   [com.wingedsheep.sdk.scripting.ModifySpellCost] for Bite Down on Crime's "This spell costs
     *   {2} less to cast if evidence was collected", where it is evaluated against the cast branch
     *   being priced.
     *
     * Never true for a merely kicked or bargained spell — the three are separate facts on a shared
     * rail. Pairs with the `collectEvidence()` DSL helper on [CardBuilder].
     */
    val WasEvidenceCollected: ConditionInterface =
        CastChoiceMadeCondition(com.wingedsheep.sdk.scripting.ChoiceSlot.EVIDENCE_COLLECTED)

    /**
     * If this spell was cast **using teamwork** (CR 702.194b, Marvel Super Heroes) — its optional
     * "tap any number of creatures you control with total power N or more" additional cost was
     * declared as it was cast.
     *
     * A facade over the durable choice-slot read ([com.wingedsheep.sdk.scripting.ChoiceSlot.TEAMWORK]),
     * so teamwork needs no condition type of its own. Works in both directions of the mechanic:
     * - on a **spell**, as a rider inside the spell's own effect ("If this spell was cast using
     *   teamwork, it deals 4 damage to that creature instead"), read from the declaration the
     *   spell carries on the stack;
     * - on a **permanent**, as an intervening-if on an enters-the-battlefield trigger, read from
     *   the flag stamped durably on the permanent as it resolved;
     * - as the condition of a `DynamicAmount.Conditional` feeding a modal's `dynamicChooseCount`,
     *   for "choose one; if this spell was cast using teamwork, choose both instead" (CR 700.2 for
     *   the mode count; the declaration it reads is made under CR 601.2b);
     * - as the gate on a teamwork-only clause that has its own target, through the rail's
     *   `kickerTarget` / `kickerEffect` slots (CR 702.194c — the plain cast is announced as though
     *   the clause weren't there).
     *
     * Never true for a merely *kicked* or *bargained* spell: the three are separate facts riding
     * separate slots on the shared optional-additional-cost rail. Pairs with the `teamwork(n)` DSL
     * helper on [CardBuilder].
     */
    val TeamworkWasPaid: ConditionInterface =
        CastChoiceMadeCondition(com.wingedsheep.sdk.scripting.ChoiceSlot.TEAMWORK)

    /**
     * If this spell's sneak cost was paid (CR 702.190 — [com.wingedsheep.sdk.scripting.KeywordAbility.Sneak]).
     * Used for riders like Leonardo, Leader in Blue and The Last Ronin's Technique whose
     * effect changes when the spell was cast for its sneak cost.
     */
    val SneakCostWasPaid: ConditionInterface =
        SneakCostWasPaidCondition

    /**
     * If this spell was cast using web-slinging (CR 702.188 —
     * [com.wingedsheep.sdk.scripting.KeywordAbility.WebSlinging]). Used for riders like
     * Spiders-Man, Heroic Horde and Scarlet Spider, Ben Reilly whose enters-the-battlefield
     * behavior changes when the spell was cast for its web-slinging cost.
     */
    val WebSlungCostWasPaid: ConditionInterface =
        WebSlungCostWasPaidCondition

    /**
     * If this spell's Mayhem cost was paid (CR 702.187 —
     * [com.wingedsheep.sdk.scripting.KeywordAbility.Mayhem]). Used for riders like Sandman's
     * Quicksand whose resolution behavior changes when the spell was cast from the graveyard for
     * its Mayhem cost.
     */
    val MayhemCostWasPaid: ConditionInterface =
        MayhemCostWasPaidCondition

    /**
     * If this spell's blight additional cost was paid (`AdditionalCost.BlightOrPay`).
     * Used for cards like Cinder Strike whose effect changes when the optional
     * Blight path was chosen during casting.
     */
    val BlightWasPaid: ConditionInterface =
        BlightWasPaidCondition

    /**
     * If this spell's optional **waterbend** additional cost was paid
     * ([com.wingedsheep.sdk.scripting.SpellWaterbendCost] with `optional = true`, Avatar: The Last
     * Airbender). Used for cards like Ruinous Waterbending and Spirit Water Revival whose effect
     * changes when "you may waterbend {N}" was paid.
     */
    val WaterbendWasPaid: ConditionInterface =
        WaterbendWasPaidCondition

    /**
     * If this spell's **gift** additional cost was paid — "if the gift was promised"
     * (CR 702.174a/b, Bloomburrow). The promise is elected as the spell is cast and stamped
     * durably on the resulting permanent, so a gift permanent's enters-the-battlefield abilities
     * branch on it: `Conditions.GiftWasPromised` for the gift itself and the riders that need it,
     * `Conditions.Not(Conditions.GiftWasPromised)` for "if the gift wasn't promised" (Kitnap's
     * stun counters).
     *
     * A facade over the durable choice-slot read — gift needs no condition type of its own.
     * Pairs with [com.wingedsheep.sdk.scripting.KeywordAbility.Gift] and the `gift(kind)` DSL
     * helper.
     *
     * **Permanents only.** Unlike `SneakCostWasPaid` / `WaterbendWasPaid` this has no resolution-time
     * fallback for a spell's own effect: the flag is written as the permanent enters, so a read from
     * a still-on-the-stack instant or sorcery is always false. Instants and sorceries branch on the
     * promise through `Patterns.Mechanic.giftSpell`'s mode instead (CR 702.174b gives them
     * "if this spell's gift cost was paid, [effect]" rather than an enters trigger).
     */
    val GiftWasPromised: ConditionInterface =
        CastChoiceMadeCondition(com.wingedsheep.sdk.scripting.ChoiceSlot.GIFT_PROMISED)

    /**
     * If a value was locked in for [slot] when the source was cast / as it entered
     * ("a color was chosen", "this spell was kicked"). Reads the durable cast-choices bag.
     */
    fun CastChoiceMade(slot: com.wingedsheep.sdk.scripting.ChoiceSlot): ConditionInterface =
        CastChoiceMadeCondition(slot)

    /**
     * If the value locked in for [slot] equals [value] (text compare; color uses the enum name).
     * The generic slot reader, e.g. `CastChoiceIs(ChoiceSlot.MODE, "Khans")`.
     */
    fun CastChoiceIs(slot: com.wingedsheep.sdk.scripting.ChoiceSlot, value: String): ConditionInterface =
        CastChoiceIsCondition(slot, value)

    /**
     * If the named cast-time capture [flag] was true *as the source spell was cast* (CR 601.2i).
     * Pairs with the `captureAtCast(flag, condition)` spell DSL: the engine freezes the cast-time
     * answer onto the spell, and this reads it back at resolution regardless of later board changes.
     * Used by Steer Clear ("deals 4 damage instead if you controlled a Mount as you cast this spell").
     */
    fun CapturedAtCast(flag: String): ConditionInterface =
        CastTimeFlagSetCondition(flag)

    /**
     * If specific colored mana was spent to cast this spell.
     * Used for Lorwyn Incarnation cycle (Catharsis, Deceit, etc.)
     * Example: ManaSpentToCastIncludes(requiredWhite = 2) checks if {W}{W} was spent.
     */
    fun ManaSpentToCastIncludes(
        requiredWhite: Int = 0,
        requiredBlue: Int = 0,
        requiredBlack: Int = 0,
        requiredRed: Int = 0,
        requiredGreen: Int = 0
    ): ConditionInterface = com.wingedsheep.sdk.scripting.conditions.ManaSpentToCastIncludes(
        requiredWhite = requiredWhite,
        requiredBlue = requiredBlue,
        requiredBlack = requiredBlack,
        requiredRed = requiredRed,
        requiredGreen = requiredGreen
    )

    /**
     * The unified "an entity matches a filter" primitive: the [entity] (named via the shared
     * [EffectTarget] vocabulary) matches [filter]. Subsumes the older near-clones — `SourceMatches`,
     * `EnchantedPermanentMatches`, `TargetMatchesFilter`, `TriggeringSpellMatchesFilter` — which are
     * now the named [SourceMatches] / [EnchantedPermanentMatches] / [TargetMatchesFilter] /
     * [TriggeringSpellMatches] helpers below. Prefer those helpers for the common roles; reach for
     * `EntityMatches` directly when you need a role they don't name (e.g. the equipped creature).
     *
     * `Self` and the enchanted/equipped attachment roles evaluate in both resolution and
     * projection; `ContextTarget` and `TriggeringEntity` are resolution-only.
     */
    fun EntityMatches(entity: EffectTarget, filter: GameObjectFilter): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.EntityMatches(entity, filter)

    /**
     * If the source permanent matches [filter]. The general source-state primitive behind the
     * `SourceIs*` / `SourceHas*` helpers; `EntityMatches(EffectTarget.Self, filter)`. Works in both
     * resolution and static-ability projection.
     */
    fun SourceMatches(filter: GameObjectFilter): ConditionInterface =
        EntityMatches(EffectTarget.Self, filter)

    /** If this creature is attacking. */
    val SourceIsAttacking: ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.attacking())

    /** If this creature is blocking. */
    val SourceIsBlocking: ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.blocking())

    /** If this permanent is tapped. */
    val SourceIsTapped: ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.tapped())

    /** If this permanent is untapped. */
    val SourceIsUntapped: ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.untapped())

    /** If this creature has dealt damage at least once since entering the battlefield. */
    val SourceHasDealtDamage: ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.hasDealtDamage())

    /** If this creature has dealt combat damage to a player (Saboteur-style payoffs). */
    val SourceHasDealtCombatDamageToPlayer: ConditionInterface =
        SourceMatches(
            com.wingedsheep.sdk.scripting.GameObjectFilter.Any
                .copy(statePredicates = listOf(StatePredicate.HasDealtCombatDamageToPlayer))
        )

    /** If this permanent entered the battlefield this turn. */
    val SourceEnteredThisTurn: ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.enteredThisTurn())

    /**
     * If this permanent is saddled (CR 702.171b). Gates Mount payoffs on "while saddled" /
     * "as long as it's saddled" — evaluates identically at resolution and during projection.
     */
    val SourceIsSaddled: ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.saddled())

    /**
     * If this permanent is suspected (CR 701.60a). Negate it with [Not] for the "if it's **not**
     * suspected" intervening-if that guards MKM's self-suspecting attack triggers (Rubblebelt
     * Braggart) — a suspected permanent can't become suspected again (CR 701.60d), so the check
     * is what stops the trigger from going on the stack at all.
     */
    val SourceIsSuspected: ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.suspected())

    /**
     * If this permanent has the solved designation (CR 719.3b). The gate behind every "Solved —"
     * ability (CR 702.169): as a [com.wingedsheep.sdk.dsl.CardBuilder.solvedStaticAbility]
     * condition, a [com.wingedsheep.sdk.dsl.CardBuilder.solvedTriggeredAbility] intervening-if, or
     * a [com.wingedsheep.sdk.dsl.CardBuilder.solvedActivatedAbility] activation restriction.
     *
     * Negated by [Not] it is the other half of the "To solve" trigger — a Case only becomes solved
     * "if [condition] and this Case is not solved" (CR 719.3a).
     */
    val SourceIsSolved: ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.solved())

    /** If this creature is soulbond-paired with another creature (CR 702.95b). */
    val SourceIsPaired: ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.paired())

    /**
     * If this creature is **unpaired** (CR 702.95b) — the intervening-if of soulbond's second
     * triggered ability, "if you control both that creature and this one and both are unpaired".
     */
    val SourceIsUnpaired: ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.unpaired())

    /**
     * If this creature was declared as an attacker at least once during the current turn.
     * Used by intervening-if triggers like Erg Raiders' "if this creature didn't attack this
     * turn, deal 2 damage to you" (negate via [com.wingedsheep.sdk.scripting.conditions.NotCondition]).
     */
    val SourceAttackedThisTurn: ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.attackedThisTurn())

    /**
     * If this creature was declared as an attacker at least once during the current combat (CR 508.1).
     * Backed by the per-entity `AttackedThisCombatComponent` marker, stamped at attacker-declaration
     * time and cleared when the combat phase ends — so, unlike [SourceAttackedThisTurn], it resets
     * between multiple combats in one turn, and unlike the current `AttackingComponent` it survives
     * the creature being removed from combat.
     */
    val SourceAttackedThisCombat: ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.attackedThisCombat())

    /**
     * If this creature was declared as a blocker at least once during the current combat (CR 509.1).
     * Backed by the per-entity `BlockedThisCombatComponent` marker, stamped at blocker-declaration
     * time and cleared when the combat phase ends. Survives the blocked attacker dying (which clears
     * the live `BlockingComponent`), so it still reads true at end of combat.
     */
    val SourceBlockedThisCombat: ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.blockedThisCombat())

    /**
     * If this creature attacked or blocked at least once during the current combat. Used as the
     * intervening-if on Clockwork Avian's `EachEndOfCombat` counter-shed trigger ("at end of combat,
     * if this creature attacked or blocked this combat, …"). Correctly per-combat: a second combat in
     * the same turn re-evaluates against fresh markers.
     */
    val SourceAttackedOrBlockedThisCombat: ConditionInterface =
        Any(SourceAttackedThisCombat, SourceBlockedThisCombat)

    /**
     * As long as this creature is a specific subtype.
     * Used for conditional static abilities like "has defender as long as it's a Wall."
     */
    fun SourceHasSubtype(subtype: Subtype): ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.withSubtype(subtype))

    /**
     * As long as this creature is blocking or blocked by a creature of one of [subtypes].
     *
     * Source-relative combat condition resolved through the source: on an Equipment/Aura it reads
     * the attached creature, so it gates a static ability granted to the equipped creature. True iff
     * that creature is currently blocking, or being blocked by, a creature with any of the given
     * subtypes (matched any-of against projected state). Used by Sting, the Glinting Dagger:
     * "Equipped creature has first strike as long as it's blocking or blocked by a Goblin or Orc."
     */
    fun SourceIsBlockingOrBlockedBySubtype(subtypes: List<Subtype>): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.SourceIsBlockingOrBlockedBySubtype(
            subtypes.map { it.value }
        )

    /**
     * As long as this creature has a specific keyword.
     * Used for conditional effects like "If this creature has flying, it gets +1/+1."
     */
    fun SourceHasKeyword(keyword: Keyword): ConditionInterface =
        SourceMatches(com.wingedsheep.sdk.scripting.GameObjectFilter.Any.withKeyword(keyword))

    /**
     * While this creature has a counter of the given type on it.
     * Used for intervening-if triggers like Moonshadow.
     */
    fun SourceHasCounter(counterType: CounterTypeFilter): ConditionInterface {
        val predicate: StatePredicate = when (counterType) {
            is CounterTypeFilter.Any -> StatePredicate.HasAnyCounter
            is CounterTypeFilter.PlusOnePlusOne -> StatePredicate.HasCounter("PLUS_ONE_PLUS_ONE")
            is CounterTypeFilter.MinusOneMinusOne -> StatePredicate.HasCounter("MINUS_ONE_MINUS_ONE")
            is CounterTypeFilter.PlusOnePlusZero -> StatePredicate.HasCounter("PLUS_ONE_PLUS_ZERO")
            is CounterTypeFilter.PlusZeroPlusOne -> StatePredicate.HasCounter("PLUS_ZERO_PLUS_ONE")
            is CounterTypeFilter.MinusOneMinusZero -> StatePredicate.HasCounter("MINUS_ONE_MINUS_ZERO")
            is CounterTypeFilter.MinusZeroMinusOne -> StatePredicate.HasCounter("MINUS_ZERO_MINUS_ONE")
            is CounterTypeFilter.Loyalty -> StatePredicate.HasCounter("LOYALTY")
            is CounterTypeFilter.Named -> StatePredicate.HasCounter(
                counterType.name.uppercase().replace(' ', '_')
            )
        }
        return SourceMatches(
            com.wingedsheep.sdk.scripting.GameObjectFilter.Any
                .copy(statePredicates = listOf(predicate))
        )
    }

    /**
     * While this permanent has [count] or more counters of [counterType] on it.
     *
     * The threshold form of [SourceHasCounter] (which only checks for one). This is the gate
     * behind a Station card's `{N+}` symbol (CR 721.2a — "As long as this permanent has N or more
     * charge counters on it, it has [abilities]"): use it as the `condition` of a
     * `staticAbility { }` row, or wrapped in `ActivationRestriction.OnlyIfCondition(...)` for a
     * threshold-gated activated ability. Generic over counter type, so it also serves any other
     * "N+ counters of a kind" gate. Reads the source's counters live, so it tracks counters added
     * or removed after the permanent entered.
     */
    fun SourceCounterCountAtLeast(counterType: String, count: Int): ConditionInterface =
        SourceCounterCountAtLeast(CounterTypeFilter.Named(counterType), count)

    /**
     * While this permanent has [count] or more counters matching [counterType] on it.
     *
     * The [CounterTypeFilter] form of [SourceCounterCountAtLeast]; pass [CounterTypeFilter.Any]
     * for "N or more counters of any kind" gates (Warden of the Inner Sky's "three or more
     * counters on it"), which sums every counter kind on the source.
     */
    fun SourceCounterCountAtLeast(counterType: CounterTypeFilter, count: Int): ConditionInterface =
        Compare(
            DynamicAmount.EntityProperty(
                EntityReference.Source,
                EntityNumericProperty.CounterCount(counterType)
            ),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * While this permanent has at most [count] counters of [counterType] on it.
     *
     * The downward-facing twin of [SourceCounterCountAtLeast] — a countdown gate rather than a
     * threshold. `count = 0` is the "if it has no [kind] counters on it" clause that follows a
     * remove-a-counter step (Thing in the Ice: "remove an ice counter from this creature. Then if
     * it has no ice counters on it, transform it"), which is why it reads the source live rather
     * than off the entry state.
     */
    fun SourceCounterCountAtMost(counterType: String, count: Int): ConditionInterface =
        SourceCounterCountAtMost(CounterTypeFilter.Named(counterType), count)

    /**
     * While this permanent has at most [count] counters matching [counterType] on it.
     *
     * The [CounterTypeFilter] form of [SourceCounterCountAtMost]; pass [CounterTypeFilter.Any] to
     * total every kind.
     */
    fun SourceCounterCountAtMost(counterType: CounterTypeFilter, count: Int): ConditionInterface =
        Compare(
            DynamicAmount.EntityProperty(
                EntityReference.Source,
                EntityNumericProperty.CounterCount(counterType)
            ),
            ComparisonOperator.LTE,
            DynamicAmount.Fixed(count)
        )

    /**
     * If a permanent with the given subtype was sacrificed as part of the cost.
     * Used for cards like Thallid Omnivore: "If a Saproling was sacrificed this way, you gain 2 life."
     */
    fun SacrificedHadSubtype(subtype: String): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.SacrificedPermanentHadSubtype(subtype)

    /**
     * If at least one permanent sacrificed as part of the cost was legendary at the
     * moment of sacrifice. Used by LTR cards like Nasty End and Gríma Wormtongue.
     */
    val SacrificedWasLegendary: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.SacrificedPermanentWasLegendary

    /**
     * If at least one permanent sacrificed as part of the cost was suspected (CR 701.60a) at the
     * moment of sacrifice. Used by MKM's Agency Coroner.
     */
    val SacrificedWasSuspected: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.SacrificedPermanentWasSuspected

    /**
     * If at least one permanent sacrificed "this way" was controlled by the source's
     * controller at the moment of sacrifice. Used to gate the personal half of a
     * symmetric edict (Rise of the Witch-king, LTR).
     */
    val YouSacrificedThisWay: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.YouSacrificedPermanentThisWay

    // =========================================================================
    // Turn Conditions
    //
    // These all compose `Compare(DynamicAmount.TurnTracking(player, key), op, Fixed(n))`
    // around the canonical [com.wingedsheep.sdk.scripting.values.TurnTracker] enum. Counts
    // and accumulators live on per-player components in the engine; the DSL just wraps the
    // comparison with friendlier names.
    // =========================================================================

    private fun trackerAtLeast(
        tracker: com.wingedsheep.sdk.scripting.values.TurnTracker,
        atLeast: Int = 1,
        player: Player = Player.You
    ): ConditionInterface =
        Compare(
            DynamicAmount.TurnTracking(player, tracker),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(atLeast)
        )

    /**
     * If you had **no cards in hand at the beginning of this turn** (Mindstorm Crown).
     *
     * Not the same question as [EmptyHand], which reads your hand *now*. This one reads the
     * snapshot taken in the untap step, so it stays answerable — and stays the same answer — after
     * you have drawn, discarded or cast anything. Any upkeep ability phrased "if you had … at the
     * beginning of this turn" wants this one; "if you have no cards in hand" wants [EmptyHand].
     */
    val YouHadNoCardsInHandAtTurnStart: ConditionInterface =
        Compare(
            DynamicAmount.TurnTracking(
                Player.You,
                com.wingedsheep.sdk.scripting.values.TurnTracker.CARDS_IN_HAND_AT_TURN_START
            ),
            ComparisonOperator.EQ,
            DynamicAmount.Fixed(0)
        )

    /**
     * If you gained life this turn.
     * Used for Lunar Convocation.
     */
    val YouGainedLifeThisTurn: ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.LIFE_GAINED)

    /**
     * If you gained [atLeast] or more life this turn.
     * Used for Scheming Silvertongue ("if you gained 2 or more life this turn").
     */
    fun YouGainedLifeThisTurnAtLeast(atLeast: Int): ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.LIFE_GAINED, atLeast)

    /**
     * If [atLeast] or more cards were put into exile this turn — game-wide, counting every
     * player's cards (summed via [Player.Each]), not just yours. Used for Ennis, Debate
     * Moderator's "if one or more cards were put into exile this turn".
     */
    fun CardsPutIntoExileThisTurn(atLeast: Int = 1): ConditionInterface =
        trackerAtLeast(
            com.wingedsheep.sdk.scripting.values.TurnTracker.CARDS_PUT_INTO_EXILE,
            atLeast,
            Player.Each,
        )

    /**
     * As long as you attacked with [atLeast] or more creatures matching [filter] this turn.
     * Used for cards like Deepway Navigator: "as long as you attacked with three or more
     * Merfolk this turn".
     */
    fun YouAttackedWithCreaturesThisTurn(
        filter: com.wingedsheep.sdk.scripting.GameObjectFilter,
        atLeast: Int
    ): ConditionInterface =
        PlayerAttackedWithCreaturesThisTurn(Player.You, filter, atLeast)

    /**
     * If [atLeast] or more creatures matching [filter] attacked this turn, **whoever declared
     * them** — the player-agnostic sibling of [YouAttackedWithCreaturesThisTurn], for text that
     * says "three or more creatures attacked this turn" rather than "you attacked with three or
     * more" (Case of the Gateway Express). Counts each creature once even if the scopes overlap.
     */
    fun CreaturesAttackedThisTurn(
        atLeast: Int,
        filter: com.wingedsheep.sdk.scripting.GameObjectFilter =
            com.wingedsheep.sdk.scripting.GameObjectFilter.Creature
    ): ConditionInterface =
        PlayerAttackedWithCreaturesThisTurn(Player.Each, filter, atLeast)

    /**
     * Whether [attacker] attacked [defender] this turn (CR 508.6) — declared one or more
     * attackers whose defending player was [defender]. Defaults [defender] to [Player.You].
     * Negate with [Not] for "didn't attack you that turn" (Faramir, Prince of Ithilien).
     */
    fun PlayerAttackedPlayerThisTurn(
        attacker: Player,
        defender: Player = Player.You
    ): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.PlayerAttackedPlayerThisTurn(attacker, defender)

    /**
     * As long as you've cast [atLeast] or more spells matching [filter] this turn.
     * Counts every spell cast — countered, fizzled, or still on the stack all count.
     * Defaults to any spell, matching the typical "you've cast two or more spells
     * this turn" pattern (Brightspear Zealot, Illvoi Infiltrator).
     *
     * Pass [fromZone] to restrict to spells cast from that zone, independently of [filter].
     * Negate with [not] for the Prairie Dog cycle's "you haven't cast a spell from your hand
     * this turn": `not(YouCastSpellsThisTurn(1, fromZone = Zone.HAND))`.
     */
    fun YouCastSpellsThisTurn(
        atLeast: Int,
        filter: com.wingedsheep.sdk.scripting.GameObjectFilter = com.wingedsheep.sdk.scripting.GameObjectFilter.Any,
        fromZone: com.wingedsheep.sdk.core.Zone? = null,
        fromZoneOtherThan: com.wingedsheep.sdk.core.Zone? = null
    ): ConditionInterface =
        PlayerCastSpellsThisTurn(Player.You, filter, atLeast, fromZone, fromZoneOtherThan)

    /**
     * As long as you've drawn [atLeast] or more cards this turn (backed by the per-player
     * `CardsDrawnThisTurnComponent`). Used by Gwaihir the Windlord's conditional cost reduction.
     */
    fun YouDrewCardsThisTurn(atLeast: Int = 1): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.PlayerDrewCardsThisTurn(Player.You, atLeast)

    /**
     * As long as you've activated [atLeast] or more exhaust abilities this turn (CR 702.177),
     * backed by the per-player `ExhaustAbilitiesActivatedThisTurnComponent`.
     */
    fun YouActivatedExhaustAbilitiesThisTurn(atLeast: Int = 1): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.PlayerActivatedExhaustAbilitiesThisTurn(Player.You, atLeast)

    /**
     * As long as you haven't activated an exhaust ability this turn — Elvish Refueler's gate on its
     * "activate exhaust abilities as though they haven't been activated" permission.
     */
    val YouHaventActivatedAnExhaustAbilityThisTurn: ConditionInterface =
        Not(com.wingedsheep.sdk.scripting.conditions.PlayerActivatedExhaustAbilitiesThisTurn(Player.You, 1))

    /**
     * If you've committed a crime this turn (CR Outlaws of Thunder Junction). A crime is committed
     * when you cast a spell, activate an ability, or put a triggered ability on the stack that
     * targets an opponent, anything an opponent controls, and/or a card in an opponent's graveyard.
     *
     * Turn-scoped tracker — stays true for the rest of the turn once any crime is committed. Used by
     * Seize the Secrets ("This spell costs {1} less to cast if you've committed a crime this turn").
     */
    val YouCommittedCrimeThisTurn: ConditionInterface =
        PlayerCommittedCrimeThisTurn(Player.You)

    /**
     * If you've **played a land this turn** (CR 305.1 special land-play action). Optionally qualify by
     * the zone it was played from — `fromZone` requires that specific zone, `fromZoneOtherThan`
     * excludes it (mutually exclusive). Backed by the per-player `LandsPlayedThisTurnComponent`.
     */
    fun YouPlayedLandThisTurn(
        fromZone: com.wingedsheep.sdk.core.Zone? = null,
        fromZoneOtherThan: com.wingedsheep.sdk.core.Zone? = null
    ): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.PlayerPlayedLandThisTurn(Player.You, fromZone, fromZoneOtherThan)

    /**
     * If you've **played a land this turn from a zone other than your hand** (CR 305.1 land-play
     * from graveyard / exile / library) — convenience for
     * [YouPlayedLandThisTurn]`(fromZoneOtherThan = Zone.HAND)`. The land half of Spider-Man 2099's
     * end-step intervening-if; compose with [YouCastSpellsThisTurn]`(1, fromZoneOtherThan = Zone.HAND)`
     * via [any] for the full "played a land or cast a spell this turn from anywhere other than your hand".
     */
    val YouPlayedLandFromNonHandThisTurn: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.PlayerPlayedLandThisTurn(
            Player.You, fromZoneOtherThan = com.wingedsheep.sdk.core.Zone.HAND
        )

    /**
     * If this is the first spell you've cast this turn that mana from a Treasure was
     * spent to cast. Used by Rain of Riches ("The first spell you cast each turn that
     * mana from a Treasure was spent to cast has cascade").
     *
     * Reads the controller's `CastSpellRecord` history and is true only when exactly
     * one record this turn carries `paidWithTreasureMana = true` and that record is
     * the most recent one (i.e., this spell).
     */
    val IsFirstSpellPaidWithTreasureManaCastThisTurn: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.IsFirstSpellPaidWithTreasureManaCastThisTurn

    /**
     * As long as you've lost life this turn.
     * Used for Essence Channeler.
     */
    val YouLostLifeThisTurn: ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.LIFE_LOST)

    /**
     * If you gained or lost life this turn.
     * Used for Star Charter and similar Bloomburrow cards.
     */
    val YouGainedOrLostLifeThisTurn: ConditionInterface =
        AnyCondition(listOf(YouGainedLifeThisTurn, YouLostLifeThisTurn))

    /**
     * If you gained and lost life this turn.
     * Used for Lunar Convocation's second ability.
     */
    val YouGainedAndLostLifeThisTurn: ConditionInterface =
        AllConditions(listOf(YouGainedLifeThisTurn, YouLostLifeThisTurn))

    /**
     * If you attacked this turn (you declared at least one attacker).
     * Used for Mardu Skullhunter, Mardu Hordechief, Wingmate Roc, Arrow Storm, etc.
     */
    val YouAttackedThisTurn: ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.PLAYER_ATTACKED)

    /**
     * If you were dealt combat damage this turn.
     */
    val YouWereDealtCombatDamageThisTurn: ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.DEALT_COMBAT_DAMAGE)

    /**
     * If you've played a land this turn.
     * Used for cards like Rock Jockey ("can't cast unless no land was played").
     */
    val PlayedLandThisTurn: ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.LANDS_PLAYED)

    /**
     * Void: "if a nonland permanent left the battlefield this turn or a spell was warped this turn".
     * Backs the Void ability word from Edge of Eternities. Tokens count as nonland permanents;
     * lands do not. A warped spell satisfies the condition even if it was countered.
     */
    val Void: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.VoidCondition

    /**
     * "If it's day" (CR 731). True only while the game's day/night designation is day — a game that
     * is neither day nor night (its starting state, CR 731.1) does not satisfy this. Mirror of
     * [IsNight].
     */
    val IsDay: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.IsDay

    /**
     * "If it's night" (CR 731). True only while the game's designation is night, never while it's
     * neither. Backs Wolf Strike's "if it's night" rider. Mirror of [IsDay].
     */
    val IsNight: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.IsNight

    /**
     * Celebration: "if two or more nonland permanents entered the battlefield under your control
     * this turn". Backs the Celebration ability word from Wilds of Eldraine (CR 207.2c — an
     * ability word is italic flavor with no rules meaning, so there is no keyword; only this
     * condition).
     *
     * Pure past-event check (per the WOE rulings): the permanents need not still be on the
     * battlefield or still be yours. Tokens count; lands do not (nor does a land creature).
     * Crossing the threshold is all that matters — a third entry changes nothing.
     *
     * Works in both shapes the mechanic ships in:
     *  - `interveningIf = Conditions.Celebration` for the intervening-'if' triggers (CR 603.4 —
     *    checked at trigger time *and* on resolution): Pests of Honor, Lady of Laughter, Ash,
     *    Party Crasher, …
     *  - a `ConditionalStaticAbility` gate for the "as long as …" statics (re-evaluated every
     *    projection): Armory Mice, Grand Ball Guest, Gallant Pie-Wielder, …
     */
    val Celebration: ConditionInterface =
        NonlandPermanentsEnteredThisTurn(atLeast = 2)

    /**
     * "If [atLeast] or more nonland permanents entered the battlefield under [player]'s control
     * this turn" — the general form behind [Celebration], for wordings with a different threshold
     * or a player other than the controller.
     */
    fun NonlandPermanentsEnteredThisTurn(
        atLeast: Int = 1,
        player: Player = Player.You
    ): ConditionInterface =
        trackerAtLeast(
            com.wingedsheep.sdk.scripting.values.TurnTracker.NONLAND_PERMANENTS_ENTERED,
            atLeast,
            player,
        )

    /**
     * "If [atLeast] or more creatures entered the battlefield under [player]'s control this turn" —
     * the creature-typed counterpart of [NonlandPermanentsEnteredThisTurn], e.g. Spider-UK's
     * end-step "if two or more creatures entered the battlefield under your control this turn".
     */
    fun CreaturesEnteredThisTurn(
        atLeast: Int = 1,
        player: Player = Player.You
    ): ConditionInterface =
        trackerAtLeast(
            com.wingedsheep.sdk.scripting.values.TurnTracker.CREATURES_ENTERED_UNDER_CONTROL,
            atLeast,
            player,
        )

    /**
     * If an opponent lost life this turn (from any source).
     * Used for cards like Hired Claw: "Activate only if an opponent lost life this turn"
     */
    val OpponentLostLifeThisTurn: ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.LIFE_LOST, player = Player.EachOpponent)

    /**
     * If [player] lost life this turn (from any source). Use when the wording binds the
     * check to a specific player rather than "an opponent" — e.g. Thought-Stalker Warlock:
     * "choose target opponent. If THEY lost life this turn, …" →
     * `PlayerLostLifeThisTurn(Player.ContextPlayer(0))`.
     */
    fun PlayerLostLifeThisTurn(player: Player): ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.LIFE_LOST, player = player)

    /**
     * If an opponent was dealt combat damage by a legendary creature this turn.
     * Used for cards like Blitzball: "Activate only if an opponent was dealt combat damage by a
     * legendary creature this turn."
     */
    val AnOpponentWasDealtCombatDamageByLegendaryCreatureThisTurn: ConditionInterface =
        trackerAtLeast(
            com.wingedsheep.sdk.scripting.values.TurnTracker.DEALT_COMBAT_DAMAGE_BY_LEGENDARY_CREATURE,
            player = Player.EachOpponent,
        )

    /**
     * If a player was dealt [amount] or more combat damage this turn — existential over *all*
     * players (including you): true when some single player's combat-damage-received running total
     * reaches the threshold. Used by Sidequest: Play Blitzball ("if a player was dealt 6 or more
     * combat damage this turn").
     *
     * Deliberately not a `trackerAtLeast(…, Player.Each)` helper: that path sums every player's
     * combat damage together, which would falsely satisfy the threshold when the total across
     * players reaches it but no single player did.
     */
    fun aPlayerWasDealtCombatDamageThisTurnAtLeast(amount: Int): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.AnyPlayerDealtCombatDamageThisTurnAtLeast(amount)

    // =========================================================================
    // Candidate-player target restrictions (CR 115)
    // =========================================================================
    // These read [Player.Candidate] — "the player being considered as a target" — so they only
    // belong inside `TargetPlayer.restriction` / `TargetOpponent.restriction`. The engine binds
    // each candidate player to `Player.Candidate` while enumerating and (per CR 608.2b)
    // re-validating targets. Used in a normal resolution/projection condition slot, the candidate
    // is unbound and the condition is false.

    /**
     * Candidate-target restriction: the player being targeted lost life this turn.
     * Backs "target player who lost life this turn" (Rix Maadi Guildmage).
     */
    fun candidateLostLifeThisTurn(): ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.LIFE_LOST, player = Player.Candidate)

    /**
     * Candidate-target restriction: the player being targeted has [n] or less life.
     * Backs "target player with N or less life". The restriction is re-checked at resolution
     * (CR 608.2b), so a player who gains above the threshold after being targeted is removed.
     */
    fun candidateLifeAtMost(n: Int): ConditionInterface =
        Compare(
            DynamicAmount.LifeTotal(Player.Candidate),
            ComparisonOperator.LTE,
            DynamicAmount.Fixed(n)
        )

    /**
     * If N or more cards left your graveyard this turn.
     */
    fun CardsLeftGraveyardThisTurn(count: Int): ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.CARDS_LEFT_GRAVEYARD, atLeast = count)

    /**
     * If you've sacrificed a Food this turn.
     */
    val SacrificedFoodThisTurn: ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.FOOD_SACRIFICED)

    /**
     * If you've sacrificed an artifact this turn — Murders at Karlov Manor's artifact-sacrifice
     * payoffs (Suspicious Detonation's cost reduction, Furtive Courier's evasion).
     *
     * Controller-scoped turn history, not a graveyard scan: the artifact having since left the
     * graveyard doesn't clear it, and an opponent sacrificing their own artifact never sets it.
     * The card-type sibling of [SacrificedFoodThisTurn].
     */
    val SacrificedArtifactThisTurn: ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.ARTIFACT_SACRIFICED)

    /**
     * If you descended this turn (CR 700.11) — i.e. at least one nontoken permanent
     * card was put into your graveyard from any zone this turn. Tokens do not count;
     * non-permanent cards (instants, sorceries) do not count.
     *
     * Pass [atLeast] > 1 for the descend N / fathomless descent ability words
     * ("if you descended four or more times this turn").
     *
     * Used by the descend gate on cards like Ruin-Lurker Bat ("At the beginning of
     * your end step, if you descended this turn, scry 1").
     */
    fun YouDescendedThisTurn(atLeast: Int = 1): ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.DESCENDED, atLeast = atLeast)

    /**
     * If [atLeast] or more creature cards were put into your graveyard from anywhere this turn —
     * the creature-typed sibling of [YouDescendedThisTurn]. Tokens don't count (a token isn't a
     * card); the origin zone doesn't matter (battlefield, hand, library, stack all qualify).
     *
     * Controller-scoped turn history, not a graveyard scan: reanimating the creature later in the
     * turn doesn't clear it, and a creature card hitting an *opponent's* graveyard never sets it.
     *
     * Gates Macabre Reconstruction's cost reduction ("This spell costs {2} less to cast if a
     * creature card was put into your graveyard from anywhere this turn").
     */
    fun CreatureCardPutIntoYourGraveyardThisTurn(atLeast: Int = 1): ConditionInterface =
        trackerAtLeast(
            com.wingedsheep.sdk.scripting.values.TurnTracker.CREATURE_CARDS_PUT_INTO_GRAVEYARD,
            atLeast = atLeast,
        )

    /**
     * If [atLeast] or more creature cards were put into graveyards this turn — **game-wide**,
     * counting every player's graveyard (summed via [Player.Each]), not just yours. The
     * player-agnostic sibling of [CreatureCardPutIntoYourGraveyardThisTurn], for Case of the
     * Gorgon's Kiss's "three or more creature cards were put into graveyards from anywhere this
     * turn".
     *
     * The underlying tracker reads the card's own type line, i.e. what it *is in the graveyard*,
     * which is the printed ruling: a creature card that was a noncreature permanent on the
     * battlefield still counts, and a noncreature card animated into a creature does not. Tokens
     * are never counted — a token isn't a card (CR 111.6).
     */
    fun CreatureCardsPutIntoGraveyardsThisTurn(atLeast: Int = 1): ConditionInterface =
        trackerAtLeast(
            com.wingedsheep.sdk.scripting.values.TurnTracker.CREATURE_CARDS_PUT_INTO_GRAVEYARD,
            atLeast = atLeast,
            player = Player.Each,
        )

    /**
     * If [atLeast] or more distinct sources you controlled dealt damage this turn — Case of the
     * Burning Masks. Counts source *objects* at the moment they dealt the damage: a source that
     * pings twice counts once, a source that left and returned counts twice, and one that dies or
     * changes controller afterwards still counts. Abilities are not sources; the source is the
     * object the ability came from.
     */
    fun SourcesYouControlledDealtDamageThisTurn(atLeast: Int): ConditionInterface =
        trackerAtLeast(
            com.wingedsheep.sdk.scripting.values.TurnTracker.DAMAGE_SOURCES,
            atLeast = atLeast,
        )

    /**
     * If you've sacrificed [atLeast] or more permanents this turn (controller-scoped, any
     * permanent type). Backed by the per-player `PermanentsSacrificedThisTurnComponent` —
     * distinct from the game-wide cost-reduction counter. Used by Sawblade Skinripper's
     * intervening-if ("if you sacrificed one or more permanents this turn").
     */
    fun YouSacrificedPermanentsThisTurn(atLeast: Int = 1): ConditionInterface =
        trackerAtLeast(
            com.wingedsheep.sdk.scripting.values.TurnTracker.PERMANENTS_SACRIFICED,
            atLeast = atLeast,
        )

    /**
     * If red sources you controlled dealt [atLeast] or more noncombat damage this turn
     * (controller-scoped). Backed by the per-player `RedNoncombatDamageDealtThisTurnComponent`.
     * Gates Temple of Power's transform-back (back of Ojer Axonil, Deepest Might).
     */
    fun YouDealtRedNoncombatDamageThisTurn(atLeast: Int = 1): ConditionInterface =
        trackerAtLeast(
            com.wingedsheep.sdk.scripting.values.TurnTracker.RED_NONCOMBAT_DAMAGE_DEALT,
            atLeast = atLeast,
        )

    /**
     * If a permanent of the given card type entered the battlefield under the given player's
     * control this turn. The permanent need not still be on the battlefield, still be of that
     * type, or still be under that player's control — only the entry event matters.
     *
     * Used for Mechan Shieldmate (EOE): "As long as an artifact entered the battlefield under
     * your control this turn ..."
     */
    fun PermanentTypeEnteredBattlefieldThisTurn(
        cardType: CardType,
        player: Player = Player.You
    ): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.PermanentTypeEnteredBattlefieldThisTurn(cardType, player)

    /**
     * Shortcut: if an artifact entered the battlefield under your control this turn.
     */
    val ArtifactEnteredBattlefieldThisTurn: ConditionInterface =
        PermanentTypeEnteredBattlefieldThisTurn(CardType.ARTIFACT)

    /**
     * If you put a counter on a creature this turn.
     * Used for Lasting Tarfire.
     */
    val PutCounterOnCreatureThisTurn: ConditionInterface =
        trackerAtLeast(com.wingedsheep.sdk.scripting.values.TurnTracker.COUNTERS_PUT_ON_CREATURE)

    /**
     * Intervening-if: "if a creature died this turn" (global — any controller).
     * Used for cards like Scorpion, Seething Striker.
     */
    val CreatureDiedThisTurn: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.CreatureDiedThisTurnCondition

    /**
     * Intervening-if: "if a creature died under your control this turn" (scoped to the
     * source's controller). Used for Barrensteppe Siege (Mardu).
     */
    val ControlledCreatureDiedThisTurn: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.ControlledCreatureDiedThisTurnCondition

    /**
     * "if a [subtype] creature died this turn" — global, matched against each dying creature's
     * last-known subtypes. Used for "a Goblin died this turn"-style gates.
     */
    fun SubtypeCreatureDiedThisTurn(subtype: Subtype): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.CreatureWithSubtypeDiedThisTurn(subtype.value, present = true)

    /**
     * "if a non-[subtype] creature died this turn" — global, satisfied when at least one creature
     * that died this turn did *not* have [subtype] among its last-known subtypes. Used by Undead
     * Sprinter (DSK): "if a non-Zombie creature died this turn".
     */
    fun NonSubtypeCreatureDiedThisTurn(subtype: Subtype): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.CreatureWithSubtypeDiedThisTurn(subtype.value, present = false)

    /**
     * Intervening-if: "if a permanent you controlled left the battlefield this turn".
     * Per-player (scoped to the source's controller), counts every permanent type
     * including lands and tokens — broader than [CreatureDiedThisTurn]/[ControlledCreatureDiedThisTurn].
     * Used by Shortcut to Mushrooms (LTR).
     */
    val YouHadPermanentLeaveBattlefieldThisTurn: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.PermanentLeftBattlefieldThisTurn(
            com.wingedsheep.sdk.scripting.references.Player.You
        )

    /**
     * If this is the Nth time this ability has resolved this turn.
     * Used for cards like Harvestrite Host.
     */
    fun SourceAbilityResolvedNTimes(count: Int): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.SourceAbilityResolvedNTimesThisTurn(count)

    /**
     * Internal: gate on the plot mechanic's may-cast permission. True when the source
     * card is currently plotted and was plotted on a prior turn (CR 718.2). Cards
     * never reference this directly — the engine's plot handler wires it up.
     */
    val SourcePlottedOnPriorTurn: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.SourcePlottedOnPriorTurn

    /**
     * Internal: the Mayhem gate (CR 702.187b). True when the source card in a graveyard was
     * discarded by its owner this turn. Cards never reference this directly — the engine's Mayhem
     * enumerator and cast-permission check wire it up.
     */
    val YouDiscardedThisCardThisTurn: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.YouDiscardedThisCardThisTurn

    /**
     * If it's the first end step of the turn (not an extra end step inserted by
     * [Effects.AddAdditionalEndSteps]). The loop guard for "there is an additional end step
     * after this step" riders — see Y'shtola Rhul.
     */
    val IsFirstEndStepOfTurn: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.IsFirstEndStepOfTurn

    /**
     * If it's the first combat phase of the turn (not an extra combat phase inserted by
     * [Effects.AddCombatPhase]). The intervening-if / loop guard for "after this phase, there is an
     * additional combat phase" riders — see Balthier and Fran, Genji Glove, Raph & Leo.
     */
    val IsFirstCombatPhaseOfTurn: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.IsFirstCombatPhaseOfTurn

    /**
     * If it's your turn.
     */
    val IsYourTurn: ConditionInterface =
        IsYourTurnCondition

    /**
     * If it's not your turn.
     */
    val IsNotYourTurn: ConditionInterface =
        IsNotYourTurnCondition

    /**
     * If it's [player]'s turn — the [Player]-parametric form of [IsYourTurn]. Wrap in [Not] for
     * "if it's not [player]'s turn" (Scytheclaw Raptor: `Not(IsPlayersTurn(Player.TriggeringPlayer))`).
     */
    fun IsPlayersTurn(player: Player): ConditionInterface =
        IsPlayersTurnCondition(player)

    /**
     * If the current phase matches any of the listed phases.
     * When `yoursOnly = true` (default), also requires that it's the controller's turn.
     */
    fun IsInPhase(vararg phases: Phase, yoursOnly: Boolean = true): ConditionInterface =
        IsInPhaseCondition(phases.toList(), yoursOnly)

    /**
     * If it's your main phase (either precombat or postcombat main, on your turn).
     * Used for cards like Dose of Dawnglow.
     */
    val IsYourMainPhase: ConditionInterface =
        IsInPhaseCondition(listOf(Phase.PRECOMBAT_MAIN, Phase.POSTCOMBAT_MAIN), yoursOnly = true)

    /**
     * If you have the city's blessing (CR 702.131 / 700.5).
     *
     * Granted by Ascend triggers once the controller controls 10+ permanents on
     * ETB; once granted, never lost for the rest of the game.
     */
    val YouHaveCitysBlessing: ConditionInterface =
        PlayerHasCitysBlessing(Player.You)

    /**
     * If you have an enduring story (The Hobbit, CR 702.195).
     *
     * Gained from the **storied** keyword once you control three or more permanents that are
     * artifacts, Sagas, and/or legendary; once gained, never lost for the rest of the game. This is
     * the gate every storied card's payoff half hangs on — see
     * [com.wingedsheep.sdk.dsl.storied].
     */
    val YouHaveEnduringStory: ConditionInterface =
        PlayerHasEnduringStory(Player.You)

    // =========================================================================
    // Speed (Aetherdrift, CR 702.178–702.179)
    // =========================================================================

    /**
     * "if you have max speed" — your speed is exactly 4 (CR 702.179e).
     *
     * This is the gate the `maxSpeed { }` block on [CardBuilder] applies to every ability inside it
     * (CR 702.178a, "as long as your speed is 4, this object has [Ability]"). It is a plain
     * [Compare] over [DynamicAmount.Speed], so it evaluates identically at resolution and during
     * state projection — which is what lets one gate serve static, activated and triggered abilities
     * alike.
     *
     * Equality (not `>=`) matches the rule literally; the engine clamps speed at
     * [com.wingedsheep.sdk.core.Speed.MAX], so the two agree today.
     */
    val YouHaveMaxSpeed: ConditionInterface = HasMaxSpeed(Player.You)

    /** [Player]-parametric "if [player] has max speed" — wrap in [Not] for "doesn't have max speed". */
    fun HasMaxSpeed(player: Player): ConditionInterface =
        Compare(DynamicAmount.Speed(player), ComparisonOperator.EQ, DynamicAmount.Fixed(Speed.MAX))

    /**
     * "if [player]'s speed is less than 4" — the intervening-if of the inherent speed trigger
     * (CR 702.179d), and the gate for any effect that should only fire while there's speed left to
     * gain. A player with no speed reads as 0 (CR 702.179f), so this holds for them too.
     */
    fun SpeedBelowMax(player: Player = Player.You): ConditionInterface =
        Compare(DynamicAmount.Speed(player), ComparisonOperator.LT, DynamicAmount.Fixed(Speed.MAX))

    // =========================================================================
    // Trigger Entity Conditions
    // =========================================================================

    /**
     * If the triggering entity was historic (legendary, artifact, or Saga).
     * Used for Curator's Ward's "if it was historic" intervening-if condition.
     */
    val TriggeringEntityWasHistoric: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TriggeringEntityWasHistoric

    /**
     * If you cast the triggering entity (the entering permanent), as opposed to it being put
     * onto the battlefield by another effect. Sibling of [WasCast] for triggers whose source is
     * a separate permanent (e.g. "whenever a creature you control enters, if you cast it" on
     * The Sibsig Ceremony).
     */
    val TriggeringEntityWasCast: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TriggeringEntityWasCast

    /**
     * Intervening-if: "if no mana was spent to cast it" about the triggering spell (Boromir, Warden
     * of the Tower). Triggering-entity counterpart of [NoManaSpentToCast].
     */
    val TriggeringSpellCastWithoutPayingMana: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TriggeringSpellCastWithoutPayingMana

    /**
     * Intervening-if: "if at least [amount] mana was spent to cast it" about the triggering spell
     * (Sahagin). Threshold counterpart of [TriggeringSpellCastWithoutPayingMana].
     */
    fun TriggeringSpellManaSpentAtLeast(amount: Int): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TriggeringSpellManaSpentAtLeast(amount)

    /**
     * If the triggering entity entered or was cast from a graveyard.
     * Used by Twilight Diviner: "if they entered or were cast from a graveyard".
     */
    val TriggeringEntityEnteredOrWasCastFromGraveyard: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TriggeringEntityEnteredOrWasCastFromGraveyard

    /**
     * If the triggering entity had a -1/-1 counter on it when it left the battlefield.
     * Used as an intervening-if condition on dies/leaves triggers (e.g., Retched Wretch).
     */
    val TriggeringEntityHadMinusOneMinusOneCounter: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TriggeringEntityHadMinusOneMinusOneCounter

    /**
     * If the triggering entity had at least one counter of any kind on it when it left
     * the battlefield. Used as an intervening-if condition on dies/leaves triggers, e.g.
     * Host of the Hereafter: "Whenever this creature or another creature you control dies,
     * if it had counters on it, ...".
     */
    val TriggeringEntityHadCounters: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TriggeringEntityHadCounters

    /**
     * If the triggering entity had [subtype] among its **projected** subtypes when it left the
     * battlefield (CR 603.10 last-known information). Wrap in [Not] for the "if it wasn't a X"
     * wording — e.g. Infernal Vessel's `Not(TriggeringEntityHadSubtype(Subtype.DEMON.value))`,
     * where the Demon type the card grants itself on return is what stops it looping.
     */
    fun TriggeringEntityHadSubtype(subtype: String): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TriggeringEntityHadSubtype(subtype)

    /**
     * If the triggering entity had [cardType] among its **projected** card types when it left the
     * battlefield (CR 603.10 last-known information). The card-type sibling of
     * [TriggeringEntityHadSubtype] — e.g. Tom, Bert, and William's
     * `TriggeringEntityHadCardType(CardType.CREATURE.name)`, where returning as an artifact is what
     * stops the death trigger looping.
     */
    fun TriggeringEntityHadCardType(cardType: String): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TriggeringEntityHadCardType(cardType)

    /**
     * "…**if it's the first time that creature has become tapped this turn**" — the triggering
     * permanent has become tapped exactly once so far this turn (Captain America, Living Legend).
     * `EntityMatches(TriggeringEntity, Any.becameTappedOnlyOnceThisTurn())`.
     *
     * This is the **intervening-`if` half** of that clause, and it belongs in
     * `TriggeredAbility.interveningIf`, not in `triggerRestriction`: CR 603.4 checks a printed "if"
     * both when the trigger event occurs and again as the ability resolves, and this condition reads
     * live state so the second check can actually change the answer — untap the creature and tap it
     * again in response and it has become tapped twice by then, so the ability is removed from the
     * stack. Pair it with `Triggers.becomesTapped(firstTimeEachTurn = true)`, which carries the same
     * clause on the tap *event* for the first check.
     */
    val TriggeringPermanentBecameTappedOnlyOnceThisTurn: ConditionInterface =
        EntityMatches(
            EffectTarget.TriggeringEntity,
            GameObjectFilter.Any.becameTappedOnlyOnceThisTurn()
        )

    /**
     * If the triggering entity was NOT put onto the battlefield by this source's ability.
     * Used to break ETB-trigger loops on cards like Kodama of the East Tree:
     * "if it wasn't put onto the battlefield with this ability". Pair with
     * `MoveCollectionEffect.markEnteredViaSourceAbility = true` on the move that
     * tags the entering permanent.
     */
    val TriggeringEntityWasNotPutByThisSource: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TriggeringEntityWasNotPutByThisSource

    /**
     * If the triggering spell or ability has exactly one target.
     * Reads the triggering entity's TargetsComponent (counts unique chosen targets).
     * Used by cards like Spinerock Tyrant.
     */
    val TriggeringSpellHasSingleTarget: ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.TriggeringSpellHasSingleTarget

    /**
     * If the spell that triggered this ability matches [filter].
     * General intervening-if guard for "whenever you cast a spell, if it's a/an X ..." cards.
     */
    fun TriggeringSpellMatches(filter: com.wingedsheep.sdk.scripting.GameObjectFilter): ConditionInterface =
        EntityMatches(EffectTarget.TriggeringEntity, filter)

    /**
     * If the card discarded to pay this spell/ability's additional discard cost
     * (`Costs.additional.DiscardCards(...)`) matches [filter]. The discarded card is in its
     * owner's graveyard by resolution (CR 608.2), so the filter is checked against that card's
     * graveyard characteristics. Resolution-only.
     *
     * Wrap in [Not] for "wasn't a [type]" wordings — e.g. Grab the Prize: "If the discarded card
     * wasn't a land card, ~ deals 2 damage to each opponent" → `Not(DiscardedCardMatches(Land))`.
     *
     * @param index Which discarded card to test (defaults to the first/only one).
     */
    fun DiscardedCardMatches(
        filter: com.wingedsheep.sdk.scripting.GameObjectFilter,
        index: Int = 0
    ): ConditionInterface =
        EntityMatches(EffectTarget.DiscardedAsCost(index), filter)

    /**
     * If the spell that triggered this ability is the first spell matching [filter] you've cast
     * this turn. True iff the triggering spell matches [filter] and no second matching spell has
     * been cast yet. Composed from [TriggeringSpellMatches] + the [YouCastSpellsThisTurn] count
     * primitive (no bespoke counting logic). Used by Alania, Divergent Storm.
     */
    fun YouCastFirstSpellOfTypeThisTurn(
        filter: com.wingedsheep.sdk.scripting.GameObjectFilter
    ): ConditionInterface =
        All(
            TriggeringSpellMatches(filter),
            Not(YouCastSpellsThisTurn(atLeast = 2, filter = filter))
        )

    // =========================================================================
    // Collection Conditions (pipeline-based)
    // =========================================================================

    /**
     * If a card in the named pipeline collection matches the given filter.
     * Used for "if you did X this way" patterns (e.g., "if you returned a Squirrel card").
     */
    fun CollectionContainsMatch(collection: String, filter: GameObjectFilter = GameObjectFilter.Any): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.CollectionContainsMatch(collection, filter)

    /**
     * If two cards in the named pipeline collection share a card type (CR 205.2a). False for a
     * collection of fewer than two cards. Models "if two cards that share a card type were milled
     * this way" (The Tale of Tamiyo).
     */
    fun CollectionSharesCardType(collection: String): ConditionInterface =
        com.wingedsheep.sdk.scripting.conditions.CollectionSharesCardType(collection)

    // =========================================================================
    // Composite Conditions
    // =========================================================================

    /**
     * All conditions must be true (AND).
     */
    fun All(vararg conditions: ConditionInterface): ConditionInterface =
        AllConditions(conditions.toList())

    /**
     * Any condition must be true (OR).
     */
    fun Any(vararg conditions: ConditionInterface): ConditionInterface =
        AnyCondition(conditions.toList())

    /**
     * Condition must NOT be true.
     */
    fun Not(condition: ConditionInterface): ConditionInterface =
        NotCondition(condition)
}
