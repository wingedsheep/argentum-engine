package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Smuggler's Copter — Kaladesh #235 (canonical printing)
 * {2} · Artifact — Vehicle · 3/3
 *
 * Flying
 * Whenever this Vehicle attacks or blocks, you may draw a card. If you do, discard a card.
 * Crew 1
 *
 * "Attacks or blocks" is two self-triggers sharing one optional loot (the Spider-Mobile shape);
 * a Vehicle can't both attack and block in the same combat, so at most one fires.
 */
val SmugglersCopter = card("Smuggler's Copter") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Vehicle"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever this Vehicle attacks or blocks, you may draw a card. If you do, discard a card.\n" +
        "Crew 1 (Tap any number of creatures you control with total power 1 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.May(Patterns.Hand.loot())
    }

    triggeredAbility {
        trigger = Triggers.self.blocks()
        effect = Effects.May(Patterns.Hand.loot())
    }

    keywordAbility(KeywordAbility.crew(1))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "235"
        artist = "Florian de Gesincourt"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/7832abb5-5107-4603-904e-491b221bd3e3.jpg?1783937147"
        ruling(
            "2017-09-29",
            "You may activate a crew ability of a Vehicle even if it's already an artifact creature. " +
                "Doing so has no effect on the Vehicle. It doesn't change its power and toughness."
        )
    }
}
