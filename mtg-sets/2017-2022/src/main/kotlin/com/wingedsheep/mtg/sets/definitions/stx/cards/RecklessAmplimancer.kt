package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reckless Amplimancer — Strixhaven: School of Mages #141 (canonical printing)
 * {1}{G} · Creature — Elf Druid · 2/2
 *
 * {4}{G}: Double this creature's power and toughness until end of turn.
 *
 * "Double" is +X/+Y where X and Y are its power and toughness as the effect begins to apply
 * (the same self-doubling shape as Grunn, the Lonely King), so a negative power doubles to a
 * more negative one.
 */
val RecklessAmplimancer = card("Reckless Amplimancer") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    oracleText = "{4}{G}: Double this creature's power and toughness until end of turn."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{4}{G}")
        effect = Effects.ModifyStats(DynamicAmounts.sourcePower(), DynamicAmounts.sourceToughness(), EffectTarget.Self)
        description = "{4}{G}: Double this creature's power and toughness until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "141"
        artist = "Yigit Koroglu"
        flavorText = "\"Linear growth? What am I, a first-year?\""
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc8072b1-6080-45cd-b1c7-353b54a55931.jpg?1783927338"
        ruling("2021-04-16", "If an effect instructs you to \"double\" a creature's power, that creature gets +X/+0, where X is its power as that effect begins to apply. The same is true for toughness.")
        ruling("2021-04-16", "If a creature's power is less than 0 when it's doubled, instead that creature gets -X/-0, where X is how much less than 0 its power is. For example, if an effect has given Reckless Amplimancer -4/-0 so that it's a -2/2 creature, doubling its power and toughness gives it -2/+2, and it becomes a -4/4 creature.")
    }
}
