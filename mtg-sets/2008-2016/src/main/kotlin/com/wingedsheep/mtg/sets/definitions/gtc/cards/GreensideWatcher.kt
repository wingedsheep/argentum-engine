package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Greenside Watcher
 * {1}{G}
 * Creature — Elf Druid
 * 2/1
 * {T}: Untap target Gate.
 */
val GreensideWatcher = card("Greenside Watcher") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 2
    toughness = 1
    oracleText = "{T}: Untap target Gate."

    activatedAbility {
        cost = Costs.Tap
        val t = target(TargetFilter.Permanent.withSubtype(Subtype.GATE))
        effect = Effects.Untap(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "122"
        artist = "Ryan Barger"
        flavorText = "\"The Izzet may incite war, but my duty is here. I will not abandon the wild.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e825cb2d-98d7-423d-9ba1-b4d04027027e.jpg?1783940118"
    }
}
