package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Embodiment of Agonies (M20 #98)
 * {1}{B}{B}  Creature — Demon  0/0
 *
 * Flying, deathtouch
 * This creature enters with a +1/+1 counter on it for each different mana cost among nonland
 * cards in your graveyard. (For example, {2}{B} and {1}{B}{B} are different mana costs.)
 */
val EmbodimentOfAgonies = card("Embodiment of Agonies") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 0
    toughness = 0
    oracleText = "Flying, deathtouch\n" +
        "This creature enters with a +1/+1 counter on it for each different mana cost among nonland " +
        "cards in your graveyard. (For example, {2}{B} and {1}{B}{B} are different mana costs.)"

    keywords(Keyword.FLYING, Keyword.DEATHTOUCH)

    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmounts.zone(Player.You, Zone.GRAVEYARD, GameObjectFilter.Nonland).distinctManaCosts()
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Igor Kieryluk"
        flavorText = "\"I feel your pain. It's exquisite.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/7/77972745-8689-4733-9a9d-39ae9d6273a2.jpg?1783932995"
    }
}
