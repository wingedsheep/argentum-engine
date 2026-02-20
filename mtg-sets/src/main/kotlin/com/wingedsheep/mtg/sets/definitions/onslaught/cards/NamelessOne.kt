package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Nameless One
 * {3}{U}
 * Creature — Wizard Avatar
 * *|*
 * Nameless One's power and toughness are each equal to the number of Wizards on the battlefield.
 * Morph {2}{U}
 */
val NamelessOne = card("Nameless One") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Wizard Avatar"
    oracleText = "Nameless One's power and toughness are each equal to the number of Wizards on the battlefield.\nMorph {2}{U}"

    dynamicStats(DynamicAmount.AggregateBattlefield(Player.Each, GameObjectFilter.Creature.withSubtype("Wizard")))

    morph = "{2}{U}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "100"
        artist = "Mark Tedin"
        flavorText = "To study magic is to walk the path of the Nameless."
        imageUri = "https://cards.scryfall.io/large/front/7/9/79cf3535-3f80-4b76-aad3-dd851e6885a6.jpg?1562923715"
    }
}
