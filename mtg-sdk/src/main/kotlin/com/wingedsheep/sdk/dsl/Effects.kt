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
import com.wingedsheep.sdk.scripting.effects.SetBasePowerEffect
import com.wingedsheep.sdk.scripting.effects.TakeFromLinkedExileEffect
import com.wingedsheep.sdk.scripting.effects.ChooseColorAndGrantProtectionToGroupEffect
import com.wingedsheep.sdk.scripting.effects.ChooseColorAndGrantProtectionToTargetEffect
import com.wingedsheep.sdk.scripting.effects.ChooseCreatureTypeGainControlEffect
import com.wingedsheep.sdk.scripting.effects.CantBlockGroupEffect
import com.wingedsheep.sdk.scripting.effects.CantCastSpellsEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.EachOpponentDiscardsEffect
import com.wingedsheep.sdk.scripting.effects.EachPlayerReturnsPermanentToHandEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.GrantPlayWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.FightEffect
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.effects.ExchangeControlEffect
import com.wingedsheep.sdk.scripting.effects.GainControlByMostOfSubtypeEffect
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.effects.RemoveKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.SetLifeTotalEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ExileGroupAndLinkEffect
import com.wingedsheep.sdk.scripting.effects.CreatePermanentGlobalTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.ReturnLinkedExileEffect
import com.wingedsheep.sdk.scripting.effects.ReturnOneFromLinkedExileEffect
import com.wingedsheep.sdk.scripting.effects.ReturnSelfToBattlefieldAttachedEffect
import com.wingedsheep.sdk.scripting.effects.DrawUpToEffect
import com.wingedsheep.sdk.scripting.effects.ReadTheRunesEffect
import com.wingedsheep.sdk.scripting.effects.RemoveFromCombatEffect
import com.wingedsheep.sdk.scripting.effects.RepeatCondition
import com.wingedsheep.sdk.scripting.effects.RepeatWhileEffect
import com.wingedsheep.sdk.scripting.effects.ReplaceNextDrawWithEffect
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
import com.wingedsheep.sdk.scripting.effects.ReselectTargetRandomlyEffect
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect
import com.wingedsheep.sdk.scripting.effects.SetCreatureSubtypesEffect
import com.wingedsheep.sdk.scripting.effects.LoseGameEffect
import com.wingedsheep.sdk.scripting.effects.SkipNextTurnEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.TargetRequirement

