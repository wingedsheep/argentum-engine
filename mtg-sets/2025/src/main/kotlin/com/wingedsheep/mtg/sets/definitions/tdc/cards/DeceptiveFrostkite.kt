package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Deceptive Frostkite — Tarkir: Dragonstorm Commander #19
 * {U}{U} · Creature — Dragon · 1/1
 *
 * Flying
 * You may have this creature enter as a copy of a creature you control with power 4 or greater,
 * except it's a Dragon in addition to its other types and it has flying.
 *
 * Mockingbird's clone shape with Moritte's "you control" candidate filter; declining leaves a
 * 1/1 flying Dragon.
 */
val DeceptiveFrostkite = card("Deceptive Frostkite") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Dragon"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "You may have this creature enter as a copy of a creature you control with power 4 or greater, " +
        "except it's a Dragon in addition to its other types and it has flying."

    keywords(Keyword.FLYING)

    replacementEffect(
        EntersAsCopy(
            optional = true,
            copyFilter = GameObjectFilter.Creature.youControl().powerAtLeast(4),
            additionalSubtypes = listOf("Dragon"),
            additionalKeywords = listOf(Keyword.FLYING)
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Josu Solano"
        flavorText = "\"That's not a dragon. Can't be. It's too small, isn't it?\"\n—Askar, Temur scout, last words"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f87120f-d2a7-4c06-aadd-3ed5729d7abf.jpg?1783907182"
        ruling(
            "2025-04-04",
            "You can choose not to have Deceptive Frostkite enter as a copy of another creature. If you do, " +
                "it will just be a 1/1 Dragon with flying."
        )
        ruling(
            "2025-04-04",
            "Any enters abilities of the copied creature will trigger when Deceptive Frostkite enters. Any " +
                "\"as [this creature] enters\" or \"[this creature] enters with\" abilities of the copied " +
                "permanent will also work."
        )
    }
}
