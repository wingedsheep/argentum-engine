package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val DevoteeOfStrength = card("Devotee of Strength") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake Wizard"
    oracleText = "{4}{G}: Target creature gets +2/+2 until end of turn."
    power = 3
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{4}{G}")
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 2, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "113"
        artist = "Yeong-Hao Han"
        flavorText = "When Rhonas fell, his followers stirred with newfound strength—a final gift from a god they thought indomitable."
        imageUri = "https://cards.scryfall.io/normal/front/9/7/977698ab-3b64-41f2-a214-ac95e04c9956.jpg?1783936021"
    }
}
