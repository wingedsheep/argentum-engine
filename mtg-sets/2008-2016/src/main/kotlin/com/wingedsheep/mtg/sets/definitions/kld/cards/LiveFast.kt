package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Live Fast — Kaladesh #87
 * {2}{B} · Sorcery
 *
 * You draw two cards, lose 2 life, and get {E}{E} (two energy counters).
 */
val LiveFast = card("Live Fast") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "You draw two cards, lose 2 life, and get {E}{E} (two energy counters)."

    spell {
        effect = Effects.DrawCards(2) then
            Effects.LoseLife(2, EffectTarget.Controller) then
            Effects.GetEnergy(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "87"
        artist = "Ryan Yee"
        flavorText = "\"When you have the chance, take it. Laugh, sing, dance. Don't allow the night to end.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad0bb8da-ad05-43d9-aba3-d917744168fe.jpg?1783937205"
    }
}
