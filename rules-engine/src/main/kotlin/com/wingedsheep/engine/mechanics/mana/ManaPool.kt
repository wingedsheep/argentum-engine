package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider
import com.wingedsheep.engine.state.components.player.RestrictedManaEntry
import kotlinx.serialization.Serializable

/**
 * Represents a player's mana pool.
 * Tracks available mana that can be spent on costs.
 */
/**
 * Context about the spell being cast or the ability being activated, used to evaluate
 * mana spending restrictions.
 *
 * Spell-cast fields (isInstantOrSorcery, isKicked, isCreature, manaValue, hasXInCost,
 * subtypes, isFromExile, [cardTypes]) describe a spell being cast. When [isAbilityActivation]
 * is true, the context instead describes an activated ability being paid for, and
 * [abilitySourceCardTypes] (plus [subtypes] of the source) carry the source's type
 * information for restrictions that check it.
 */
data class SpellPaymentContext(
    val isInstantOrSorcery: Boolean = false,
    val isKicked: Boolean = false,
    val isCreature: Boolean = false,
    val manaValue: Int = 0,
    val hasXInCost: Boolean = false,
    val subtypes: Set<String> = emptySet(),
    val isFromExile: Boolean = false,
    /** True when the spell being cast is legendary (has the Legendary supertype). */
    val isLegendary: Boolean = false,
    /** Card types of the spell being cast (empty for non-spell contexts). */
    val cardTypes: Set<com.wingedsheep.sdk.core.CardType> = emptySet(),
    val isAbilityActivation: Boolean = false,
    /** Card types of the source whose ability is being activated (empty for spell-cast contexts). */
    val abilitySourceCardTypes: Set<com.wingedsheep.sdk.core.CardType> = emptySet(),
    /**
     * True when the spell is being cast from the caster's hand. Defaults to `true` because most
     * spell casts originate from hand and the standard [CastSpellEnumerator] path is hand-only;
     * cast-from-non-hand paths (top of library, exile, graveyard, command zone) must set this to
     * `false` so restrictions like [ManaRestriction.CastFromNonHandOnly] recognize the cast.
     *
     * The field is irrelevant for ability activations because every restriction that reads it
     * also requires `!isAbilityActivation`.
     */
    val isFromHand: Boolean = true,
    /**
     * True when the payment is for the turn-face-up special action (CR 707.9 / disguise's
     * turn-up). Lets restrictions like [ManaRestriction.TurnPermanentsFaceUpOnly] recognize
     * "spend this mana only to turn permanents face up" (Overgrown Zealot, Creeping Peeper).
     */
    val isTurnFaceUpAction: Boolean = false,
    /**
     * True when the payment is for the unlock-a-door special action (CR 709.5e). Lets
     * [ManaRestriction.UnlockDoorOnly] recognize "spend this mana only to ... unlock a door"
     * (Creeping Peeper).
     */
    val isUnlockDoorAction: Boolean = false,
)

/**
 * Check whether a mana restriction is satisfied by the spell being cast or ability being activated.
 */
fun ManaRestriction.isSatisfiedBy(context: SpellPaymentContext): Boolean = when (this) {
    is ManaRestriction.AnySpend -> true
    is ManaRestriction.InstantOrSorceryOnly -> !context.isAbilityActivation && context.isInstantOrSorcery
    is ManaRestriction.KickedSpellsOnly -> !context.isAbilityActivation && context.isKicked
    is ManaRestriction.CreatureMV4OrXCost ->
        !context.isAbilityActivation && context.isCreature && (context.manaValue >= 4 || context.hasXInCost)
    is ManaRestriction.SpellsMV4OrGreater -> !context.isAbilityActivation && context.manaValue >= 4
    is ManaRestriction.CreatureSpellsOnly -> !context.isAbilityActivation && context.isCreature
    is ManaRestriction.LegendarySpellsOnly -> !context.isAbilityActivation && context.isLegendary
    is ManaRestriction.SubtypeSpellsOrAbilitiesOnly ->
        (!creatureOnly || (!context.isAbilityActivation && context.isCreature)) &&
            context.subtypes.any { it.equals(subtype, ignoreCase = true) }
    is ManaRestriction.CastFromExileOnly -> !context.isAbilityActivation && context.isFromExile
    is ManaRestriction.CastFromNonHandOnly -> !context.isAbilityActivation && !context.isFromHand
    is ManaRestriction.TurnPermanentsFaceUpOnly -> context.isTurnFaceUpAction
    is ManaRestriction.UnlockDoorOnly -> context.isUnlockDoorAction
    is ManaRestriction.AbilityActivationOnly -> context.isAbilityActivation
    is ManaRestriction.AnyOf -> restrictions.any { it.isSatisfiedBy(context) }
    is ManaRestriction.SubtypeSpellsOnly ->
        !context.isAbilityActivation &&
            subtypes.any { sub -> context.subtypes.any { it.equals(sub, ignoreCase = true) } }
    is ManaRestriction.CardTypeSpellsOrAbilitiesOnly ->
        if (context.isAbilityActivation) allowAbilities && (cardType in context.abilitySourceCardTypes) != negated
        else allowSpells && (cardType in context.cardTypes) != negated
}

