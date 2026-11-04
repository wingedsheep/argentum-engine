package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanBlockAdditionalForCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

val SpikeTailedCeratops = card("Spike-Tailed Ceratops") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dinosaur"
    power = 4
    toughness = 4
    oracleText = "This creature can block an additional creature each combat."

    staticAbility {
        ability = CanBlockAdditionalForCreatureGroup(count = 1, filter = GroupFilter.source())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "209"
        artist = "Lius Lasahido"
        flavorText = "The conquistadors surrounded the dinosaur only to find that it had outflanked them."
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41a3b5f2-0b0b-42e0-a15b-4bb2fce05927.jpg?1783935717"
    }
}
