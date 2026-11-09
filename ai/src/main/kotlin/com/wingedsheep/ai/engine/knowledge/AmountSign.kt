package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.PayCountersEffect
import com.wingedsheep.sdk.scripting.effects.StoreNumberEffect
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Which way a [DynamicAmount] can point, read off its shape without a game state.
 *
 * The AI's static readers — "is this a pump?", "is this target slot friendly?" — used to read only
 * a [DynamicAmount.Fixed] and treat everything else as zero. That made every "+X/+X where X is …"
 * invisible: Accelerated Mutation (+X/+X, X the greatest mana value among your permanents) was not
 * a pump, so not a combat trick, so `HoldPolicy` never held it for combat and the AI fired it in its
 * main phase. A count, a mana value, an X — most dynamic amounts cannot go below zero, and saying
 * so is enough to call the modifier a gain.
 *
 * The `when` is exhaustive on purpose: a new [DynamicAmount] member must decide here rather than
 * fall through to a guess. Anything that can genuinely land on either side — a life total
 * mid-resolution, a pipeline variable — is [UNKNOWN], and callers treat that exactly as
 * they treated a non-fixed amount before.
 */
internal enum class AmountSign {
    /** Always exactly zero. */
    ZERO,

    /** Never negative, and not the constant zero: "+X" for any non-negative X. */
    GAIN,

    /** Never positive, and not the constant zero: "-X". */
    LOSS,

    /** Could be either. */
    UNKNOWN;

    fun flipped(): AmountSign = when (this) {
        GAIN -> LOSS
        LOSS -> GAIN
        ZERO, UNKNOWN -> this
    }

