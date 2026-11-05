package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.eternalize
import com.wingedsheep.sdk.model.Rarity

/**
 * Champion of Wits — Hour of Devastation #31 (canonical printing)
 * {2}{U} · Creature — Snake Wizard · 2/1
 *
 * When this creature enters, you may draw cards equal to its power. If you do, discard two cards.
 * Eternalize {5}{U}{U}
 *
 * Per the ruling, accepting the "may" always discards two, however many cards were drawn. The
 * eternalized 4/4 token copies the enters trigger, so it draws 4.
 */
val ChampionOfWits = card("Champion of Wits") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Snake Wizard"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, you may draw cards equal to its power. If you do, discard two cards.\n" +
        "Eternalize {5}{U}{U} ({5}{U}{U}, Exile this card from your graveyard: Create a token that's a copy of it, " +
        "except it's a 4/4 black Zombie Snake Wizard with no mana cost. Eternalize only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.DrawCards(DynamicAmounts.sourcePower()) then Effects.Discard(2)
        )
    }

    eternalize("{5}{U}{U}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Even Amundsen"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d765221d-9718-47eb-aa49-9c0719337139.jpg?1783936053"
        ruling("2017-07-14", "Use Champion of Wits's power as the first ability resolves to determine if you want to use the ability and how many cards you draw if you do. If you use the ability, you discard two cards regardless of how many cards you draw. If Champion of Wits leaves the battlefield before that ability resolves, use its last known power.")
        ruling("2017-07-14", "The token copies exactly what was printed on the original card and nothing else, except the characteristics specifically modified by eternalize. It doesn't copy any information about the object the card was before it was put into your graveyard.")
        ruling("2017-07-14", "If the card copied by the token had any \"when [this permanent] enters the battlefield\" abilities, then the token also has those abilities and will trigger them when it's created.")
    }
}
