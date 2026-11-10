package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Oath of Gideon
 * {2}{W}
 * Legendary Enchantment
 * When Oath of Gideon enters, create two 1/1 white Kor Ally creature tokens.
 * Each planeswalker you control enters with an additional loyalty counter on it.
 *
 * The second ability is a global [EntersWithCounters] replacement (CR 614.1d): the extra loyalty
 * counter is added on top of the planeswalker's printed starting loyalty as it enters, whether it
 * was cast or put onto the battlefield some other way.
 */
val OathOfGideon = card("Oath of Gideon") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Enchantment"
    oracleText = "When Oath of Gideon enters, create two 1/1 white Kor Ally creature tokens.\n" +
        "Each planeswalker you control enters with an additional loyalty counter on it."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Kor", "Ally"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/2/e/2e820517-9895-47ba-a9d7-11ed807dcc6d.jpg?1783915235"
        )
    }

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.LOYALTY,
            count = 1,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Planeswalker.youControl(),
                to = Zone.BATTLEFIELD,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "30"
        artist = "Wesley Burt"
        flavorText = "\"For justice and peace, I will keep watch.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97976ade-c6ec-4068-be6b-953255b86a79.jpg?1783937924"
        ruling("2016-01-22", "The “legend rule” checks the full names of legendary permanents. You can control Oath of Gideon and Oath of Chandra at the same time, for example.")
        ruling("2016-01-22", "Even if you control a particular Oath, you can cast another of that Oath to take advantage of its enters-the-battlefield ability. It will enter the battlefield, causing its first ability to trigger. You will then choose one to keep, as detailed above, and then its enters-the-battlefield ability will resolve.")
        ruling("2016-01-22", "Each of the Oaths is legendary: if you control more than one of any particular Oath, you choose one to remain on the battlefield and the others are put into your graveyard.")
    }
}
