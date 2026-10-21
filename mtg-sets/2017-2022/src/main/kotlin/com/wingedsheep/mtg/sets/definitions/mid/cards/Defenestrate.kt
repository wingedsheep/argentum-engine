package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Defenestrate
 * {2}{B}
 * Instant
 * Destroy target creature without flying.
 */
val Defenestrate = card("Defenestrate") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature without flying."
    spell {
        val t = target(TargetFilter.Creature.withoutKeyword(Keyword.FLYING))
        effect = Effects.Move(t, Zone.GRAVEYARD, byDestruction = true)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "Darek Zabrocki"
        flavorText = "The chaplain would perhaps have found some small comfort had he known that the replacement window would be named in his honor."
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df3abdcc-83a8-45c3-9bfd-23f929705018.jpg"
    }
}