    companion object {
        fun of(amount: DynamicAmount): AmountSign = when (amount) {
            is DynamicAmount.Fixed -> ofInt(amount.amount)

            is DynamicAmount.Add -> sum(of(amount.left), of(amount.right))
            is DynamicAmount.Subtract -> sum(of(amount.left), of(amount.right).flipped())
            is DynamicAmount.Multiply -> when {
                amount.multiplier == 0 -> ZERO
                amount.multiplier > 0 -> of(amount.amount)
                else -> of(amount.amount).flipped()
            }
            is DynamicAmount.Divide -> {
                val n = of(amount.numerator)
                val d = of(amount.denominator)
                when {
                    n == ZERO -> ZERO
                    d == GAIN && (n == GAIN || n == LOSS) -> n
                    else -> UNKNOWN
                }
            }
            is DynamicAmount.Power -> GAIN
            is DynamicAmount.IfPositive -> when (of(amount.amount)) {
                ZERO, LOSS -> ZERO
                GAIN, UNKNOWN -> GAIN
            }
            // "The difference between its power and toughness" is max(a - b, b - a): whichever
            // operand is larger, one side is never negative (Doran, Besieged by Time).
            is DynamicAmount.Max -> if (isAbsoluteDifference(amount)) GAIN else max(of(amount.left), of(amount.right))
            is DynamicAmount.Min -> max(of(amount.left).flipped(), of(amount.right).flipped()).flipped()
            is DynamicAmount.Conditional -> either(of(amount.ifTrue), of(amount.ifFalse))
            is DynamicAmount.GreatestAmongPlayers -> of(amount.inner)
            is DynamicAmount.LeastAmongPlayers -> of(amount.inner)

            // A power or toughness can be negative, but CR 107.1b floors a calculation that yields
            // a negative result at zero: "+X/+X where X is its power", "double its power" never
            // take anything away. Doubling a negative power is the rule's exception (CR 701.10c
            // makes it -X/-0), a corner too rare to give up reading every doubler as a pump. Inside a sum the operand is still a raw value, so
            // "X minus its power" stays unknown through [sum].
            // The same floor holds for an aggregate: a total power, a greatest toughness.
            is DynamicAmount.EntityProperty, is DynamicAmount.AggregateBattlefield,
            is DynamicAmount.AggregateZone -> GAIN
            // A life total is read as-is, not floored by an effect: it can be at or below zero
            // before state-based actions.
            is DynamicAmount.YourLifeTotal, is DynamicAmount.LifeTotal -> UNKNOWN
            // Opaque at analysis time: a pipeline variable could be anything.
            is DynamicAmount.VariableReference -> UNKNOWN
            // A trigger-context value is a magnitude of the event — life gained, damage dealt, a
            // die result, counters placed — so "+X/+X where X is the life you gained" (Hurska
            // Sweet-Tooth) is a pump. See [contextSign].
            is DynamicAmount.ContextProperty -> contextSign(amount.key)
            // A cast-time capture holds whatever amount the card froze, which can be negative.
            is DynamicAmount.CapturedAtCast -> UNKNOWN

            // Counts, mana values, mana spent, X: none of them can go below zero.
            is DynamicAmount.Speed, is DynamicAmount.DungeonsCompleted, is DynamicAmount.PlayerCounterCount, is DynamicAmount.CardsCycledThisGame,
            is DynamicAmount.SpellsCastThisGame, is DynamicAmount.ExtraTurnsBegunThisGame,
            is DynamicAmount.StartingLifeTotal, is DynamicAmount.UnspentMana, is DynamicAmount.XValue,
            is DynamicAmount.LastKnownSourceCounters, is DynamicAmount.LastKnownDamageDealtToSource,
            is DynamicAmount.CastX, is DynamicAmount.CastChoice, is DynamicAmount.TotalManaSpent,
            is DynamicAmount.ManaSpentOnX, is DynamicAmount.ManaSpentFromSubtype, is DynamicAmount.ManaOfColorSpent,
            is DynamicAmount.SnowManaSpent, is DynamicAmount.DistinctColorsManaSpent, is DynamicAmount.DevotionTo, is DynamicAmount.CommanderColorIdentityCount,
            is DynamicAmount.StoredCardManaValue, is DynamicAmount.DistinctEntitiesInCollections,
            is DynamicAmount.DistinctCardTypesInCollections, is DynamicAmount.ManaValueSumOfCollection,
            is DynamicAmount.PlayerCount, is DynamicAmount.CountPlayersWith, is DynamicAmount.GraveyardRelativeCount,
            is DynamicAmount.Count, is DynamicAmount.UnlockedDoors, is DynamicAmount.StationCharge,
            is DynamicAmount.TurnTracking, is DynamicAmount.CombatDamageSourcesThisTurn,
            is DynamicAmount.PermanentsSacrificedThisTurn,
            is DynamicAmount.SpellsCastThisTurn, is DynamicAmount.SpellsCastLastTurn,
            is DynamicAmount.CraftedMaterialsTotalPower, is DynamicAmount.CraftedMaterialsTotalManaValue,
            is DynamicAmount.CraftedMaterialsColorCount, is DynamicAmount.CreaturesThatCrewedOrSaddledThisTurn,
            is DynamicAmount.CardTypeEnteredUnderControlThisTurn, is DynamicAmount.SubtypeEnteredUnderControlThisTurn,
            is DynamicAmount.CreaturesWithSubtypeDiedThisTurn, is DynamicAmount.PermanentsSacrificedThisWay,
            is DynamicAmount.CountersRemovedAsCost, is DynamicAmount.TotalPowerSacrificedThisWay,
            is DynamicAmount.LargestSharedCreatureTypeCount -> GAIN
            is DynamicAmount.PartySize -> GAIN
        }

        private fun isAbsoluteDifference(amount: DynamicAmount.Max): Boolean {
            val left = amount.left as? DynamicAmount.Subtract ?: return false
            val right = amount.right as? DynamicAmount.Subtract ?: return false
            return left.left == right.right && left.right == right.left
        }

        /**
         * Every [ContextPropertyKey] names an amount, count, mana value or die result read off the
         * triggering event or the resolving context; none can go below zero (a toughness or total
         * power is floored like [DynamicAmount.EntityProperty]). Exhaustive on purpose, so a new
         * key that could be negative has to say so here.
         */
        private fun contextSign(key: ContextPropertyKey): AmountSign = when (key) {
            ContextPropertyKey.TRIGGER_DIE_RESULT, ContextPropertyKey.TRIGGER_HIGHEST_DIE_RESULT,
            ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT, ContextPropertyKey.TRIGGER_RECIPIENT_TOUGHNESS,
            ContextPropertyKey.PREVENTED_DAMAGE_AMOUNT, ContextPropertyKey.TRIGGER_LIFE_GAINED,
            ContextPropertyKey.TRIGGER_LIFE_LOST, ContextPropertyKey.ADDITIONAL_COST_EXILED_COUNT,
            ContextPropertyKey.DISCARDED_AS_COST_TOTAL_MANA_VALUE, ContextPropertyKey.TARGET_COUNT,
            ContextPropertyKey.TARGETS_TOTAL_MANA_VALUE, ContextPropertyKey.LAST_KNOWN_PLUS_ONE_COUNTER_COUNT,
            ContextPropertyKey.TRIGGER_COUNTERS_PLACED_AMOUNT, ContextPropertyKey.TRIGGER_COUNTERS_REMOVED_AMOUNT,
            ContextPropertyKey.LAST_KNOWN_TOTAL_COUNTER_COUNT, ContextPropertyKey.LINKED_EXILE_CARD_COUNT,
            ContextPropertyKey.LINKED_EXILE_DISTINCT_CARD_TYPE_COUNT, ContextPropertyKey.MODES_CHOSEN_ON_TRIGGERING_SPELL,
            ContextPropertyKey.MANA_SPENT_ON_TRIGGERING_SPELL, ContextPropertyKey.COLORS_SPENT_ON_TRIGGERING_SPELL,
            ContextPropertyKey.TRIGGERING_SPELL_MANA_VALUE, ContextPropertyKey.X_VALUE_OF_TRIGGERING_SPELL,
            ContextPropertyKey.TRIGGER_SCRY_COUNT, ContextPropertyKey.TRIGGER_SCRY_BOTTOM_COUNT,
            ContextPropertyKey.TRIGGER_EXCESS_DAMAGE_AMOUNT, ContextPropertyKey.TRIGGER_DISCOVER_VALUE,
            ContextPropertyKey.DIED_BATCH_TOTAL_POWER, ContextPropertyKey.TRIGGER_DISCARD_COUNT,
            ContextPropertyKey.TRIGGER_MANA_PRODUCED -> GAIN
        }

        private fun ofInt(n: Int): AmountSign = when {
            n > 0 -> GAIN
            n < 0 -> LOSS
            else -> ZERO
        }

        private fun sum(a: AmountSign, b: AmountSign): AmountSign = when {
            a == ZERO -> b
            b == ZERO -> a
            a == b && a != UNKNOWN -> a
            else -> UNKNOWN
        }

        /** max(a, b): one non-negative side is enough to make the whole non-negative. */
        private fun max(a: AmountSign, b: AmountSign): AmountSign = when {
            a == GAIN || b == GAIN -> GAIN
            a == UNKNOWN || b == UNKNOWN -> UNKNOWN
            a == ZERO || b == ZERO -> ZERO
            else -> LOSS
        }

        /** One of two branches, not known which. */
        private fun either(a: AmountSign, b: AmountSign): AmountSign = when {
            a == b -> a
            a == ZERO && b != UNKNOWN -> b
            b == ZERO && a != UNKNOWN -> a
            else -> UNKNOWN
        }
    }
}

