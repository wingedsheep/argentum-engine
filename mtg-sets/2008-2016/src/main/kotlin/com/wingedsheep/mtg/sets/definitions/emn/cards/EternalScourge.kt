package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eternal Scourge
 * {3}
 * Creature — Eldrazi Horror
 * 3/3
 * You may cast this card from exile.
 * When this creature becomes the target of a spell or ability an opponent controls, exile this creature.
 */
val EternalScourge = card("Eternal Scourge") {
    manaCost = "{3}"
    typeLine = "Creature — Eldrazi Horror"
    power = 3
    toughness = 3
    oracleText = "You may cast this card from exile.\n" +
        "When this creature becomes the target of a spell or ability an opponent controls, exile this creature."

    staticAbility {
        ability = MayCastSelfFromZones(zones = listOf(Zone.EXILE))
    }

    triggeredAbility {
        trigger = Triggers.self.becomesTarget(byOpponent = true)
        effect = Effects.Exile(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Winona Nelson"
        flavorText = "\"It has latched onto this place like an infestation. All efforts at eradication have proved futile.\"\n—Yeter, Trostad villager"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13ce52f5-6d49-4d44-a3d7-925340de8406.jpg?1783937526"
        ruling("2016-07-13", "You must follow the normal timing permissions and restrictions and pay its cost to cast Eternal Scourge from exile.")
        ruling("2016-07-13", "Eternal Scourge's triggered ability only triggers while it's on the battlefield. For example, Convolute cast targeting Eternal Scourge won't cause its ability to trigger.")
        ruling("2016-07-13", "You'll exile Eternal Scourge even if the spell or ability that targets it is countered.")
    }
}
