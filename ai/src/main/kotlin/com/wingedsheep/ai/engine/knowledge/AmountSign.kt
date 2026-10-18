package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

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
 * fall through to a guess. Anything that can genuinely land on either side — power, toughness, a
 * life total mid-resolution, a pipeline variable — is [UNKNOWN], and callers treat that exactly as
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
            is DynamicAmount.Max -> max(of(amount.left), of(amount.right))
            is DynamicAmount.Min -> max(of(amount.left).flipped(), of(amount.right).flipped()).flipped()
            is DynamicAmount.Conditional -> either(of(amount.ifTrue), of(amount.ifFalse))
            is DynamicAmount.GreatestAmongPlayers -> of(amount.inner)
            is DynamicAmount.LeastAmongPlayers -> of(amount.inner)

            // Power and toughness can be negative; so can a life total before state-based actions.
            is DynamicAmount.EntityProperty -> when (amount.numericProperty) {
                EntityNumericProperty.Power, EntityNumericProperty.Toughness,
                EntityNumericProperty.BasePower, EntityNumericProperty.BaseToughness -> UNKNOWN
                else -> GAIN
            }
            is DynamicAmount.AggregateBattlefield -> ofCardProperty(amount.property)
            is DynamicAmount.AggregateZone -> ofCardProperty(amount.property)
            is DynamicAmount.YourLifeTotal, is DynamicAmount.LifeTotal -> UNKNOWN
            // Opaque at analysis time: a pipeline variable or a trigger-context value could be anything.
            is DynamicAmount.VariableReference, is DynamicAmount.ContextProperty -> UNKNOWN
            // A cast-time capture holds whatever amount the card froze, which can be negative.
            is DynamicAmount.CapturedAtCast -> UNKNOWN

            // Counts, mana values, mana spent, X: none of them can go below zero.
            is DynamicAmount.Speed, is DynamicAmount.DungeonsCompleted, is DynamicAmount.PlayerCounterCount, is DynamicAmount.CardsCycledThisGame,
            is DynamicAmount.StartingLifeTotal, is DynamicAmount.UnspentMana, is DynamicAmount.XValue,
            is DynamicAmount.LastKnownSourceCounters, is DynamicAmount.LastKnownDamageDealtToSource,
            is DynamicAmount.CastX, is DynamicAmount.CastChoice, is DynamicAmount.TotalManaSpent,
            is DynamicAmount.ManaSpentOnX, is DynamicAmount.ManaSpentFromSubtype, is DynamicAmount.ManaOfColorSpent,
            is DynamicAmount.SnowManaSpent, is DynamicAmount.DistinctColorsManaSpent, is DynamicAmount.DevotionTo, is DynamicAmount.CommanderColorIdentityCount,
            is DynamicAmount.StoredCardManaValue, is DynamicAmount.DistinctEntitiesInCollections,
            is DynamicAmount.DistinctCardTypesInCollections, is DynamicAmount.ManaValueSumOfCollection,
            is DynamicAmount.PlayerCount, is DynamicAmount.CountPlayersWith, is DynamicAmount.GraveyardRelativeCount,
            is DynamicAmount.Count, is DynamicAmount.UnlockedDoors, is DynamicAmount.StationCharge,
            is DynamicAmount.TurnTracking, is DynamicAmount.SpellsCastThisTurn, is DynamicAmount.SpellsCastLastTurn,
            is DynamicAmount.CraftedMaterialsTotalPower, is DynamicAmount.CraftedMaterialsTotalManaValue,
            is DynamicAmount.CraftedMaterialsColorCount, is DynamicAmount.CreaturesThatCrewedOrSaddledThisTurn,
            is DynamicAmount.CardTypeEnteredUnderControlThisTurn, is DynamicAmount.SubtypeEnteredUnderControlThisTurn,
            is DynamicAmount.CreaturesWithSubtypeDiedThisTurn, is DynamicAmount.PermanentsSacrificedThisWay,
            is DynamicAmount.CountersRemovedAsCost, is DynamicAmount.TotalPowerSacrificedThisWay,
            is DynamicAmount.LargestSharedCreatureTypeCount -> GAIN
        }

        private fun ofInt(n: Int): AmountSign = when {
            n > 0 -> GAIN
            n < 0 -> LOSS
            else -> ZERO
        }

        private fun ofCardProperty(property: CardNumericProperty?): AmountSign = when (property) {
            CardNumericProperty.POWER, CardNumericProperty.TOUGHNESS -> UNKNOWN
            null, CardNumericProperty.MANA_VALUE, CardNumericProperty.COUNTERS,
            CardNumericProperty.COLOR_COUNT -> GAIN
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
