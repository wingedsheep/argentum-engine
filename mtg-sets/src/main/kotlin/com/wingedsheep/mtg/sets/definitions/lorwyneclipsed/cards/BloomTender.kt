package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Bloom Tender
 * {1}{G}
 * Creature — Elf Druid
 * 1/1
 *
 * Vivid — {T}: For each color among permanents you control, add one mana of that color.
 */
val BloomTender = card("Bloom Tender") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Elf Druid"
    power = 1
    toughness = 1
    oracleText = "Vivid — {T}: For each color among permanents you control, add one mana of that color."

    keywords(Keyword.VIVID)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddOneManaOfEachColorAmong(GameObjectFilter.Permanent.youControl())
        manaAbility = true
        description = "Vivid — {T}: For each color among permanents you control, add one mana of that color."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "166"
        artist = "Nils Hamm"
        flavorText = "Heart calmed by the beauty of the blossom, the dawnhand wondered who was actually protecting whom."
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba86688d-18f0-4b5c-a797-42bf125a6c9f.jpg?1767658349"
    }
}
