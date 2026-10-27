package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Cyclone Summoner — Kaldheim #52
 * {5}{U}{U} · Creature — Giant Wizard · 7/7 · Rare
 *
 * When this creature enters, if you cast it from your hand, return all permanents to their owners'
 * hands except for Giants, Wizards, and lands.
 *
 * The intervening-if is [Conditions.WasCastFromHand] (the Reiver Demon shape), so a reanimated or
 * blinked Summoner bounces nothing. The sweep is [Patterns.Group.returnAllToHand] over nonland
 * permanents that are neither Giants nor Wizards, gathered across the whole battlefield and moved
 * to each card's owner's hand. The Summoner is itself a Giant Wizard, so it stays.
 */
val CycloneSummoner = card("Cyclone Summoner") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Giant Wizard"
    power = 7
    toughness = 7
    oracleText = "When this creature enters, if you cast it from your hand, return all permanents " +
        "to their owners' hands except for Giants, Wizards, and lands."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasCastFromHand
        effect = Patterns.Group.returnAllToHand(
            GroupFilter(
                GameObjectFilter.NonlandPermanent.notAnyOfSubtypes(listOf(Subtype.GIANT, Subtype.WIZARD))
            )
        )
        description = "When this creature enters, if you cast it from your hand, return all " +
            "permanents to their owners' hands except for Giants, Wizards, and lands."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Andrey Kuzinskiy"
        flavorText = "Frost giants study for centuries in isolation, honing their ability to harness " +
            "elemental mayhem."
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2d40071-cadf-48de-a8ed-d55adbfab632.jpg?1783928266"
    }
}
