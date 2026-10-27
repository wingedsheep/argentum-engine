package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Gladewalker Ritualist
 * {2}{G}
 * Creature — Shapeshifter
 * 3/3
 * Changeling (This card is every creature type.)
 * Whenever another creature you control named Gladewalker Ritualist enters, draw a card.
 *
 * "Another … named Gladewalker Ritualist" is `Triggers.another(...)` (OTHER binding, so its own
 * entry never fires it) over a creature-you-control filter narrowed by `named(...)`.
 */
val GladewalkerRitualist = card("Gladewalker Ritualist") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Shapeshifter"
    oracleText = "Changeling (This card is every creature type.)\n" +
        "Whenever another creature you control named Gladewalker Ritualist enters, draw a card."
    power = 3
    toughness = 3

    keywords(Keyword.CHANGELING)

    triggeredAbility {
        trigger = Triggers.another(
            GameObjectFilter.Creature.youControl().named("Gladewalker Ritualist")
        ).enters()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "392"
        artist = "Irina Nordsol"
        flavorText = "\"We are all.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/0/8053b888-a680-4318-88c3-0924e596e780.jpg?1783928116"

        ruling("2021-02-05", "If multiple creatures named Gladewalker Ritualist all enter the battlefield under your control at the same time, each of their abilities will trigger once for each of the others.")
    }
}
