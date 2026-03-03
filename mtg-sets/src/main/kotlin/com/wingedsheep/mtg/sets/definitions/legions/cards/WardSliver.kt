package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantProtectionFromChosenColorToGroup
import com.wingedsheep.sdk.scripting.EntersWithColorChoice
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Ward Sliver
 * {4}{W}
 * Creature — Sliver
 * 2/2
 * As this creature enters, choose a color.
 * All Slivers have protection from the chosen color.
 */
val WardSliver = card("Ward Sliver") {
    manaCost = "{4}{W}"
    typeLine = "Creature — Sliver"
    power = 2
    toughness = 2
    oracleText = "As this creature enters, choose a color. All Slivers have protection from the chosen color."

    replacementEffect(EntersWithColorChoice())

    staticAbility {
        ability = GrantProtectionFromChosenColorToGroup(
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Sliver"))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Pete Venters"
        flavorText = "The first wave of slivers perished from the Riptide wizards' magic. The second wave shrugged off their spells like water."
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e264369a-ab81-4938-9fa6-7c3e069442f4.jpg?1562940465"
    }
}
