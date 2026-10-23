package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Exponential Growth — Strixhaven: School of Mages #130 (canonical printing)
 * {X}{X}{G}{G} · Sorcery
 *
 * Until end of turn, double target creature's power X times.
 *
 * Each doubling is the Bulk Up shape — +N/+0 until end of turn where N is the creature's power as
 * that doubling applies — wrapped in [Effects.Repeat] X times. The repeated bodies run in
 * sequence and each reads the power the previous one left, so a 2/2 at X = 3 becomes 4, 8, then
 * 16 (the "sequentially, not all at the same time" ruling). A negative power doubles downward,
 * because the bonus is the (negative) power itself. X = 0 repeats nothing.
 */
val ExponentialGrowth = card("Exponential Growth") {
    manaCost = "{X}{X}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Until end of turn, double target creature's power X times."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Repeat(
            DynamicAmounts.xValue(),
            Effects.ModifyStats(
                power = DynamicAmounts.powerOf(creature),
                toughness = DynamicAmounts.fixed(0),
                target = creature
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "130"
        artist = "Izzy"
        flavorText = "Most Quandrix students fundamentally disagree with the concept of \"enough.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c129bb7-9489-44bd-a46d-c5f738bf9d25.jpg?1783927343"
        ruling("2021-04-16", "If an effect instructs you to \"double\" a creature's power, that creature gets +X/+0, where X is its power as that effect begins to apply. (This is not the value of X you chose when you cast the spell)")
        ruling("2021-04-16", "Exponential Growth doubles a creature's power sequentially, not all at the same time. For example, if X is 3 and you target a 2/2 creature, you will double its power once to 4, again to 8, and a third time to 16, making it a 16/2 creature until end of turn.")
        ruling("2021-04-16", "If a creature's power is less than 0 when it's doubled, instead that creature gets -X/-0, where X is how much less than 0 its power is. For example, if an effect has given a 5/5 creature -7/-0 so that it's a -2/5 creature, doubling its power makes it a -4/5 creature.")
    }
}
