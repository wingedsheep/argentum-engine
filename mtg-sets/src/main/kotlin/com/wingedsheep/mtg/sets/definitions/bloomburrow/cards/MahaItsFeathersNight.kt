package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.SetBaseToughnessForCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Maha, Its Feathers Night
 * {3}{B}{B}
 * Legendary Creature — Elemental Bird
 * 6/5
 *
 * Flying, trample
 * Ward—Discard a card.
 * Creatures your opponents control have base toughness 1.
 */
val MahaItsFeathersNight = card("Maha, Its Feathers Night") {
    manaCost = "{3}{B}{B}"
    typeLine = "Legendary Creature — Elemental Bird"
    power = 6
    toughness = 5
    oracleText = "Flying, trample\n" +
        "Ward—Discard a card.\n" +
        "Creatures your opponents control have base toughness 1."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)
    keywordAbility(KeywordAbility.wardDiscard())

    staticAbility {
        ability = SetBaseToughnessForCreatureGroup(
            toughness = 1,
            filter = GroupFilter.AllCreaturesOpponentsControl
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "100"
        artist = "Alessandra Pisano"
        flavorText = "Its wingspan reaches from dusk to dawn."
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf3320ec-c4e8-405a-982d-e009c58c9e21.jpg?1721426449"
        ruling("2024-07-26", "Maha's last ability overwrites all previous effects that set those creatures' base toughness to specific values. Any toughness-setting effects that start to apply afterward will overwrite this effect.")
        ruling("2024-07-26", "Effects that modify a creature's power and/or toughness, such as the effect of Overprotect, will apply to the creature no matter when they started to take effect. The same is true for counters that change its power and/or toughness and effects that switch its power and toughness.")
    }
}
