package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Battle at the Bridge
 * {X}{B}
 * Sorcery
 *
 * Improvise
 * Target creature gets -X/-X until end of turn. You gain X life.
 *
 * Improvise pays generic mana of the total cost, the announced X included (the card's own
 * ruling), so X stays the announced value however many artifacts were tapped for it.
 */
val BattleAtTheBridge = card("Battle at the Bridge") {
    manaCost = "{X}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Improvise (Your artifacts can help cast this spell. Each artifact you tap after " +
        "you're done activating mana abilities pays for {1}.)\n" +
        "Target creature gets -X/-X until end of turn. You gain X life."

    keywords(Keyword.IMPROVISE)

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-DynamicAmounts.xValue(), -DynamicAmounts.xValue(), creature) then
            Effects.GainLife(DynamicAmounts.xValue())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "53"
        artist = "Chris Rallis"
        flavorText = "\"This is bigger than you. All of you.\"\n—Tezzeret"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74d8b644-4cca-451e-a46c-5237c13bf373.jpg?1783936766"
        ruling("2017-02-09", "If the target creature becomes an illegal target, Battle at the Bridge doesn't resolve and none of its effects happen. You won't gain life.")
        ruling("2017-02-09", "When using improvise to cast a spell with {X} in its mana cost, first choose the value for X. That choice, plus any cost increases or decreases, will determine the spell's total cost. Then you can tap artifacts you control to help pay that cost. For example, if you cast Whir of Invention (a spell with improvise and mana cost {X}{U}{U}{U}) and choose X to be 3, the total cost is {3}{U}{U}{U}. If you tap two artifacts, you'll have to pay {1}{U}{U}{U}.")
        ruling("2017-02-09", "Improvise can't pay for {W}, {U}, {B}, {R}, {G}, or {C} mana symbols in a spell's total cost.")
        ruling("2017-02-09", "Improvise doesn't change a spell's mana cost or mana value.")
    }
}
