package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.beamMeUp
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Pure Energy
 * {1}{W}
 * Sorcery
 * Distribute two +1/+1 counters among one or two target creatures.
 * Beam me up {2}{W} (You may cast this card from your graveyard for {2}{W} if you also return a
 * creature you control to its owner's hand. Then exile this spell.)
 */
val PureEnergy = card("Pure Energy") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Distribute two +1/+1 counters among one or two target creatures.\n" +
        "Beam me up {2}{W} (You may cast this card from your graveyard for {2}{W} if you also " +
        "return a creature you control to its owner's hand. Then exile this spell.)"
    beamMeUp("{2}{W}")
    spell {
        target = TargetObject(filter = TargetFilter.Creature, count = 2, minCount = 1)
        effect = Effects.DistributeCountersAmongTargets(2, CounterType.PLUS_ONE_PLUS_ONE)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Evan Shipard"
        flavorText = "So advanced in thought and being, the Organians have no need for corporeal form."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/215709a8-044f-4fdb-b3ca-bd5a394b2a0f.jpg?1793361211"
    }
}
