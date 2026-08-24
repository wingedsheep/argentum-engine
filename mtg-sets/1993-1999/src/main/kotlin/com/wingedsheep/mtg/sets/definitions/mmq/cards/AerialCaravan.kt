package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Aerial Caravan
 * {4}{U}{U}
 * Creature — Human Soldier
 * 4 / 3
 *
 * The activated ability is the impulse-draw composition ([Patterns.Exile.impulse]) at count 1 with
 * the default [com.wingedsheep.sdk.scripting.effects.MayPlayExpiry.EndOfTurn] expiry — gather the
 * top card, move it to exile, grant "you may play it".
 */
val AerialCaravan = card("Aerial Caravan") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Soldier"
    oracleText = "Flying\n" +
        "{1}{U}{U}: Exile the top card of your library. Until end of turn, you may play that card. (Reveal the card as you exile it.)"
    power = 4
    toughness = 3

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{1}{U}{U}")
        effect = Patterns.Exile.impulse(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "DiTerlizzi"
        flavorText = "Successful delivery is *not* guaranteed."
        imageUri = "https://cards.scryfall.io/normal/front/a/d/adac91af-5165-4779-99f7-e75c83fa5d5d.jpg"
    }
}
