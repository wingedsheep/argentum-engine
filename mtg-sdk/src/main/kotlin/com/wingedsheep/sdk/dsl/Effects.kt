package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaEffect
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.AnimateLandEffect
import com.wingedsheep.sdk.scripting.effects.ChooseColorAndGrantProtectionToGroupEffect
import com.wingedsheep.sdk.scripting.effects.ChooseColorAndGrantProtectionToTargetEffect
import com.wingedsheep.sdk.scripting.effects.ChooseCreatureTypeGainControlEffect
import com.wingedsheep.sdk.scripting.effects.CantCastSpellsEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.EachOpponentDiscardsEffect
import com.wingedsheep.sdk.scripting.effects.EachPlayerReturnsPermanentToHandEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.FightEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.effects.GainControlByMostOfSubtypeEffect
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.effects.RemoveKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ReturnSelfToBattlefieldAttachedEffect
import com.wingedsheep.sdk.scripting.effects.DrawUpToEffect
import com.wingedsheep.sdk.scripting.effects.ReadTheRunesEffect
import com.wingedsheep.sdk.scripting.effects.RemoveFromCombatEffect
import com.wingedsheep.sdk.scripting.effects.RepeatCondition
import com.wingedsheep.sdk.scripting.effects.RepeatWhileEffect
import com.wingedsheep.sdk.scripting.effects.ReplaceNextDrawWithEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.effects.ChooseOptionEffect
import com.wingedsheep.sdk.scripting.effects.OptionType
import com.wingedsheep.sdk.scripting.effects.SelectTargetEffect
import com.wingedsheep.sdk.scripting.effects.SeparatePermanentsIntoPilesEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.CreateTreasureTokensEffect
import com.wingedsheep.sdk.scripting.effects.CounterAbilityEffect
import com.wingedsheep.sdk.scripting.effects.CounterSpellEffect
import com.wingedsheep.sdk.scripting.effects.CounterTriggeringSpellEffect
import com.wingedsheep.sdk.scripting.effects.CounterUnlessPaysEffect
import com.wingedsheep.sdk.scripting.effects.CounterUnlessDynamicPaysEffect
import com.wingedsheep.sdk.scripting.effects.ChangeSpellTargetEffect
import com.wingedsheep.sdk.scripting.effects.ChangeTargetEffect
import com.wingedsheep.sdk.scripting.effects.SetCreatureSubtypesEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.TargetRequirement

/**
 * Facade object providing convenient factory methods for creating Effects.
 *
 * Usage:
 * ```kotlin
 * Effects.DealDamage(3, EffectTarget.ContextTarget(0))
 * Effects.DrawCards(2)
 * Effects.GainLife(5)
 * Effects.Composite(effect1, effect2)
 * ```
 */
object Effects {

    // =========================================================================
    // Damage Effects
    // =========================================================================

    /**
     * Deal damage to a target.
     * No default — every damage effect must explicitly declare its target.
     */
    fun DealDamage(amount: Int, target: EffectTarget): Effect =
        DealDamageEffect(amount, target)

    /**
     * Deal dynamic damage to a target.
     * Used for effects like "deal damage equal to the number of lands you control".
     */
    fun DealDamage(amount: DynamicAmount, target: EffectTarget): Effect =
        DealDamageEffect(amount, target)

    /**
     * Deal X damage to a target, where X is the spell's X value.
     * Used for X spells like Blaze.
     */
    fun DealXDamage(target: EffectTarget): Effect =
        DealDamageEffect(DynamicAmount.XValue, target)

    /**
     * Two creatures fight — each deals damage equal to its power to the other.
     */
    fun Fight(target1: EffectTarget, target2: EffectTarget): Effect =
        FightEffect(target1, target2)

    // =========================================================================
    // Life Effects
    // =========================================================================

    /**
     * Gain life. Default target is the controller.
     */
    fun GainLife(amount: Int, target: EffectTarget = EffectTarget.Controller): Effect =
        GainLifeEffect(amount, target)

