package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val Sandblast = card("Sandblast") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Sandblast deals 5 damage to target attacking or blocking creature."

    spell {
        val t = target(TargetFilter.AttackingOrBlockingCreature)
        effect = Effects.DealDamage(5, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Min Yum"
        flavorText = "In the Shifting Wastes, more are killed by sand than by steel."
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7305cd4-a9f1-4801-bdfe-b07869ed5b56.jpg?1783938710"
    }
}
