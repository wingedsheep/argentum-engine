package com.wingedsheep.mtg.sets.definitions.mbs.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Consecrated Sphinx — Mirrodin Besieged #21 (canonical / earliest real printing, 2011)
 * {4}{U}{U} · Creature — Sphinx · 4/6
 *
 * Flying
 * Whenever an opponent draws a card, you may draw two cards.
 *
 * `Triggers.anOpponent.draws()` fires once per individual card drawn (the shape Mind's Eye's
 * scenario test proves) and matches any player who is an opponent of the controller, so in
 * multiplayer every opponent's draws trigger it. The "may" is all-or-nothing — draw two or
 * draw none — and is asked as each instance resolves.
 */
val ConsecratedSphinx = card("Consecrated Sphinx") {
    manaCost = "{4}{U}{U}"
    typeLine = "Creature — Sphinx"
    oracleText = "Flying\nWhenever an opponent draws a card, you may draw two cards."
    power = 4
    toughness = 6

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.anOpponent.draws()
        effect = Effects.May(Effects.DrawCards(2))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "21"
        artist = "Mark Zug"
        flavorText = "Blessed by the hands of Jin-Gitaxias."
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7f6b20c-9871-433c-8557-44493447e914.jpg?1783941389"
        ruling(
            "2017-11-17",
            "You may either draw two cards or not draw at all. You can't choose to draw only one card."
        )
        ruling(
            "2017-11-17",
            "The ability triggers once for each card an opponent draws. You choose whether to draw " +
                "two cards as each of those abilities resolves."
        )
        ruling(
            "2017-11-17",
            "If each player controls a Consecrated Sphinx, their abilities will cause each other to " +
                "trigger until one player chooses not to draw cards."
        )
    }
}
