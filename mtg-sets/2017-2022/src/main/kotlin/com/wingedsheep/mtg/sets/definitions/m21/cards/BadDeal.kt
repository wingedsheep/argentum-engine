package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bad Deal
 * {4}{B}{B}
 * Sorcery
 *
 * You draw two cards and each opponent discards two cards. Each player loses 2 life.
 */
val BadDeal = card("Bad Deal") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "You draw two cards and each opponent discards two cards. Each player loses 2 life."

    spell {
        effect = Effects.DrawCards(2) then
            Effects.EachOpponentDiscards(2) then
            Effects.LoseLife(2, EffectTarget.PlayerRef(Player.Each))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "89"
        artist = "Zoltan Boros"
        flavorText = "There are still places where a smile and a handshake are all you need to seal a deal."
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a3f8bc2-ef66-474f-92a3-9c4df1670cec.jpg?1783930712"
        ruling("2020-06-23", "If an opponent has just one card in hand, that player discards just one card. Other opponents still discard two if able.")
        ruling("2020-06-23", "Each player loses 2 life, including you, regardless of how many cards they draw or discard.")
    }
}