/**
 * How a "+P/+T" modifier pair reads as a whole: [AmountSign.GAIN] when neither side can lower a
 * stat and at least one raises it, [AmountSign.LOSS] for the mirror, and otherwise [AmountSign.ZERO]
 * or [AmountSign.UNKNOWN] — "+N/-N" and anything unreadable depend on the board.
 */
internal fun statsSign(power: DynamicAmount, toughness: DynamicAmount): AmountSign {
    val p = AmountSign.of(power)
    val t = AmountSign.of(toughness)
    return when {
        p == AmountSign.ZERO && t == AmountSign.ZERO -> AmountSign.ZERO
        (p == AmountSign.GAIN || p == AmountSign.ZERO) && (t == AmountSign.GAIN || t == AmountSign.ZERO) -> AmountSign.GAIN
        (p == AmountSign.LOSS || p == AmountSign.ZERO) && (t == AmountSign.LOSS || t == AmountSign.ZERO) -> AmountSign.LOSS
        else -> AmountSign.UNKNOWN
    }
}

/**
 * The numbers [effect]'s pipeline stores for a later step to read back, by variable name: a
 * [StoreNumberEffect]'s own amount, and for "you may pay any amount of {E}" ([PayCountersEffect],
 * Die Young, Galvanic Discharge) the payer's counters of that kind — what was paid lies between
 * zero and that count, so it carries the same sign. [AmountSign.of] alone reads a
 * [DynamicAmount.VariableReference] as [AmountSign.UNKNOWN]; [resolveStored] lets it see through.
 */
