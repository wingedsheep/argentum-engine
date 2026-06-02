package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Sliver Overlord
 * {W}{U}{B}{R}{G}
 * Legendary Creature — Sliver Mutant
 * 7/7
 * {3}: Search your library for a Sliver card, reveal that card, put it into your hand, then shuffle.
 * {3}: Gain control of target Sliver. (This effect lasts indefinitely.)
 */
val SliverOverlord = card("Sliver Overlord") {
    manaCost = "{W}{U}{B}{R}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Sliver Mutant"
    power = 7
    toughness = 7
    oracleText = "{3}: Search your library for a Sliver card, reveal that card, put it into your hand, then shuffle.\n{3}: Gain control of target Sliver. (This effect lasts indefinitely.)"

    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = LibraryPatterns.searchLibrary(
            filter = GameObjectFilter.Any.withSubtype("Sliver"),
            reveal = true
        )
    }

    activatedAbility {
        cost = Costs.Mana("{3}")
        val t = target("target", TargetObject(filter = TargetFilter(GameObjectFilter.Permanent.withSubtype("Sliver"))))
        effect = Effects.GainControl(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "139"
        artist = "Tony Szczudlo"
        flavorText = "\"The end of evolution.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c16915b-c50d-4fb5-830f-9ca4597a9c0f.jpg?1562527622"
    }
}