/**
 * Facade object providing convenient factory methods for creating atomic Effects.
 *
 * For composite effect patterns (search, scry, mill, discard, etc.),
 * use [EffectPatterns] directly.
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
     * Gain life with a dynamic amount. Default target is the controller.
     */
    fun GainLife(amount: DynamicAmount, target: EffectTarget = EffectTarget.Controller): Effect =
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
     * Target player loses the game.
     */
    fun LoseGame(target: EffectTarget = EffectTarget.Controller, message: String? = null): Effect =
        LoseGameEffect(target, message)

    /**
     * Set a player's life total to a fixed amount.
     * Per Rule 118.5, the player gains or loses the necessary amount of life.
     */
    fun SetLifeTotal(amount: Int, target: EffectTarget = EffectTarget.Controller): Effect =
        SetLifeTotalEffect(amount, target)

    /**
     * Set a player's life total to a dynamic amount.
     */
    fun SetLifeTotal(amount: DynamicAmount, target: EffectTarget = EffectTarget.Controller): Effect =
        SetLifeTotalEffect(amount, target)

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
     * Draw up to N cards. The player chooses how many (0 to maxCards).
     */
    fun DrawUpTo(maxCards: Int, target: EffectTarget = EffectTarget.Controller): Effect =
        DrawUpToEffect(maxCards, target)

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
     */
    fun ReplaceNextDraw(effect: Effect): Effect = ReplaceNextDrawWithEffect(effect)

    // =========================================================================
    // Zone Movement Effects
    // =========================================================================

    /**
     * Destroy a target.
     */
    fun Destroy(target: EffectTarget): Effect =
        MoveToZoneEffect(target, Zone.GRAVEYARD, byDestruction = true)

    /**
     * Destroy all permanents matching a filter using the pipeline.
     * Correctly handles indestructible and regeneration via MoveType.Destroy.
     *
     * @param filter Which permanents to destroy
     * @param noRegenerate If true, destroyed permanents can't be regenerated
     * @param storeDestroyedAs If set, stores actually-destroyed IDs for follow-up effects
     *   (count available as DynamicAmount.VariableReference("{key}_count"))
     */
    fun DestroyAll(
        filter: GameObjectFilter,
        noRegenerate: Boolean = false,
        storeDestroyedAs: String? = null
    ): Effect = EffectPatterns.destroyAllPipeline(filter, noRegenerate, storeDestroyedAs)

    /**
     * Exile a target.
     */
    fun Exile(target: EffectTarget): Effect =
        MoveToZoneEffect(target, Zone.EXILE)

    /**
     * Exile all permanents matching a filter that the controller controls and link
     * them to the source permanent. Used for Day of the Dragons-style effects.
     * The count is available as DynamicAmount.VariableReference("{storeAs}_count").
     */
    fun ExileGroupAndLink(filter: GroupFilter, storeAs: String = "linked_exile"): Effect =
        ExileGroupAndLinkEffect(filter, storeAs)

    /**
     * Return all cards linked to the source permanent (via LinkedExileComponent)
     * to the battlefield under the controller's control.
     */
    fun ReturnLinkedExile(): Effect = ReturnLinkedExileEffect()

    /**
     * Return all cards linked to the source permanent (via LinkedExileComponent)
     * to the battlefield under their owners' control.
     */
    fun ReturnLinkedExileUnderOwnersControl(): Effect = ReturnLinkedExileEffect(underOwnersControl = true)

    /**
     * Return one card from the source's linked exile to the battlefield.
     * The active player chooses one of their owned exiled cards.
     */
    fun ReturnOneFromLinkedExile(): Effect = ReturnOneFromLinkedExileEffect

    /**
     * Create a global triggered ability that lasts permanently.
     * Used for recurring triggers from non-permanent sources.
     */
    fun CreatePermanentGlobalTriggeredAbility(ability: TriggeredAbility): Effect =
        CreatePermanentGlobalTriggeredAbilityEffect(ability)

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
     * Grant "may play from exile" permission to all cards in a named collection.
     * Does NOT waive mana cost — pair with [GrantPlayWithoutPayingCost] for free play.
     */
    fun GrantMayPlayFromExile(from: String): Effect = GrantMayPlayFromExileEffect(from)

    /**
     * Grant "play without paying mana cost" permission to all cards in a named collection.
     * Card must still be in a playable zone (hand, or exile with GrantMayPlayFromExile).
     */
    fun GrantPlayWithoutPayingCost(from: String): Effect = GrantPlayWithoutPayingCostEffect(from)

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

    /**
     * Return all permanents matching a filter to their owners' hands.
     */
    fun ReturnAllToHand(filter: GroupFilter): Effect =
        EffectPatterns.returnAllToHand(filter)

    /**
     * Take the top card from the source's linked exile pile and put it into your hand.
     * Used by Parallel Thoughts and similar cards.
     */
    fun TakeFromLinkedExile(): Effect = TakeFromLinkedExileEffect

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

    /**
     * Set a creature's base power to a dynamic value.
     * "Change this creature's base power to target creature's power."
     */
    fun SetBasePower(
        target: EffectTarget = EffectTarget.Self,
        power: DynamicAmount,
        duration: Duration = Duration.Permanent
    ): Effect = SetBasePowerEffect(target, power, duration)

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
     * @param controller Who receives the token. Null = spell controller.
     *   Use [EffectTarget.TargetController] to give tokens to the target's controller.
     */
    fun CreateToken(
        power: Int,
        toughness: Int,
        colors: Set<Color> = emptySet(),
        creatureTypes: Set<String>,
        keywords: Set<Keyword> = emptySet(),
        count: Int = 1,
        controller: EffectTarget? = null
    ): Effect = CreateTokenEffect(count, power, toughness, colors, creatureTypes, keywords, controller = controller)

    /**
     * Create Treasure tokens.
     */
    fun CreateTreasure(count: Int = 1): Effect =
        CreateTreasureTokensEffect(count)

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
     * Exchange control of two target creatures.
     */
    fun ExchangeControl(
        target1: EffectTarget = EffectTarget.ContextTarget(0),
        target2: EffectTarget = EffectTarget.ContextTarget(1)
    ): Effect = ExchangeControlEffect(target1, target2)

    /**
     * The player who controls the most creatures of the given subtype gains control of the target.
     */
    fun GainControlByMostOfSubtype(subtype: Subtype, target: EffectTarget = EffectTarget.Self): Effect =
        GainControlByMostOfSubtypeEffect(subtype, target)

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
     */
    fun ChangeSpellTarget(targetMustBeSource: Boolean = false): Effect =
        ChangeSpellTargetEffect(targetMustBeSource)

    /**
     * Change the target of target spell or ability with a single target.
     */
    fun ChangeTarget(): Effect =
        ChangeTargetEffect

    /**
     * Reselect the target of the triggering spell or ability at random.
     */
    fun ReselectTargetRandomly(): Effect =
        ReselectTargetRandomlyEffect

    /**
     * Copy target instant or sorcery spell. You may choose new targets for the copy.
     */
    fun CopyTargetSpell(target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        CopyTargetSpellEffect(target)

    // =========================================================================
    // Sacrifice Effects
    // =========================================================================

    /**
     * Force a player to sacrifice permanents matching a filter.
     */
    fun Sacrifice(filter: GameObjectFilter, count: Int = 1, target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent)): Effect =
        ForceSacrificeEffect(filter, count, target)

    /**
     * Sacrifice a specific permanent identified by target.
     * Used in delayed triggers where the exact permanent was determined at resolution time.
     */
    fun SacrificeTarget(target: EffectTarget): Effect =
        SacrificeTargetEffect(target)

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

    // =========================================================================
    // Group Effects (atomic effect classes)
    // =========================================================================

    /**
     * All creatures matching a filter can't block this turn.
     */
    fun CantBlockGroup(filter: GroupFilter, duration: Duration = Duration.EndOfTurn): Effect =
        CantBlockGroupEffect(filter, duration)

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

    /**
     * Target player skips their next turn.
     * Used for cards like Lethal Vapors.
     */
    fun SkipNextTurn(target: EffectTarget = EffectTarget.Controller): Effect =
        SkipNextTurnEffect(target)

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

    /**
     * Grant a creature "can't attack or block unless its controller pays {X} for each [creature type]
     * on the battlefield" until end of turn.
     */
    fun GrantAttackBlockTaxPerCreatureType(
        target: EffectTarget = EffectTarget.ContextTarget(0),
        creatureType: String,
        manaCostPer: String,
        duration: Duration = Duration.EndOfTurn
    ): Effect = com.wingedsheep.sdk.scripting.effects.GrantAttackBlockTaxPerCreatureTypeEffect(
        target, creatureType, manaCostPer, duration
    )

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
     */
    fun ChooseOption(
        optionType: OptionType,
        storeAs: String = "chosenOption",
        prompt: String? = null,
        excludedOptions: List<String> = emptyList()
    ): Effect = ChooseOptionEffect(optionType, storeAs, prompt, excludedOptions)

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
