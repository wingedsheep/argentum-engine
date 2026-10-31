package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tuinvale Treefolk // Oaken Boon
 * {5}{G}
 * Creature — Treefolk Druid
 * 6/5
 *
 * Adventure: Oaken Boon — {3}{G}, Sorcery — Adventure
 * Put two +1/+1 counters on target creature.
 */
val TuinvaleTreefolk = card("Tuinvale Treefolk") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Treefolk Druid"
    power = 6
    toughness = 5
    oracleText = ""

    adventure("Oaken Boon") {
        manaCost = "{3}{G}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Put two +1/+1 counters on target creature. (Then exile this card. You may cast " +
            "the creature later from exile.)"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "180"
        artist = "Jason A. Engle"
        flavorText = "\"Right now you are a feeble stick, but I will help you grow some rings.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8bc518fc-904e-4e39-aeda-ffb222bfcc82.jpg?1783932601"
    }
}
