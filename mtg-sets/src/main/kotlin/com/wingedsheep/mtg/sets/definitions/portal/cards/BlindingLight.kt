package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.TapUntapEffect

/**
 * Blinding Light
 * {2}{W}
 * Sorcery
 * Tap all nonwhite creatures.
 */
val BlindingLight = card("Blinding Light") {
    manaCost = "{2}{W}"
    typeLine = "Sorcery"

    spell {
        effect = ForEachInGroupEffect(GroupFilter.AllCreatures.notColor(Color.WHITE), TapUntapEffect(EffectTarget.Self, tap = true))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "John Coulthart"
        flavorText = "Let the unjust avert their faces from the light."
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4ea283d2-8f00-4836-81b4-c041b0469dcb.jpg"
    }
}
