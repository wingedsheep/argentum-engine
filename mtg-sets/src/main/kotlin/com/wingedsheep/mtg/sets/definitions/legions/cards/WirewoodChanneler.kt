package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaEffect

/**
 * Wirewood Channeler
 * {3}{G}
 * Creature — Elf Druid
 * 2/2
 * {T}: Add X mana of any one color, where X is the number of Elves on the battlefield.
 */
val WirewoodChanneler = card("Wirewood Channeler") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Elf Druid"
    power = 2
    toughness = 2
    oracleText = "{T}: Add X mana of any one color, where X is the number of Elves on the battlefield."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddAnyColorManaEffect(
            amount = DynamicAmounts.creaturesWithSubtype(Subtype.ELF)
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "144"
        artist = "Alan Pollack"
        flavorText = "\"Your words are meaningless. The rustling of leaves is the only language that makes any sense.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36e5579e-dab7-49db-a141-a5bc5b5aee90.jpg?1562906076"
    }
}
