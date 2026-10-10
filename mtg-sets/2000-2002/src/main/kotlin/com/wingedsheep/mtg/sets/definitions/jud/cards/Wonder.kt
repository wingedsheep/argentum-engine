package com.wingedsheep.mtg.sets.definitions.jud.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Wonder
 * {3}{U}
 * Creature — Incarnation
 * 2/2
 * Flying
 * As long as this card is in your graveyard and you control an Island, creatures you control
 * have flying.
 *
 * `activeZones = setOf(Zone.GRAVEYARD)` is "as long as this card is in your graveyard" (CR 113.6b):
 * the grant is off while Wonder is on the battlefield and on once it dies. "You" is Wonder's owner,
 * so both the Island check and "creatures you control" read the graveyard's owner.
 */
val Wonder = card("Wonder") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Incarnation"
    oracleText = "Flying\n" +
        "As long as this card is in your graveyard and you control an Island, creatures you " +
        "control have flying."
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, GroupFilter(GameObjectFilter.Creature.youControl()))
        condition = Conditions.YouControl(Filters.IslandCard)
        activeZones = setOf(Zone.GRAVEYARD)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Rebecca Guay"
        flavorText = "\"The awestruck birds gazed at Wonder. Slowly, timidly, they rose into the air.\"\n—*Scroll of Beginnings*"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44670666-9028-4b4a-a5af-a3bf35fc6a21.jpg?1783945126"
    }
}
