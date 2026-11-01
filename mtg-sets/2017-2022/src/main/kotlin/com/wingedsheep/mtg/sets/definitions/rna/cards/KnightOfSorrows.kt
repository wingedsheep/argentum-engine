package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanBlockAdditionalForCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Knight of Sorrows — Ravnica Allegiance #14
 * {4}{W} · Creature — Human Knight · 3/3
 * This creature can block an additional creature each combat.
 * Afterlife 1 (When this creature dies, create a 1/1 white and black Spirit creature token with flying.)
 *
 * The extra block is [CanBlockAdditionalForCreatureGroup] scoped to [GroupFilter.source] (Lairwatch
 * Giant's shape); afterlife is composed as its reminder text (Ministrant of Obligation).
 */
val KnightOfSorrows = card("Knight of Sorrows") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 3
    toughness = 3
    oracleText = "This creature can block an additional creature each combat.\n" +
        "Afterlife 1 (When this creature dies, create a 1/1 white and black Spirit creature token with flying.)"

    staticAbility {
        ability = CanBlockAdditionalForCreatureGroup(
            count = 1,
            filter = GroupFilter.source(),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE, Color.BLACK),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/4/5/45b3bdd7-b093-4bfd-8a9a-965e72bfefb3.jpg?1783933602",
        )
        description = "Afterlife 1 (When this creature dies, create a 1/1 white and black Spirit " +
            "creature token with flying.)"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "Zezhou Chen"
        flavorText = "The helmet reveals no eyes to witness your penance, no mouth to offer absolution."
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6ce239e9-9b35-4dde-8a44-6c7ce4eb2d1a.jpg?1783933721"
        ruling(
            "2019-01-25",
            "Because blockers are chosen all at once, you can't block with a creature with afterlife, wait for " +
                "it to die, then block with the resulting Spirit tokens."
        )
    }
}
