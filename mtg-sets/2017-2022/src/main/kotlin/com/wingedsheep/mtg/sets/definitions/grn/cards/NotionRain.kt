package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Notion Rain
 * {1}{U}{B}
 * Sorcery
 * Surveil 2, then draw two cards. Notion Rain deals 2 damage to you.
 */
val NotionRain = card("Notion Rain") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Surveil 2, then draw two cards. Notion Rain deals 2 damage to you. (To surveil 2, look at " +
        "the top two cards of your library, then put any number of them into your graveyard and the rest on " +
        "top of your library in any order.)"

    spell {
        effect = Patterns.Library.surveil(2) then
            Effects.DrawCards(2) then
            Effects.DealDamage(2, EffectTarget.PlayerRef(Player.You))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "193"
        artist = "Lius Lasahido"
        flavorText = "City air is a constant drizzle of private thoughts."
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4db287e-34c1-459b-882d-3db58f13eade.jpg?1783934125"
    }
}