internal fun storedAmounts(effect: Effect): Map<String, DynamicAmount> = buildMap {
    for (leaf in EffectWalker.leaves(effect)) when (leaf) {
        is StoreNumberEffect -> put(leaf.name, leaf.amount)
        is PayCountersEffect -> put(leaf.storeAmountAs, DynamicAmount.PlayerCounterCount(leaf.counterType, leaf.player))
        else -> Unit
    }
}

/**
 * [amount] with every pipeline variable [stored] knows replaced by the amount it holds, through
 * the arithmetic around it: "-1/-1 for each {E} paid this way" is `-paid`, and it is a loss only
 * once `paid` is seen to be a count. Returns [amount] itself when nothing resolves.
 */
internal fun resolveStored(amount: DynamicAmount, stored: Map<String, DynamicAmount>): DynamicAmount {
    if (stored.isEmpty()) return amount
    fun r(a: DynamicAmount) = resolveStored(a, stored)
    return when (amount) {
        is DynamicAmount.VariableReference -> stored[amount.variableName] ?: amount
        is DynamicAmount.Multiply -> r(amount.amount).let { if (it === amount.amount) amount else amount.copy(amount = it) }
        is DynamicAmount.IfPositive -> r(amount.amount).let { if (it === amount.amount) amount else amount.copy(amount = it) }
        is DynamicAmount.Add -> pair(amount.left, amount.right, ::r) { l, rt -> amount.copy(left = l, right = rt) } ?: amount
        is DynamicAmount.Subtract -> pair(amount.left, amount.right, ::r) { l, rt -> amount.copy(left = l, right = rt) } ?: amount
        is DynamicAmount.Max -> pair(amount.left, amount.right, ::r) { l, rt -> amount.copy(left = l, right = rt) } ?: amount
        is DynamicAmount.Min -> pair(amount.left, amount.right, ::r) { l, rt -> amount.copy(left = l, right = rt) } ?: amount
        is DynamicAmount.Divide -> pair(amount.numerator, amount.denominator, ::r) { n, d -> amount.copy(numerator = n, denominator = d) } ?: amount
        else -> amount
    }
}

private inline fun pair(
    a: DynamicAmount,
    b: DynamicAmount,
    resolve: (DynamicAmount) -> DynamicAmount,
    rebuild: (DynamicAmount, DynamicAmount) -> DynamicAmount,
): DynamicAmount? {
    val ra = resolve(a)
    val rb = resolve(b)
    return if (ra === a && rb === b) null else rebuild(ra, rb)
}
