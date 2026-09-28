package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Infected Defector
 * {4}{W}
 * Creature — Phyrexian Knight
 * 4/3
 *
 * When this creature dies, incubate 3. (Create an Incubator token with three +1/+1 counters on it
 * and "{2}: Transform this token." It transforms into a 0/0 Phyrexian artifact creature.)
 */
val InfectedDefector = card("Infected Defector") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Phyrexian Knight"
    oracleText = "When this creature dies, incubate 3. (Create an Incubator token with three +1/+1 counters " +
        "on it and \"{2}: Transform this token.\" It transforms into a 0/0 Phyrexian artifact creature.)"
    power = 4
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Incubate(3)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "18"
        artist = "Nino Vecia"
        flavorText = "The diamond storms of Gobakhan hampered Phyrexia's progress until they recruited some of the local shieldmages."
        imageUri = "https://cards.scryfall.io/normal/front/2/7/2777f8b5-2f8e-4cb8-9206-8a4978488657.jpg?1783917062"
    }
}
