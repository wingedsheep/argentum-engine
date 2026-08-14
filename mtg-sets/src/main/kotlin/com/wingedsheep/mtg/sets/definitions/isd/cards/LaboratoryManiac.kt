package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ReplaceDrawWithEffect
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Laboratory Maniac
 * {2}{U}
 * Creature — Human Wizard
 * 2/2
 *
 * If you would draw a card while your library has no cards in it, you win the game instead.
 */
val LaboratoryManiac = card("Laboratory Maniac") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "If you would draw a card while your library has no cards in it, you win the game instead."

    replacementEffect(
        ReplaceDrawWithEffect(
            replacementEffect = Effects.WinGame(),
            restrictions = listOf(
                Exists(
                    player = Player.You,
                    zone = Zone.LIBRARY,
                    negate = true
                )
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Jason Felix"
        flavorText = "His mind whirled with grand plans, never thinking of what might happen if he were to succeed."
        imageUri =
            "https://cards.scryfall.io/normal/front/8/0/809205f3-acf5-4244-b360-09ce4ba76795.jpg?1783940974"

        ruling(
            "2021-03-19",
            "If for some reason you can't win the game, you won't lose for having tried to draw a card " +
                "from a library with no cards in it. The draw was still replaced."
        )
    }
}
