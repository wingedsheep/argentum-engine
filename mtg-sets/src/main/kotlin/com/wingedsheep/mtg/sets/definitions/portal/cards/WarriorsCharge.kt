package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.ModifyStatsEffect

/**
 * Warrior's Charge
 * {2}{W}
 * Sorcery
 * Creatures you control get +1/+1 until end of turn.
 */
val WarriorsCharge = card("Warrior's Charge") {
    manaCost = "{2}{W}"
    typeLine = "Sorcery"

    spell {
        effect = ForEachInGroupEffect(
            GroupFilter.AllCreaturesYouControl,
            ModifyStatsEffect(1, 1, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Ted Naifeh"
        flavorText = "\"It is not the absence of fear that makes a warrior, but its domination.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/6/8668e4af-ae89-4fab-8015-8dc643c6cd36.jpg"
    }
}
