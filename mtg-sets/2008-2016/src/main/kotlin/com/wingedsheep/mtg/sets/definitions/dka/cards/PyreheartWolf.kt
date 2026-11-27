package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Pyreheart Wolf
 * {2}{R}
 * Creature — Wolf
 * 1/1
 * Whenever this creature attacks, creatures you control gain menace until end of turn.
 * Undying
 */
val PyreheartWolf = card("Pyreheart Wolf") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Wolf"
    oracleText = "Whenever this creature attacks, creatures you control gain menace until end of turn. (They can't be blocked except by two or more creatures.)\n" +
        "Undying (When this creature dies, if it had no +1/+1 counters on it, return it to the battlefield under its owner's control with a +1/+1 counter on it.)"
    power = 1
    toughness = 1
    keywords(Keyword.UNDYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Group.grantKeywordToAll(
            keyword = Keyword.MENACE,
            filter = GroupFilter.AllCreaturesYouControl
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/9722f20c-e0d9-4165-8cd5-4abadc5378eb.jpg"
    }
}
