package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * False Mourning
 * {G}
 * Sorcery
 * Put target card from your graveyard on top of your library.
 */
val FalseMourning = card("False Mourning") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Put target card from your graveyard on top of your library."

    spell {
        val t = target(TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.PutOnTopOfLibrary(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "134"
        artist = "Koji"
        flavorText = "Zhou Yu, Sun Ce, and other famous generals feigned their deaths in order to later surprise their opponents."
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61bdfefb-f2e2-409c-b5e1-66d24ab3ee5d.jpg?1783946102"
    }
}
