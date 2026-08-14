package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/** Rise from the Wreck — Aetherdrift #178. */
val RiseFromTheWreck = card("Rise from the Wreck") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Return up to one target creature card, up to one target Mount card, up to one " +
        "target Vehicle card, and up to one target creature card with no abilities from your " +
        "graveyard to your hand."

    spell {
        val creature = target(
            "creature card",
            TargetObject(optional = true, filter = TargetFilter.CreatureInYourGraveyard),
        )
        val mount = target(
            "Mount card",
            TargetObject(
                optional = true,
                filter = TargetFilter(
                    GameObjectFilter.Any.withSubtype(Subtype("Mount")).ownedByYou(),
                    zone = Zone.GRAVEYARD,
                ),
            ),
        )
        val vehicle = target(
            "Vehicle card",
            TargetObject(
                optional = true,
                filter = TargetFilter(
                    GameObjectFilter.Any.withSubtype(Subtype.VEHICLE).ownedByYou(),
                    zone = Zone.GRAVEYARD,
                ),
            ),
        )
        val noAbilities = target(
            "creature card with no abilities",
            TargetObject(
                optional = true,
                filter = TargetFilter(
                    Filters.CreatureWithNoAbilities.ownedByYou(),
                    zone = Zone.GRAVEYARD,
                ),
            ),
        )
        effect = Effects.Composite(
            Effects.ReturnToHand(creature),
            Effects.ReturnToHand(mount),
            Effects.ReturnToHand(vehicle),
            Effects.ReturnToHand(noAbilities),
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "178"
        artist = "Nino Is"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43e6ac32-a7a4-4c15-81b0-8485d6a0e7ca.jpg?1783907866"
    }
}
