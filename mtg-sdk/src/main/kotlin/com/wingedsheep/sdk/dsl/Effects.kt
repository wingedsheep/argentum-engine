package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.*

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

    // =========================================================================
    // Card Drawing Effects
    // =========================================================================

    /**
     * Draw cards. Default target is the controller.
     */
    fun DrawCards(count: Int, target: EffectTarget = EffectTarget.Controller): Effect =
        DrawCardsEffect(count, target)

    /**
     * Discard cards. Default target is target opponent.
     */
    fun Discard(count: Int, target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent)): Effect =
        DiscardCardsEffect(count, target)

    // =========================================================================
    // Destruction Effects
    // =========================================================================

    /**
     * Destroy a target.
     */
    fun Destroy(target: EffectTarget): Effect =
        DestroyEffect(target)

    /**
     * Exile a target.
     */
    fun Exile(target: EffectTarget): Effect =
        ExileEffect(target)

    /**
     * Return to hand.
     */
    fun ReturnToHand(target: EffectTarget): Effect =
        ReturnToHandEffect(target)

    // =========================================================================
    // Stat Modification Effects
    // =========================================================================

    /**
     * Modify power and toughness.
     */
    fun ModifyStats(power: Int, toughness: Int, target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        ModifyStatsEffect(power, toughness, target)

    /**
     * Grant a keyword until end of turn.
     */
    fun GrantKeyword(keyword: Keyword, target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
        GrantKeywordUntilEndOfTurnEffect(keyword, target)

    /**
     * Add counters.
     */
    fun AddCounters(counterType: String, count: Int, target: EffectTarget): Effect =
        AddCountersEffect(counterType, count, target)

    // =========================================================================
    // Mana Effects
    // =========================================================================

    /**
     * Add mana of a specific color.
     */
    fun AddMana(color: Color, amount: Int = 1): Effect =
        AddManaEffect(color, amount)

    /**
     * Add colorless mana.
     */
    fun AddColorlessMana(amount: Int): Effect =
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
     * Search library for cards.
     */
    fun SearchLibrary(
        filter: GameObjectFilter,
        count: Int = 1,
        destination: SearchDestination = SearchDestination.HAND,
        entersTapped: Boolean = false,
        shuffle: Boolean = true,
        reveal: Boolean = false
    ): Effect = SearchLibraryEffect(filter, count, destination, entersTapped, shuffle, reveal)

    /**
     * Scry N.
     */
    fun Scry(count: Int): Effect =
        ScryEffect(count)

    /**
     * Surveil N.
     */
    fun Surveil(count: Int): Effect =
        SurveilEffect(count)

    /**
     * Mill N cards.
     */
    fun Mill(count: Int, target: EffectTarget = EffectTarget.Controller): Effect =
        MillEffect(count, target)

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

    // =========================================================================
    // Counter Effects
    // =========================================================================

    /**
     * Counter target spell.
     */
    fun CounterSpell(): Effect =
        CounterSpellEffect

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

    // =========================================================================
    // Special Effects
    // =========================================================================

    /**
     * Separate permanents into piles (Liliana ultimate style).
     */
    fun SeparatePermanentsIntoPiles(target: EffectTarget = EffectTarget.PlayerRef(Player.TargetPlayer)): Effect =
        SeparatePermanentsIntoPilesEffect(target)

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
            DiscardCardsEffect(discard, EffectTarget.Controller)
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

}
