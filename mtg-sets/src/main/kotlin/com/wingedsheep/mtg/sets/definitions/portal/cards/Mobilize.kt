package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect

/**
 * Mobilize
 * {G}
 * Sorcery
 * Untap all creatures you control.
 */
val Mobilize = card("Mobilize") {
    manaCost = "{G}"
    typeLine = "Sorcery"

    spell {
        effect = ForEachInGroupEffect(GroupFilter.AllCreaturesYouControl, TapUntapEffect(EffectTarget.Self, tap = false))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "172"
        artist = "Rebecca Guay"
        flavorText = "Rise, warriors! The battle awaits!"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/9712ecaa-4059-44ba-98b7-07bfe7411b5b.jpg"
    }
}