    /**
     * Lose life. Default target is target opponent.
     */
    fun LoseLife(amount: Int, target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent)): Effect =
        LoseLifeEffect(amount, target)

    /**
     * Lose dynamic life amount. Default target is target opponent.
     */
    fun LoseLife(amount: DynamicAmount, target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent)): Effect =
        LoseLifeEffect(amount, target)

    /**
     * Lose half your life, rounded up.
     * Composes as LoseLifeEffect with Divide(LifeTotal, 2).
     *
     * @param roundUp Whether to round up (true) or down (false)
     * @param target Who loses life (default: Controller)
     * @param lifePlayer Which player's life total to halve (default: Player.You for controller)
     */
    fun LoseHalfLife(
        roundUp: Boolean = true,
        target: EffectTarget = EffectTarget.Controller,
        lifePlayer: Player = Player.You
    ): Effect = LoseLifeEffect(
        amount = DynamicAmount.Divide(
            DynamicAmount.LifeTotal(lifePlayer),
            DynamicAmount.Fixed(2),
            roundUp = roundUp
        ),
        target = target
    )

    // =========================================================================
    // Card Drawing Effects
    // =========================================================================

    /**
     * Draw cards. Default target is the controller.
     */
    fun DrawCards(count: Int, target: EffectTarget = EffectTarget.Controller): Effect =
        DrawCardsEffect(count, target)

    /**
     * Draw a dynamic number of cards. Default target is the controller.
     */
    fun DrawCards(count: DynamicAmount, target: EffectTarget = EffectTarget.Controller): Effect =
        DrawCardsEffect(count, target)

    /**
     * Discard cards. Default target is target opponent.
     */
    fun Discard(count: Int, target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent)): Effect =
        EffectPatterns.discardCards(count, target)

    /**
     * Discard cards at random. Default target is target opponent.
     */
    fun DiscardRandom(count: Int, target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent)): Effect =
        EffectPatterns.discardRandom(count, target)

    /**
     * Target player discards their entire hand.
     */
    fun DiscardHand(target: EffectTarget = EffectTarget.Controller): Effect =
        EffectPatterns.discardHand(target)

    /**
     * Each player draws X cards, where X is the spell's X value.
     */
    fun EachPlayerDrawsX(
        includeController: Boolean = true,
        includeOpponents: Boolean = true
    ): Effect = EffectPatterns.eachPlayerDrawsX(includeController, includeOpponents)

    /**
     * Draw up to N cards. The player chooses how many (0 to maxCards).
     */
    fun DrawUpTo(maxCards: Int, target: EffectTarget = EffectTarget.Controller): Effect =
        DrawUpToEffect(maxCards, target)

    /**
     * Each player may draw up to N cards. Optionally gain life for each card not drawn.
     * "Each player may draw up to two cards. For each card less than two a player
     * draws this way, that player gains 2 life."
     */
    fun EachPlayerMayDraw(maxCards: Int, lifePerCardNotDrawn: Int = 0): Effect =
        EffectPatterns.eachPlayerMayDraw(maxCards, lifePerCardNotDrawn)

    /**
     * Draw X cards, then for each card drawn, discard a card unless you sacrifice a permanent.
     * Used for Read the Runes.
     */
    fun ReadTheRunes(): Effect = ReadTheRunesEffect

    /**
     * Each opponent discards N cards.
     * Note: when used as a replacement effect in ReplaceNextDraw (Words of Waste cycle),
     * this must stay as EachOpponentDiscardsEffect — the draw replacement system checks
     * the concrete type. Use EffectPatterns.eachOpponentDiscards() for triggered ability contexts.
     */
    fun EachOpponentDiscards(count: Int = 1): Effect = EachOpponentDiscardsEffect(count)

    /**
     * Each player returns a permanent they control to its owner's hand.
     * Used as a replacement in Words of Wind:
     * ReplaceNextDraw(Effects.EachPlayerReturnPermanentToHand())
     */
    fun EachPlayerReturnPermanentToHand(): Effect = EachPlayerReturnsPermanentToHandEffect

    /**
     * Replace the next draw this turn with the given effect instead.
     * Used by the "Words of" enchantment cycle.
     *
     * Examples:
     * ```kotlin
     * ReplaceNextDraw(Effects.GainLife(5))                                   // Words of Worship
     * ReplaceNextDraw(Effects.EachPlayerReturnPermanentToHand())             // Words of Wind
     * ReplaceNextDraw(Effects.EachOpponentDiscards(1))                       // Words of Waste
     * ReplaceNextDraw(Effects.DealDamage(2, EffectTarget.ContextTarget(0)))  // Words of War
     * ReplaceNextDraw(Effects.CreateToken(2, 2, setOf(Color.GREEN), setOf("Bear")))  // Words of Wilding
     * ```
     */
    fun ReplaceNextDraw(effect: Effect): Effect = ReplaceNextDrawWithEffect(effect)

    // =========================================================================
    // Destruction Effects
    // =========================================================================

    /**
     * Destroy a target.
     */
    fun Destroy(target: EffectTarget): Effect =
        MoveToZoneEffect(target, Zone.GRAVEYARD, byDestruction = true)

    /**
     * Exile a target.
     */
    fun Exile(target: EffectTarget): Effect =
        MoveToZoneEffect(target, Zone.EXILE)

    /**
     * Exile a target until the beginning of the next end step.
     * Used by Astral Slide-style effects.
     */
    fun ExileUntilEndStep(target: EffectTarget): Effect =
        EffectPatterns.exileUntilEndStep(target)

    /**
     * Return to hand.
     */
    fun ReturnToHand(target: EffectTarget): Effect =
        MoveToZoneEffect(target, Zone.HAND)

    /**
     * Put on top of library.
     */
    fun PutOnTopOfLibrary(target: EffectTarget): Effect =
        MoveToZoneEffect(target, Zone.LIBRARY, ZonePlacement.Top)

    /**
     * Shuffle into library.
     */
    fun ShuffleIntoLibrary(target: EffectTarget): Effect =
        MoveToZoneEffect(target, Zone.LIBRARY, ZonePlacement.Shuffled)

    /**
     * Put onto the battlefield.
     */
    fun PutOntoBattlefield(target: EffectTarget, tapped: Boolean = false): Effect =
        MoveToZoneEffect(target, Zone.BATTLEFIELD, if (tapped) ZonePlacement.Tapped else ZonePlacement.Default)

    /**
     * Put onto the battlefield under your control (the effect controller's control).
     */
    fun PutOntoBattlefieldUnderYourControl(target: EffectTarget): Effect =
        MoveToZoneEffect(target, Zone.BATTLEFIELD, controllerOverride = EffectTarget.Controller)

    /**
     * Return this card from its current zone to the battlefield attached to the target.
     * Used by the Dragon aura cycle (Dragon Shadow, Dragon Breath, etc.).
     */
    fun ReturnSelfToBattlefieldAttached(target: EffectTarget = EffectTarget.TriggeringEntity): Effect =
        ReturnSelfToBattlefieldAttachedEffect(target)

    // =========================================================================
    // Stat Modification Effects
    // =========================================================================

    /**
     * Modify power and toughness.
     */
    fun ModifyStats(power: Int, toughness: Int, target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        ModifyStatsEffect(power, toughness, target)

    /**
     * Modify power and toughness by dynamic amounts.
     * Used for effects like "Target creature gets -X/-X where X is the number of Zombies."
     */
    fun ModifyStats(power: DynamicAmount, toughness: DynamicAmount, target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        ModifyStatsEffect(power, toughness, target)

    /**
     * Grant a keyword until end of turn.
     */
    fun GrantKeyword(keyword: Keyword, target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        GrantKeywordUntilEndOfTurnEffect(keyword.name, target)

    /**
     * Grant an ability flag until end of turn.
     */
    fun GrantKeyword(flag: AbilityFlag, target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        GrantKeywordUntilEndOfTurnEffect(flag.name, target)

    /**
     * Remove a keyword from a single target.
     * "It loses defender."
     */
    fun RemoveKeyword(keyword: Keyword, target: EffectTarget = EffectTarget.ContextTarget(0), duration: Duration = Duration.EndOfTurn): Effect =
        RemoveKeywordUntilEndOfTurnEffect(keyword.name, target, duration)

    /**
     * Remove an ability flag from a single target.
     */
    fun RemoveKeyword(flag: AbilityFlag, target: EffectTarget = EffectTarget.ContextTarget(0), duration: Duration = Duration.EndOfTurn): Effect =
        RemoveKeywordUntilEndOfTurnEffect(flag.name, target, duration)

    /**
     * Set creature subtypes for a single target.
     * "It becomes a Bird Giant."
     */
    fun SetCreatureSubtypes(subtypes: Set<String>, target: EffectTarget = EffectTarget.Self, duration: Duration = Duration.Permanent): Effect =
        SetCreatureSubtypesEffect(subtypes, target, duration)

    /**
     * Add counters.
     */
    fun AddCounters(counterType: String, count: Int, target: EffectTarget): Effect =
        AddCountersEffect(counterType, count, target)

    /**
     * Distribute any number of counters from this creature onto other creatures.
     * Used for Forgotten Ancient's upkeep ability.
     */
    fun DistributeCountersFromSelf(counterType: String = "+1/+1"): Effect =
        com.wingedsheep.sdk.scripting.effects.DistributeCountersFromSelfEffect(counterType)

    // =========================================================================
    // Mana Effects
    // =========================================================================

    /**
     * Add mana of a specific color.
     */
    fun AddMana(color: Color, amount: Int = 1): Effect =
        AddManaEffect(color, amount)

    /**
     * Add a dynamic amount of mana of a specific color.
     * Used for effects like "Add {R} for each Goblin on the battlefield."
     */
    fun AddMana(color: Color, amount: DynamicAmount): Effect =
        AddManaEffect(color, amount)

    /**
     * Add colorless mana.
     */
    fun AddColorlessMana(amount: Int): Effect =
        AddColorlessManaEffect(amount)

    /**
     * Add a dynamic amount of colorless mana.
     */
    fun AddColorlessMana(amount: DynamicAmount): Effect =
        AddColorlessManaEffect(amount)

    /**
     * Add one mana of any color.
     */
    fun AddAnyColorMana(amount: Int = 1): Effect =
        AddAnyColorManaEffect(amount)

    // =========================================================================
    // Token Effects
    // =========================================================================

    /**
     * Create creature tokens.
     */
    fun CreateToken(
        power: Int,
        toughness: Int,
        colors: Set<Color> = emptySet(),
        creatureTypes: Set<String>,
        keywords: Set<Keyword> = emptySet(),
        count: Int = 1
    ): Effect = CreateTokenEffect(count, power, toughness, colors, creatureTypes, keywords)

    /**
     * Create Treasure tokens.
     */
    fun CreateTreasure(count: Int = 1): Effect =
        CreateTreasureTokensEffect(count)

    // =========================================================================
    // Library Effects
    // =========================================================================

    /**
     * Shuffle a player's graveyard into their library.
     */
    fun ShuffleGraveyardIntoLibrary(target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        EffectPatterns.shuffleGraveyardIntoLibrary(target)

    /**
     * Search library for cards.
     */
    fun SearchLibrary(
        filter: GameObjectFilter,
        count: Int = 1,
        destination: SearchDestination = SearchDestination.HAND,
        entersTapped: Boolean = false,
        shuffle: Boolean = true,
        reveal: Boolean = false
    ): Effect = EffectPatterns.searchLibrary(filter, count, destination, entersTapped, shuffle, reveal)

    /**
     * Search library for a card, shuffle, and put it Nth from the top.
     * E.g., positionFromTop = 2 for "third from the top" (Long-Term Plans).
     */
    fun SearchLibraryNthFromTop(
        filter: GameObjectFilter = GameObjectFilter.Any,
        positionFromTop: Int = 2
    ): Effect = EffectPatterns.searchLibraryNthFromTop(filter, positionFromTop)

    /**
     * Scry N.
     */
    fun Scry(count: Int): Effect =
        EffectPatterns.scry(count)

    /**
     * Surveil N.
     */
    fun Surveil(count: Int): Effect =
        EffectPatterns.surveil(count)

    /**
     * Mill N cards.
     */
    fun Mill(count: Int, target: EffectTarget = EffectTarget.Controller): Effect =
        EffectPatterns.mill(count, target)

    /**
     * Mill a dynamic number of cards.
     */
    fun Mill(count: DynamicAmount, target: EffectTarget = EffectTarget.Controller): Effect =
        EffectPatterns.mill(count, target)

    /**
     * Head Games — Target opponent puts cards from their hand on top of their library.
     * Search that player's library for that many cards. The player puts those cards
     * into their hand, then shuffles.
     */
    fun HeadGames(target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        EffectPatterns.headGames(target)

    /**
     * Each player may reveal any number of creature cards from their hand.
     * Then each player creates tokens for each card they revealed.
     */
    fun EachPlayerRevealCreaturesCreateTokens(
        tokenPower: Int,
        tokenToughness: Int,
        tokenColors: Set<Color>,
        tokenCreatureTypes: Set<String>,
        tokenImageUri: String? = null
    ): Effect = EffectPatterns.eachPlayerRevealCreaturesCreateTokens(
        tokenPower, tokenToughness, tokenColors, tokenCreatureTypes, tokenImageUri
    )

    /**
     * Each player may search their library for up to X cards matching a filter.
     */
    fun EachPlayerSearchesLibrary(
        filter: GameObjectFilter,
        count: DynamicAmount
    ): Effect = EffectPatterns.eachPlayerSearchesLibrary(filter, count)

    // =========================================================================
    // Protection Effects
    // =========================================================================

    /**
     * Choose a color and grant protection from that color to a group of creatures.
     * "Choose a color. Creatures you control gain protection from the chosen color until end of turn."
     */
    fun ChooseColorAndGrantProtection(
        filter: GroupFilter = GroupFilter(GameObjectFilter.Creature.youControl()),
        duration: Duration = Duration.EndOfTurn
    ): Effect = ChooseColorAndGrantProtectionToGroupEffect(filter, duration)

    /**
     * Choose a color and grant protection from that color to a single target.
     * "{W}: This creature gains protection from the color of your choice until end of turn."
     */
    fun ChooseColorAndGrantProtectionToTarget(
        target: EffectTarget = EffectTarget.Self,
        duration: Duration = Duration.EndOfTurn
    ): Effect = ChooseColorAndGrantProtectionToTargetEffect(target, duration)

    // =========================================================================
    // Control Effects
    // =========================================================================

    /**
     * Gain control of target permanent.
     */
    fun GainControl(target: EffectTarget, duration: Duration = Duration.Permanent): Effect =
        GainControlEffect(target, duration)

    /**
     * The player who controls the most creatures of the given subtype gains control of the target.
     */
    fun GainControlByMostOfSubtype(subtype: Subtype, target: EffectTarget = EffectTarget.Self): Effect =
        GainControlByMostOfSubtypeEffect(subtype, target)

    /**
     * Gain control of all creatures matching a filter until end of turn.
     */
    fun GainControlOfGroup(filter: GroupFilter = GroupFilter.AllCreatures, duration: Duration = Duration.EndOfTurn): Effect =
        ForEachInGroupEffect(
            filter = filter,
            effect = GainControlEffect(EffectTarget.Self, duration)
        )

    /**
     * Choose a creature type. If you control more creatures of that type than each
     * other player, gain control of all creatures of that type.
     */
    fun ChooseCreatureTypeGainControl(duration: Duration = Duration.Permanent): Effect =
        ChooseCreatureTypeGainControlEffect(duration)

    // =========================================================================
    // Composite Effects
    // =========================================================================

    /**
     * Combine multiple effects.
     */
    fun Composite(vararg effects: Effect): Effect =
        CompositeEffect(effects.toList())

    /**
     * Combine multiple effects from a list.
     */
    fun Composite(effects: List<Effect>): Effect =
        CompositeEffect(effects)

    /**
     * Repeat a body effect in a do-while loop controlled by a repeat condition.
     *
     * The body executes at least once. After each iteration, the condition determines
     * whether to repeat.
     *
     * Example (Trade Secrets):
     * ```kotlin
     * RepeatWhile(
     *     body = Composite(DrawCards(2, target), DrawUpTo(4)),
     *     repeatCondition = RepeatCondition.PlayerChooses(target, "Repeat?")
     * )
     * ```
     */
    fun RepeatWhile(body: Effect, repeatCondition: RepeatCondition): Effect =
        RepeatWhileEffect(body, repeatCondition)

    // =========================================================================
    // Counter Effects
    // =========================================================================

    /**
     * Counter target spell.
     */
    fun CounterSpell(): Effect =
        CounterSpellEffect

    /**
     * Counter the spell that triggered this ability (non-targeted).
     * "Counter that spell."
     */
    fun CounterTriggeringSpell(): Effect =
        CounterTriggeringSpellEffect

    /**
     * Counter target spell unless its controller pays a mana cost.
     * "Counter target spell unless its controller pays {cost}."
     */
    fun CounterUnlessPays(cost: String): Effect =
        CounterUnlessPaysEffect(ManaCost.parse(cost))

    /**
     * Counter target spell unless its controller pays a dynamic generic mana cost.
     * "Counter target spell unless its controller pays {2} for each Wizard on the battlefield."
     */
    fun CounterUnlessDynamicPays(amount: DynamicAmount): Effect =
        CounterUnlessDynamicPaysEffect(amount)

    /**
     * Counter target activated or triggered ability.
     * "Counter target activated or triggered ability."
     */
    fun CounterAbility(): Effect =
        CounterAbilityEffect

    /**
     * Change the target of a spell to another creature.
     * "If target spell has only one target and that target is a creature, change that spell's target to another creature."
     */
    fun ChangeSpellTarget(targetMustBeSource: Boolean = false): Effect =
        ChangeSpellTargetEffect(targetMustBeSource)

    /**
     * Change the target of target spell or ability with a single target.
     * "Change the target of target spell or ability with a single target."
     */
    fun ChangeTarget(): Effect =
        ChangeTargetEffect

    // =========================================================================
    // Sacrifice Effects
    // =========================================================================

    /**
     * Force a player to sacrifice permanents matching a filter.
     */
    fun Sacrifice(filter: GameObjectFilter, count: Int = 1, target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent)): Effect =
        ForceSacrificeEffect(filter, count, target)

    // =========================================================================
    // Tap/Untap Effects
    // =========================================================================

    /**
     * Tap a target.
     */
    fun Tap(target: EffectTarget): Effect =
        TapUntapEffect(target, tap = true)

    /**
     * Untap a target.
     */
    fun Untap(target: EffectTarget): Effect =
        TapUntapEffect(target, tap = false)

    /**
     * Untap all creatures matching a filter.
     */
    fun UntapGroup(filter: GroupFilter = GroupFilter.AllCreatures): Effect =
        ForEachInGroupEffect(
            filter = filter,
            effect = TapUntapEffect(EffectTarget.Self, tap = false)
        )

    /**
     * Tap all creatures matching a filter.
     */
    fun TapAll(filter: GroupFilter): Effect =
        ForEachInGroupEffect(
            filter = filter,
            effect = TapUntapEffect(EffectTarget.Self, tap = true)
        )

    /**
     * Destroy all permanents matching a filter.
     */
    fun DestroyAll(filter: GroupFilter, noRegenerate: Boolean = false): Effect =
        ForEachInGroupEffect(
            filter = filter,
            effect = MoveToZoneEffect(EffectTarget.Self, Zone.GRAVEYARD, byDestruction = true),
            noRegenerate = noRegenerate
        )

    /**
     * Grant a keyword to all creatures matching a filter.
     */
    fun GrantKeywordToAll(
        keyword: Keyword,
        filter: GroupFilter,
        duration: Duration = Duration.EndOfTurn
    ): Effect =
        ForEachInGroupEffect(
            filter = filter,
            effect = GrantKeywordUntilEndOfTurnEffect(keyword.name, EffectTarget.Self, duration)
        )

    /**
     * Remove a keyword from all creatures matching a filter.
     * "All other creatures lose flying until end of turn."
     */
    fun RemoveKeywordFromAll(
        keyword: Keyword,
        filter: GroupFilter,
        duration: Duration = Duration.EndOfTurn
    ): Effect =
        ForEachInGroupEffect(
            filter = filter,
            effect = RemoveKeywordUntilEndOfTurnEffect(keyword.name, EffectTarget.Self, duration)
        )

    /**
     * Modify stats for all creatures matching a filter.
     */
    fun ModifyStatsForAll(
        power: Int,
        toughness: Int,
        filter: GroupFilter,
        duration: Duration = Duration.EndOfTurn
    ): Effect =
        ForEachInGroupEffect(
            filter = filter,
            effect = ModifyStatsEffect(power, toughness, EffectTarget.Self, duration)
        )

    /**
     * Deal damage to all creatures matching a filter.
     */
    fun DealDamageToAll(amount: Int, filter: GroupFilter): Effect =
        ForEachInGroupEffect(
            filter = filter,
            effect = DealDamageEffect(amount, EffectTarget.Self)
        )

    /**
     * Deal dynamic damage to all creatures matching a filter.
     */
    fun DealDamageToAll(amount: DynamicAmount, filter: GroupFilter): Effect =
        ForEachInGroupEffect(
            filter = filter,
            effect = DealDamageEffect(amount, EffectTarget.Self)
        )

    // =========================================================================
    // Special Effects
    // =========================================================================

    /**
     * Separate permanents into piles (Liliana ultimate style).
     */
    fun SeparatePermanentsIntoPiles(target: EffectTarget = EffectTarget.PlayerRef(Player.TargetPlayer)): Effect =
        SeparatePermanentsIntoPilesEffect(target)

    // =========================================================================
    // Player Restriction Effects
    // =========================================================================

    /**
     * Target player can't cast spells this turn.
     * Used for cards like Xantid Swarm.
     */
    fun CantCastSpells(target: EffectTarget = EffectTarget.PlayerRef(Player.Opponent), duration: Duration = Duration.EndOfTurn): Effect =
        CantCastSpellsEffect(target, duration)

    // =========================================================================
    // Combat Effects
    // =========================================================================

    /**
     * Provoke: untap target creature and force it to block the source creature if able.
     */
    fun Provoke(target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        com.wingedsheep.sdk.scripting.effects.ProvokeEffect(target)

    /**
     * Prevent all combat damage that would be dealt to and dealt by a creature this turn.
     */
    fun PreventCombatDamageToAndBy(target: EffectTarget = EffectTarget.Self): Effect =
        com.wingedsheep.sdk.scripting.effects.PreventCombatDamageToAndByEffect(target)

    /**
     * Remove a creature from combat.
     */
    fun RemoveFromCombat(target: EffectTarget): Effect =
        RemoveFromCombatEffect(target)

    // =========================================================================
    // Animate Effects
    // =========================================================================

    /**
     * Target land becomes an X/Y creature until end of turn. It's still a land.
     */
    fun AnimateLand(
        target: EffectTarget = EffectTarget.ContextTarget(0),
        power: Int = 1,
        toughness: Int = 1,
        duration: Duration = Duration.EndOfTurn
    ): Effect = AnimateLandEffect(target, power, toughness, duration)

    // =========================================================================
    // Pipeline Targeting
    // =========================================================================

    /**
     * Select a target during effect resolution (mid-pipeline).
     * The selected target IDs are stored in a named collection for use by
     * subsequent pipeline effects via [EffectTarget.PipelineTarget].
     */
    fun SelectTarget(requirement: TargetRequirement, storeAs: String = "pipelineTarget"): Effect =
        SelectTargetEffect(requirement, storeAs)

    /**
     * Generic pipeline step: choose an option from a set (creature type, color, etc.)
     * and store the result in EffectContext.chosenValues[storeAs].
     *
     * Downstream pipeline effects can read the chosen value to condition their behavior.
     */
    fun ChooseOption(
        optionType: OptionType,
        storeAs: String = "chosenOption",
        prompt: String? = null,
        excludedOptions: List<String> = emptyList()
    ): Effect = ChooseOptionEffect(optionType, storeAs, prompt, excludedOptions)

    // =========================================================================
    // Composite Effect Helpers
    // =========================================================================

    /**
     * Draw cards then discard cards (looting).
     * "Draw a card, then discard a card."
     */
    fun Loot(draw: Int = 1, discard: Int = 1): Effect = CompositeEffect(
        listOf(
            DrawCardsEffect(draw, EffectTarget.Controller),
            EffectPatterns.discardCards(discard)
        )
    )

    /**
     * Deal damage to target and gain that much life.
     * "Deal X damage to target and you gain X life."
     */
    fun Drain(amount: Int, target: EffectTarget): Effect = CompositeEffect(
        listOf(
            DealDamageEffect(amount, target),
            GainLifeEffect(amount, EffectTarget.Controller)
        )
    )

    // =========================================================================
    // Chain Copy Effects
    // =========================================================================

    /**
     * Destroy target permanent, then its controller may copy this spell.
     * Used for Chain of Acid.
     */
    fun DestroyAndChainCopy(
        target: EffectTarget,
        targetFilter: com.wingedsheep.sdk.scripting.filters.unified.TargetFilter,
        spellName: String
    ): Effect = com.wingedsheep.sdk.scripting.effects.ChainCopyEffect(
        action = com.wingedsheep.sdk.scripting.effects.ChainAction.Destroy,
        target = target,
        targetFilter = targetFilter,
        copyRecipient = com.wingedsheep.sdk.scripting.effects.CopyRecipient.TARGET_CONTROLLER,
        copyCost = com.wingedsheep.sdk.scripting.effects.ChainCopyCost.NoCost,
        copyTargetRequirement = com.wingedsheep.sdk.scripting.targets.TargetObject(filter = targetFilter),
        spellName = spellName
    )

    /**
     * Bounce target permanent, then its controller may sacrifice a land to copy.
     * Used for Chain of Vapor.
     */
    fun BounceAndChainCopy(
        target: EffectTarget,
        targetFilter: com.wingedsheep.sdk.scripting.filters.unified.TargetFilter,
        spellName: String
    ): Effect = com.wingedsheep.sdk.scripting.effects.ChainCopyEffect(
        action = com.wingedsheep.sdk.scripting.effects.ChainAction.BounceToHand,
        target = target,
        targetFilter = targetFilter,
        copyRecipient = com.wingedsheep.sdk.scripting.effects.CopyRecipient.TARGET_CONTROLLER,
        copyCost = com.wingedsheep.sdk.scripting.effects.ChainCopyCost.SacrificeALand,
        copyTargetRequirement = com.wingedsheep.sdk.scripting.targets.TargetObject(filter = targetFilter),
        spellName = spellName
    )

    /**
     * Deal damage to any target, then that player may discard a card to copy.
     * Used for Chain of Plasma.
     */
    fun DamageAndChainCopy(
        amount: Int,
        target: EffectTarget,
        spellName: String
    ): Effect = com.wingedsheep.sdk.scripting.effects.ChainCopyEffect(
        action = com.wingedsheep.sdk.scripting.effects.ChainAction.DealDamage(amount),
        target = target,
        copyRecipient = com.wingedsheep.sdk.scripting.effects.CopyRecipient.AFFECTED_PLAYER,
        copyCost = com.wingedsheep.sdk.scripting.effects.ChainCopyCost.DiscardACard,
        copyTargetRequirement = com.wingedsheep.sdk.scripting.targets.AnyTarget(),
        spellName = spellName
    )

    /**
     * Target player discards cards, then may copy and choose a new target.
     * Used for Chain of Smog.
     */
    fun DiscardAndChainCopy(
        count: Int,
        target: EffectTarget,
        spellName: String
    ): Effect = com.wingedsheep.sdk.scripting.effects.ChainCopyEffect(
        action = com.wingedsheep.sdk.scripting.effects.ChainAction.Discard(count),
        target = target,
        copyRecipient = com.wingedsheep.sdk.scripting.effects.CopyRecipient.TARGET_PLAYER,
        copyCost = com.wingedsheep.sdk.scripting.effects.ChainCopyCost.NoCost,
        copyTargetRequirement = com.wingedsheep.sdk.scripting.targets.TargetPlayer(),
        spellName = spellName
    )

    /**
     * Prevent all damage target creature would deal, then its controller may sacrifice a land to copy.
     * Used for Chain of Silence.
     */
    fun PreventDamageAndChainCopy(
        target: EffectTarget,
        targetFilter: com.wingedsheep.sdk.scripting.filters.unified.TargetFilter,
        spellName: String
    ): Effect = com.wingedsheep.sdk.scripting.effects.ChainCopyEffect(
        action = com.wingedsheep.sdk.scripting.effects.ChainAction.PreventAllDamageDealt,
        target = target,
        targetFilter = targetFilter,
        copyRecipient = com.wingedsheep.sdk.scripting.effects.CopyRecipient.TARGET_CONTROLLER,
        copyCost = com.wingedsheep.sdk.scripting.effects.ChainCopyCost.SacrificeALand,
        copyTargetRequirement = com.wingedsheep.sdk.scripting.targets.TargetObject(filter = targetFilter),
        spellName = spellName
    )

}
