package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.UntapLimitPerStep

// A zero untap cap, read at the untap step over final projected power (so pumps and
// P/T-setting effects count), rather than a layer-6 DOESNT_UNTAP group grant.
val Meekstone = card("Meekstone") {
    manaCost = "{1}"
    typeLine = "Artifact"
    oracleText = "Creatures with power 3 or greater don't untap during their controllers' untap steps."

    staticAbility {
        ability = UntapLimitPerStep(GameObjectFilter.Creature.powerAtLeast(3), 0)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "260"
        artist = "Quinton Hoover"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13a68a17-22ee-47c9-870a-83e911862b94.jpg?1783948664"
    }
}
