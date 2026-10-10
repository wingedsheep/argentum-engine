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
 * Anger
 * {3}{R}
 * Creature — Incarnation
 * 2/2
 * Haste
 * As long as this card is in your graveyard and you control a Mountain, creatures you control
 * have haste.
 *
 * `activeZones = setOf(Zone.GRAVEYARD)` is "as long as this card is in your graveyard" (CR 113.6b):
 * the grant is off while Anger is on the battlefield and on once it dies. "You" is Anger's owner,
 * so both the Mountain check and "creatures you control" read the graveyard's owner.
 */
val Anger = card("Anger") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Incarnation"
    oracleText = "Haste\n" +
        "As long as this card is in your graveyard and you control a Mountain, creatures you " +
        "control have haste."
    power = 2
    toughness = 2

    keywords(Keyword.HASTE)

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, GroupFilter(GameObjectFilter.Creature.youControl()))
        condition = Conditions.YouControl(Filters.MountainCard)
        activeZones = setOf(Zone.GRAVEYARD)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "77"
        artist = "John Avon"
        flavorText = "\"For its time as a mortal, Anger chose a shell of boiling rock.\"\n—*Scroll of Beginnings*"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa2920af-e6a1-4939-ab59-67af4430e5b8.jpg?1783945121"
        ruling("2004-10-04", "The timestamp for the \"in your graveyard\" ability is set at the time that this card goes to your graveyard, regardless of whether you control a Mountain at that time.")
    }
}
