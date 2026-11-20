package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sea God's Revenge
 * {5}{U}
 * Sorcery
 * Return up to three target creatures your opponents control to their owners' hands. Scry 1.
 */
val SeaGodsRevenge = card("Sea God's Revenge") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return up to three target creatures your opponents control to their owners' hands. " +
        "Scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    spell {
        targets(TargetFilter.CreatureOpponentControls, count = 3, optional = true)
        effect = Effects.ForEachTarget(Effects.ReturnToHand(EffectTarget.ContextTarget(0))) then
            Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Eric Velhagen"
        flavorText = "\"What has neither mouth nor throat, yet swallows captain, crew, and boat?\"\n—Sphinx's riddle"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5acdf4e4-5933-43dc-bf8b-25f89415db5b.jpg?1783939793"
    }
}
