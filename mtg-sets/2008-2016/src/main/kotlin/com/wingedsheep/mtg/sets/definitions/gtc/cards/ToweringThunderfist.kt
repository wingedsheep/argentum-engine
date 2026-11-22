package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Towering Thunderfist
 * {4}{R}
 * Creature — Giant Soldier
 * 4/4
 * {W}: This creature gains vigilance until end of turn.
 */
val ToweringThunderfist = card("Towering Thunderfist") {
    manaCost = "{4}{R}"
    colorIdentity = "WR"
    typeLine = "Creature — Giant Soldier"
    oracleText = "{W}: This creature gains vigilance until end of turn."
    power = 4
    toughness = 4

    activatedAbility {
        cost = Costs.Mana("{W}")
        effect = Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "109"
        artist = "Zoltan Boros"
        flavorText = "\"Nothing ruins a party like one of those big killjoys bursting through the door and setting all the guests free.\"\n—Ruba, Rakdos cultist"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d68e9280-cb1a-48e1-a91e-217e101f19c5.jpg"
    }
}
