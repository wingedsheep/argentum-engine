package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.eternalize
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sinuous Striker — Hour of Devastation #45 (canonical printing)
 * {2}{U} · Creature — Snake Warrior · 2/2
 *
 * {U}: This creature gets +1/-1 until end of turn.
 * Eternalize—{3}{U}{U}, Discard a card.
 */
val SinuousStriker = card("Sinuous Striker") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Snake Warrior"
    power = 2
    toughness = 2
    oracleText = "{U}: This creature gets +1/-1 until end of turn.\n" +
        "Eternalize—{3}{U}{U}, Discard a card. ({3}{U}{U}, Discard a card, Exile this card from your graveyard: " +
        "Create a token that's a copy of it, except it's a 4/4 black Zombie Snake Warrior with no mana cost. " +
        "Eternalize only as a sorcery.)"

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Effects.ModifyStats(1, -1, EffectTarget.Self)
    }

    eternalize("{3}{U}{U}", Costs.DiscardCard)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "45"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed2b97d1-3e69-4305-b1df-947ff64c5043.jpg?1783936048"
        ruling("2017-07-14", "Several cards have an eternalize cost that includes \"Discard a card.\" You can't discard the card with eternalize to pay its own cost because the card has to be in your graveyard to begin activating its eternalize ability.")
        ruling("2017-07-14", "The token copies exactly what was printed on the original card and nothing else, except the characteristics specifically modified by eternalize. It doesn't copy any information about the object the card was before it was put into your graveyard.")
    }
}
