package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/** Draw a card for each color among permanents you control — counted as the ability resolves. */
private val drawForEachColorAmongPermanents = Effects.DrawCards(DynamicAmounts.colorsAmongPermanents())

/**
 * Soul of Ravnica
 * {4}{U}{U}
 * Creature — Avatar
 * 6/6
 * Flying
 * {5}{U}{U}: Draw a card for each color among permanents you control.
 * {5}{U}{U}, Exile this card from your graveyard: Draw a card for each color among permanents you control.
 *
 * Soul of Theros's shape: both abilities share one effect; the second is activated from the
 * graveyard with exiling this card as part of its cost. The count is Chromatic Orrery's
 * [DynamicAmounts.colorsAmongPermanents] — projected colors, read on resolution, zero to five.
 */
val SoulOfRavnica = card("Soul of Ravnica") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Avatar"
    power = 6
    toughness = 6
    oracleText = "Flying\n" +
        "{5}{U}{U}: Draw a card for each color among permanents you control.\n" +
        "{5}{U}{U}, Exile this card from your graveyard: Draw a card for each color among permanents you control."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{5}{U}{U}")
        effect = drawForEachColorAmongPermanents
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}{U}{U}"), Costs.ExileSelf)
        effect = drawForEachColorAmongPermanents
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "78"
        artist = "Stephan Martiniere"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7332471-c052-4a24-abf1-a5805d685658.jpg?1783939188"
        ruling("2014-07-18", "To determine how many cards to draw, count the number of colors among permanents you control as either activated ability resolves. You'll draw between zero and five cards.")
    }
}