@Serializable
data class ManaPool(
    val white: Int = 0,
    val blue: Int = 0,
    val black: Int = 0,
    val red: Int = 0,
    val green: Int = 0,
    val colorless: Int = 0,
    val restrictedMana: List<RestrictedManaEntry> = emptyList(),
    /**
     * Count of mana in the pool added by a permanent with the Treasure
     * subtype. See [com.wingedsheep.engine.state.components.player.ManaPoolComponent.treasureMana].
     */
    val treasureMana: Int = 0
) {
    /**
     * Get amount of mana for a specific color.
     */
    fun get(color: Color): Int = when (color) {
        Color.WHITE -> white
        Color.BLUE -> blue
        Color.BLACK -> black
        Color.RED -> red
        Color.GREEN -> green
    }

    /**
     * Total mana in the pool.
     */
    /**
     * Total unrestricted mana in the pool (does not include restricted mana).
     */
    val total: Int get() = white + blue + black + red + green + colorless

    /**
     * Total mana including restricted mana eligible for a given spell context.
     */
    fun totalForSpell(context: SpellPaymentContext): Int = total + getTotalEligibleRestricted(context)

    /**
     * Check if the pool is empty (including restricted mana).
     */
    fun isEmpty(): Boolean = total == 0 && restrictedMana.isEmpty()

    /**
     * Add mana of a specific color.
     */
    fun add(color: Color, amount: Int = 1): ManaPool = when (color) {
        Color.WHITE -> copy(white = white + amount)
        Color.BLUE -> copy(blue = blue + amount)
        Color.BLACK -> copy(black = black + amount)
        Color.RED -> copy(red = red + amount)
        Color.GREEN -> copy(green = green + amount)
    }

    /**
     * Add colorless mana.
     */
    fun addColorless(amount: Int = 1): ManaPool = copy(colorless = colorless + amount)

    /**
     * Add restricted mana to the pool.
     */
    fun addRestricted(
        color: Color?,
        amount: Int,
        restriction: ManaRestriction,
        riders: Set<ManaSpellRider> = emptySet()
    ): ManaPool {
        val entries = (1..amount).map { RestrictedManaEntry(color, restriction, riders) }
        return copy(restrictedMana = restrictedMana + entries)
    }

    /**
     * Spend one unit of restricted mana matching the given color and whose restriction is satisfied by the spell context.
     * Returns null if no matching restricted mana is available.
     */
    fun spendRestricted(color: Color?, context: SpellPaymentContext): ManaPool? {
        val index = restrictedMana.indexOfFirst { it.color == color && it.restriction.isSatisfiedBy(context) }
        if (index == -1) return null
        return copy(restrictedMana = restrictedMana.toMutableList().apply { removeAt(index) })
    }

    /**
     * Count eligible restricted mana of a given color for a spell.
     */
    fun getEligibleRestrictedCount(color: Color?, context: SpellPaymentContext): Int =
        restrictedMana.count { it.color == color && it.restriction.isSatisfiedBy(context) }

    /**
     * Count all eligible restricted mana (any color) for a spell.
     */
    fun getTotalEligibleRestricted(context: SpellPaymentContext): Int =
        restrictedMana.count { it.restriction.isSatisfiedBy(context) }

    /**
     * Remove mana of a specific color.
     */
    fun spend(color: Color, amount: Int = 1): ManaPool? {
        val current = get(color)
        if (current < amount) return null
        return when (color) {
            Color.WHITE -> copy(white = white - amount)
            Color.BLUE -> copy(blue = blue - amount)
            Color.BLACK -> copy(black = black - amount)
            Color.RED -> copy(red = red - amount)
            Color.GREEN -> copy(green = green - amount)
        }
    }

    /**
     * Remove colorless mana.
     */
    fun spendColorless(amount: Int = 1): ManaPool? {
        if (colorless < amount) return null
        return copy(colorless = colorless - amount)
    }

    /**
     * The ordered units of *unrestricted* floating mana this pool would spend to cover up to
     * [xAmount] of an {X} cost: colorless first (only when X is not color-restricted), then the
     * allowed colors (all colors when [xManaRestriction] is empty). Each element is the color of
     * one unit, or `null` for a colorless unit; the list length is how much of X this pool covers.
     * Restricted/rider mana is not considered.
     *
     * Shared by `CastPaymentProcessor.autoPay` (spends each unit and tallies per-color X spend)
     * and `ActivateAbilityHandler.autoTapForManaCost` (uses only the count, to reduce how much X
     * it must tap sources for) so both apply the exact same coverage rule.
     */
    fun xCoveragePlan(xAmount: Int, xManaRestriction: Set<Color>): List<Color?> {
        if (xAmount <= 0) return emptyList()
        val allowed = if (xManaRestriction.isEmpty()) Color.entries.toSet() else xManaRestriction
        val plan = ArrayList<Color?>(xAmount)
        var remaining = xAmount
        // Colorless pays generic X, but never a color-restricted X.
        if (xManaRestriction.isEmpty()) {
            val take = minOf(remaining, colorless)
            repeat(take) { plan.add(null) }
            remaining -= take
        }
        for (color in Color.entries) {
            if (remaining <= 0) break
            if (color !in allowed) continue
            val take = minOf(remaining, get(color))
            repeat(take) { plan.add(color) }
            remaining -= take
        }
        return plan
    }

    /**
     * How much of an {X} amount of [xAmount] this pool can cover: eligible restricted mana first
     * (entries whose restriction is satisfied by [spellContext] and — for a color-restricted X —
     * whose color is allowed), then unrestricted mana per [xCoveragePlan]. Mirrors the spending
     * order of `CastPaymentProcessor.autoPay`, so validation counting with this method never
     * accepts an X the payment path can't actually cover from the pool.
     */
    fun xCoverage(xAmount: Int, xManaRestriction: Set<Color>, spellContext: SpellPaymentContext?): Int {
        if (xAmount <= 0) return 0
        val eligibleRestricted = if (spellContext == null) 0 else restrictedMana.count { entry ->
            entry.restriction.isSatisfiedBy(spellContext) &&
                // A color-restricted X can't be paid with off-color or colorless restricted mana.
                (xManaRestriction.isEmpty() || (entry.color != null && entry.color in xManaRestriction))
        }
        val fromRestricted = minOf(xAmount, eligibleRestricted)
        val fromUnrestricted = xCoveragePlan(xAmount - fromRestricted, xManaRestriction).size
        return fromRestricted + fromUnrestricted
    }

    /**
     * Check if this pool can pay a mana cost.
     * When [spellContext] is provided, eligible restricted mana is considered (spent first).
     */
    fun canPay(cost: ManaCost, spellContext: SpellPaymentContext? = null): Boolean {
        var remaining = this

        // First, pay colored costs — try restricted mana first, then unrestricted
        for (symbol in cost.symbols) {
            when (symbol) {
                is ManaSymbol.Colored -> {
                    remaining = remaining.trySpendColored(symbol.color, spellContext) ?: return false
                }
                is ManaSymbol.Colorless -> {
                    remaining = remaining.trySpendColorless(spellContext) ?: return false
                }
                is ManaSymbol.Generic -> {
                    // Will handle in second pass
                }
                is ManaSymbol.X -> {
                    // X is 0 unless specified otherwise
                }
                is ManaSymbol.Hybrid -> {
                    val spent = remaining.trySpendColored(symbol.color1, spellContext)
                        ?: remaining.trySpendColored(symbol.color2, spellContext)
                        ?: return false
                    remaining = spent
                }
                is ManaSymbol.Phyrexian -> {
                    remaining = remaining.trySpendColored(symbol.color, spellContext) ?: return false
                }
                is ManaSymbol.MonocolorHybrid -> {
                    // Resolved after strict pips below so a strict pip of the same color claims
                    // its mana first.
                }
            }
        }

        // Monocolored hybrids ({2/B}): prefer one mana of the color (cheaper, fewer mana),
        // otherwise treat as the generic amount. Strict pips are already paid above, so spending
        // the color here can't rob them, and any other hybrid can still fall back to generic.
        var monoHybridGeneric = 0
        for (symbol in cost.symbols.filterIsInstance<ManaSymbol.MonocolorHybrid>()) {
            val spent = remaining.trySpendColored(symbol.color, spellContext)
            if (spent != null) remaining = spent else monoHybridGeneric += symbol.generic
        }

        // Then, pay generic costs with any remaining mana (restricted first, then unrestricted)
        val genericAmount = cost.genericAmount + monoHybridGeneric
        val availableForGeneric = if (spellContext != null) {
            remaining.total + remaining.getTotalEligibleRestricted(spellContext)
        } else {
            remaining.total
        }
        if (availableForGeneric < genericAmount) return false

        return true
    }

    /**
     * Try to spend one colored mana, preferring eligible restricted mana first.
     */
    private fun trySpendColored(color: Color, spellContext: SpellPaymentContext?): ManaPool? {
        if (spellContext != null) {
            val fromRestricted = spendRestricted(color, spellContext)
            if (fromRestricted != null) return fromRestricted
        }
        return spend(color)
    }

    /**
     * Try to spend one colorless mana, preferring eligible restricted mana first.
     */
    private fun trySpendColorless(spellContext: SpellPaymentContext?): ManaPool? {
        if (spellContext != null) {
            val fromRestricted = spendRestricted(null, spellContext)
            if (fromRestricted != null) return fromRestricted
        }
        return spendColorless()
    }

    /**
     * Pay a mana cost, returning the new pool or null if can't pay.
     * When [spellContext] is provided, eligible restricted mana is spent first.
     */
    fun pay(cost: ManaCost, spellContext: SpellPaymentContext? = null): ManaPool? {
        if (!canPay(cost, spellContext)) return null

        var remaining = this

        // Pay colored costs — restricted first, then unrestricted
        for (symbol in cost.symbols) {
            when (symbol) {
                is ManaSymbol.Colored -> {
                    remaining = remaining.trySpendColored(symbol.color, spellContext)!!
                }
                is ManaSymbol.Colorless -> {
                    remaining = remaining.trySpendColorless(spellContext)!!
                }
                is ManaSymbol.Generic -> {
                    // Will handle separately
                }
                is ManaSymbol.X -> {
                    // Handled by caller
                }
                is ManaSymbol.Hybrid -> {
                    remaining = remaining.trySpendColored(symbol.color1, spellContext)
                        ?: remaining.trySpendColored(symbol.color2, spellContext)!!
                }
                is ManaSymbol.Phyrexian -> {
                    remaining = remaining.trySpendColored(symbol.color, spellContext)!!
                }
                is ManaSymbol.MonocolorHybrid -> {
                    // Resolved after strict pips below (mirrors canPay).
                }
            }
        }

        // Monocolored hybrids ({2/B}): prefer one mana of the color, else fall back to generic.
        // Mirrors canPay so a cost canPay accepts is always payable here.
        var monoHybridGeneric = 0
        for (symbol in cost.symbols.filterIsInstance<ManaSymbol.MonocolorHybrid>()) {
            val spent = remaining.trySpendColored(symbol.color, spellContext)
            if (spent != null) remaining = spent else monoHybridGeneric += symbol.generic
        }

        // Pay generic costs - spend eligible restricted first, then colorless, then colored
        var genericRemaining = cost.genericAmount + monoHybridGeneric

        // Spend eligible restricted mana for generic costs (any color)
        if (spellContext != null) {
            for (entry in remaining.restrictedMana.toList()) {
                if (genericRemaining <= 0) break
                if (entry.restriction.isSatisfiedBy(spellContext)) {
                    remaining = remaining.spendRestricted(entry.color, spellContext)!!
                    genericRemaining--
                }
            }
        }

        while (genericRemaining > 0 && remaining.colorless > 0) {
            remaining = remaining.spendColorless()!!
            genericRemaining--
        }

        for (color in Color.entries) {
            while (genericRemaining > 0 && remaining.get(color) > 0) {
                remaining = remaining.spend(color)!!
                genericRemaining--
            }
        }

        return remaining
    }

    /**
     * Result of a partial mana payment.
     */
    data class PartialPaymentResult(
        val newPool: ManaPool,
        val remainingCost: ManaCost,
        val manaSpent: ManaPool
    )

    /**
     * Pay as much of a mana cost as possible from this pool.
     * Returns the new pool, the remaining unpaid cost, and the mana that was spent.
     * This is used for AutoPay to use floating mana before tapping lands.
     * When [spellContext] is provided, eligible restricted mana is spent first.
     */
    fun payPartial(cost: ManaCost, spellContext: SpellPaymentContext? = null): PartialPaymentResult {
        var remaining = this
        val unpaidSymbols = mutableListOf<ManaSymbol>()

        // Track mana spent
        var whiteSpent = 0
        var blueSpent = 0
        var blackSpent = 0
        var redSpent = 0
        var greenSpent = 0
        var colorlessSpent = 0

        fun trackColorSpent(color: Color) {
            when (color) {
                Color.WHITE -> whiteSpent++
                Color.BLUE -> blueSpent++
                Color.BLACK -> blackSpent++
                Color.RED -> redSpent++
                Color.GREEN -> greenSpent++
            }
        }

        // Try to pay colored costs first — restricted mana preferred
        for (symbol in cost.symbols) {
            when (symbol) {
                is ManaSymbol.Colored -> {
                    val spent = remaining.trySpendColored(symbol.color, spellContext)
                    if (spent != null) {
                        remaining = spent
                        trackColorSpent(symbol.color)
                    } else {
                        unpaidSymbols.add(symbol)
                    }
                }
                is ManaSymbol.Colorless -> {
                    val spent = remaining.trySpendColorless(spellContext)
                    if (spent != null) {
                        remaining = spent
                        colorlessSpent++
                    } else {
                        unpaidSymbols.add(symbol)
                    }
                }
                is ManaSymbol.Hybrid -> {
                    val beforeRemaining = remaining
                    val spent = remaining.trySpendColored(symbol.color1, spellContext)
                        ?: remaining.trySpendColored(symbol.color2, spellContext)
                    if (spent != null) {
                        // Determine which color was used by checking what changed
                        val color1Before = beforeRemaining.get(symbol.color1) +
                            beforeRemaining.restrictedMana.count { it.color == symbol.color1 }
                        val color1After = spent.get(symbol.color1) +
                            spent.restrictedMana.count { it.color == symbol.color1 }
                        if (color1Before > color1After) {
                            trackColorSpent(symbol.color1)
                        } else {
                            trackColorSpent(symbol.color2)
                        }
                        remaining = spent
                    } else {
                        unpaidSymbols.add(symbol)
                    }
                }
                is ManaSymbol.Phyrexian -> {
                    val spent = remaining.trySpendColored(symbol.color, spellContext)
                    if (spent != null) {
                        remaining = spent
                        trackColorSpent(symbol.color)
                    } else {
                        unpaidSymbols.add(symbol)
                    }
                }
                is ManaSymbol.MonocolorHybrid -> {
                    // Spend floating mana of the color if available; otherwise leave the hybrid
                    // unpaid (as-is) so the land solver keeps the color-vs-generic choice rather
                    // than locking it to the generic amount.
                    val spent = remaining.trySpendColored(symbol.color, spellContext)
                    if (spent != null) {
                        remaining = spent
                        trackColorSpent(symbol.color)
                    } else {
                        unpaidSymbols.add(symbol)
                    }
                }
                is ManaSymbol.Generic -> {
                    unpaidSymbols.add(symbol)
                }
                is ManaSymbol.X -> {
                    unpaidSymbols.add(symbol)
                }
            }
        }

        // Now pay generic costs with remaining mana
        var genericRemaining = unpaidSymbols.filterIsInstance<ManaSymbol.Generic>().sumOf { it.amount }
        unpaidSymbols.removeAll { it is ManaSymbol.Generic }

        // Spend eligible restricted mana for generic costs first
        if (spellContext != null) {
            for (entry in remaining.restrictedMana.toList()) {
                if (genericRemaining <= 0) break
                if (entry.restriction.isSatisfiedBy(spellContext)) {
                    val spent = remaining.spendRestricted(entry.color, spellContext)
                    if (spent != null) {
                        remaining = spent
                        if (entry.color != null) trackColorSpent(entry.color) else colorlessSpent++
                        genericRemaining--
                    }
                }
            }
        }

        // Spend colorless first for generic
        while (genericRemaining > 0 && remaining.colorless > 0) {
            remaining = remaining.spendColorless()!!
            colorlessSpent++
            genericRemaining--
        }

        // Spend colored mana for remaining generic
        for (color in Color.entries) {
            while (genericRemaining > 0 && remaining.get(color) > 0) {
                remaining = remaining.spend(color)!!
                trackColorSpent(color)
                genericRemaining--
            }
        }

        // Add remaining generic back to unpaid
        if (genericRemaining > 0) {
            unpaidSymbols.add(ManaSymbol.Generic(genericRemaining))
        }

        return PartialPaymentResult(
            newPool = remaining,
            remainingCost = ManaCost(unpaidSymbols),
            manaSpent = ManaPool(
                white = whiteSpent,
                blue = blueSpent,
                black = blackSpent,
                red = redSpent,
                green = greenSpent,
                colorless = colorlessSpent
            )
        )
    }

    /**
     * Empty the mana pool (at end of phases).
     */
    fun empty(): ManaPool = EMPTY

    companion object {
        val EMPTY = ManaPool()
    }
}
