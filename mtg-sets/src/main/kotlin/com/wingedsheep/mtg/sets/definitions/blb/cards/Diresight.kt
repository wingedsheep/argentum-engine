package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Diresight
 * {2}{B}
 * Sorcery
 * Surveil 2, then draw two cards. You lose 2 life.
 */
val Diresight = card("Diresight") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Surveil 2, then draw two cards. You lose 2 life."

    spell {
        effect = LibraryPatterns.surveil(2)
            .then(Effects.DrawCards(2))
            .then(Effects.LoseLife(2, EffectTarget.PlayerRef(Player.You)))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "91"
        artist = "Alix Branwyn"
        flavorText = "There is a fine line between predicting the future and causing it."
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fada29c0-5293-40a4-b36d-d073ee99e650.jpg?1721426397"
    }
}
