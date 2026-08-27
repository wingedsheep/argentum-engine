package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Abbot of Keral Keep
 * {1}{R}
 * Creature — Human Monk
 * 2/1
 * Prowess
 * When this creature enters, exile the top card of your library. Until end of turn, you may play
 * that card.
 */
val AbbotOfKeralKeep = card("Abbot of Keral Keep") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Monk"
    power = 2
    toughness = 1
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\nWhen this creature enters, exile the top card of your library. Until end of turn, you may play that card."

    keywords(Keyword.PROWESS)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Exile.impulse(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "127"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb7a2770-9a20-4f52-aac4-24502f50e374.jpg?1783938334"

        ruling("2015-06-22", "The card exiled by Abbot of Keral Keep's ability is exiled face up.")
        ruling("2015-06-22", "You may play that card that turn even if Abbot of Keral Keep is no longer on the battlefield or under your control.")
        ruling("2015-06-22", "Playing the card exiled with Abbot of Keral Keep's ability follows the normal rules for playing that card. You must pay its costs, and you must follow all applicable timing rules. For example, if the card is a creature card, you can cast that card by paying its mana cost only during your main phase while the stack is empty.")
        ruling("2015-06-22", "Unless an effect allows you to play additional lands that turn, you can play a land card exiled with Abbot of Keral Keep's ability only if you haven't played a land yet that turn.")
        ruling("2015-06-22", "If you don't play the card, it will remain exiled.")
        ruling("2015-06-22", "Any spell you cast that doesn't have the type creature will cause prowess to trigger. If a spell has multiple types, and one of those types is creature (such as an artifact creature), casting it won't cause prowess to trigger. Playing a land also won't cause prowess to trigger.")
        ruling("2015-06-22", "Prowess goes on the stack on top of the spell that caused it to trigger. It will resolve before that spell.")
        ruling("2015-06-22", "Once it triggers, prowess isn't connected to the spell that caused it to trigger. If that spell is countered, prowess will still resolve.")
    }
}
