package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Neutralizing Blast
 * {1}{U}
 * Instant
 * Counter target multicolored spell.
 */
val NeutralizingBlast = card("Neutralizing Blast") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target multicolored spell."

    spell {
        target(TargetFilter(GameObjectFilter.Multicolored, zone = Zone.STACK))
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "44"
        artist = "Dan Murayama Scott"
        flavorText = "\"To defeat your enemies, you must remove their ability to make war against you.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e549a8fc-6001-43db-88b1-ce8ed42a3443.jpg?1783938704"
    }
}
