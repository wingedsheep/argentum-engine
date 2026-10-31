package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Master Splicer (NPH #16)
 * {3}{W}  Creature — Phyrexian Human Artificer  1/1
 *
 * When this creature enters, create a 3/3 colorless Phyrexian Golem artifact creature token.
 * Golems you control get +1/+1.
 */
val MasterSplicer = card("Master Splicer") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Human Artificer"
    oracleText = "When this creature enters, create a 3/3 colorless Phyrexian Golem artifact creature token.\nGolems you control get +1/+1."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            creatureTypes = setOf("Phyrexian", "Golem"),
            artifactToken = true
        )
    }

    staticAbility {
        ability = ModifyStats(1, 1, GroupFilter(GameObjectFilter.Permanent.withSubtype("Golem").youControl()))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "Chippy"
        flavorText = "With Phyrexia's victory at hand, each sect began perfecting its vision."
        imageUri = "https://cards.scryfall.io/normal/front/8/5/859d2b91-63af-4700-8ca5-b1756aa6639b.jpg"
    }
}
