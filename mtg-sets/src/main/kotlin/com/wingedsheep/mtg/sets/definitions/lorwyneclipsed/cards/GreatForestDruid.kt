package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost

/**
 * Great Forest Druid
 * {1}{G}
 * Creature — Treefolk Druid
 * 0/4
 * {T}: Add one mana of any color.
 */
val GreatForestDruid = card("Great Forest Druid") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Treefolk Druid"
    power = 0
    toughness = 4
    oracleText = "{T}: Add one mana of any color."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddAnyColorMana()
        manaAbility = true
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "178"
        artist = "Pete Venters"
        flavorText = "\"Forget the elves, that meddling faerie queen, and those greasy machines. If you ask me, it's the generous who should be remembered.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/7/8793a19e-6743-4031-86d9-2ff55f384549.jpg?1767732797"
    }
}
