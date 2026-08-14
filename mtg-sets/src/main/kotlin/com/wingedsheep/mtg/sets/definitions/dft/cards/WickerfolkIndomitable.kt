package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones

/**
 * Wickerfolk Indomitable
 * {3}{B}
 * Artifact Creature — Scarecrow
 * 4/3
 * You may cast this card from your graveyard by paying 2 life and sacrificing an artifact or
 * creature in addition to paying its other costs.
 *
 * [MayCastSelfFromZones] keeps normal timing and the printed {3}{B} mana cost; the two extra
 * costs ride along as a composite additional cost. Sacrificing the Wickerfolk itself is not an
 * option — it's in the graveyard, not on the battlefield, when the cost is paid.
 */
val WickerfolkIndomitable = card("Wickerfolk Indomitable") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Scarecrow"
    oracleText = "You may cast this card from your graveyard by paying 2 life and sacrificing an " +
        "artifact or creature in addition to paying its other costs."
    power = 4
    toughness = 3

    staticAbility {
        ability = MayCastSelfFromZones(
            zones = listOf(Zone.GRAVEYARD),
            additionalCost = Costs.additional.Composite(
                listOf(
                    Costs.additional.PayLife(2),
                    Costs.additional.SacrificePermanent(
                        GameObjectFilter.Artifact or GameObjectFilter.Creature
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "109"
        artist = "Sergio Cosmai"
        flavorText = "Some horrors are created. Others are the recognition of a possibility that " +
            "has always existed."
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba78e076-8962-4b3f-b86f-04400b062951.jpg?1783907888"
    }
}
