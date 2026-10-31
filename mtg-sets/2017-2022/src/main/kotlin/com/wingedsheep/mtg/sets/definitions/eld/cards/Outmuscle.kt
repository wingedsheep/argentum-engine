package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Outmuscle
 * {3}{G}
 * Sorcery
 * Put a +1/+1 counter on target creature you control, then it fights target creature you don't
 * control.
 * Adamant — If at least three green mana was spent to cast this spell, the creature you control
 * gains indestructible until end of turn.
 *
 * The adamant instruction runs after the fight, in printed order; state-based actions aren't checked
 * mid-resolution, so indestructible still saves a creature dealt lethal damage in the fight. If only
 * the opposing target is illegal, the fight does nothing but the counter and indestructible still apply.
 */
val Outmuscle = card("Outmuscle") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on target creature you control, then it fights target creature you don't control. (Each deals damage equal to its power to the other.)\n" +
        "Adamant — If at least three green mana was spent to cast this spell, the creature you control gains indestructible until end of turn."

    spell {
        val mine = target(TargetFilter.CreatureYouControl)
        val theirs = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, mine) then
            Effects.Fight(mine, theirs) then
            Effects.If(
                Conditions.CompareAmounts(
                    DynamicAmounts.manaOfColorSpent(Color.GREEN),
                    ComparisonOperator.GTE,
                    3,
                ),
                Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, mine),
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "170"
        artist = "Viktor Titov"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40d0ddcc-e6df-4fe1-9ac9-f895885ccc84.jpg?1783932605"
        ruling("2019-10-04", "If either target is an illegal target as Outmuscle tries to resolve, neither creature will deal or be dealt damage.")
        ruling("2019-10-04", "If the creature you control is an illegal target as Outmuscle tries to resolve, no creature gets a +1/+1 counter. If that creature is a legal target but the creature you don't control isn't, it still gets a +1/+1 counter and gains indestructible if appropriate.")
        ruling("2019-10-04", "Because state-based actions don't destroy a creature with lethal damage marked on it until a player would receive priority, gaining indestructible from Outmuscle will save your creature if it's dealt lethal damage in the fight.")
    }
}
