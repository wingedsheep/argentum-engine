package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sculptor of Winter
 * {1}{G}
 * Snow Creature — Elf Rogue
 * 2/2
 * {T}: Untap target snow land.
 */
val SculptorOfWinter = card("Sculptor of Winter") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Snow Creature — Elf Rogue"
    power = 2
    toughness = 2
    oracleText = "{T}: Untap target snow land."

    activatedAbility {
        cost = Costs.Tap
        val land = target(TargetFilter(GameObjectFilter.Land.snow()))
        effect = Effects.Untap(land)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "193"
        artist = "PINDURSKI"
        flavorText = "\"I and my kin remember the old ways. The frost whispers to me in a language too ancient for the Skoti usurpers to comprehend.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9dab2ca2-0039-4eac-a7dc-68756362737d.jpg?1783928205"

        ruling("2021-02-05", "Sculptor of Winter's activated ability is not a mana ability. It can't be activated while casting a spell or activating an ability. It uses the stack and can be responded to.")
    }
}
