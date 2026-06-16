package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Treason of Isengard
 * {2}{U}
 * Sorcery
 *
 * Put up to one target instant or sorcery card from your graveyard on top of your library.
 * Amass Orcs 2.
 */
val TreasonOfIsengard = card("Treason of Isengard") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Put up to one target instant or sorcery card from your graveyard on top of your library.\nAmass Orcs 2. (To amass Orcs 2, put two +1/+1 counters on an Army you control. It's also an Orc. If you don't control an Army, create a 0/0 black Orc Army creature token first.)"

    spell {
        val card = target(
            "up to one target instant or sorcery card from your graveyard",
            TargetObject(
                filter = TargetFilter(GameObjectFilter.InstantOrSorcery.ownedByYou(), zone = Zone.GRAVEYARD),
                optional = true
            )
        )
        effect = Effects.PutOnTopOfLibrary(card)
            .then(Effects.Amass(2, "Orc"))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Pavel Kolomeyets"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71f97505-c961-4890-acd0-32a63919ac2a.jpg?1686968343"
    }
}
