package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cruel Truths
 * {3}{B}
 * Instant
 *
 * Surveil 2, then draw two cards. You lose 2 life.
 */
val CruelTruths = card("Cruel Truths") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Surveil 2, then draw two cards. You lose 2 life. " +
        "(To surveil 2, look at the top two cards of your library, then put any number of " +
        "them into your graveyard and the rest on top of your library in any order.)"

    spell {
        effect = EffectPatterns.surveil(2)
            .then(Effects.DrawCards(2))
            .then(Effects.LoseLife(2, EffectTarget.PlayerRef(Player.You)))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Fajareka Setiawan"
        flavorText = "\"Who are you to defy me? In my time, the sibsig knew their place.\"\n—Sidisi, to Nishang"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/6852b4d5-74e0-44ba-ba44-20aa91e3c4c8.jpg?1743204266"
    }
}
