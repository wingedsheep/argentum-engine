package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec

/**
 * Rotlung Reanimator
 * {2}{B}
 * Creature — Zombie Cleric
 * 2/2
 * Whenever Rotlung Reanimator or another Cleric dies, create a 2/2 black Zombie creature token.
 */
val RotlungReanimator = card("Rotlung Reanimator") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Zombie Cleric"
    power = 2
    toughness = 2
    oracleText = "Whenever Rotlung Reanimator or another Cleric dies, create a 2/2 black Zombie creature token."

    val zombieToken = CreateTokenEffect(
        count = 1,
        power = 2,
        toughness = 2,
        colors = setOf(Color.BLACK),
        creatureTypes = setOf("Zombie"),
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17f001ab-514b-49e7-a657-b2872ad7a1de.jpg?1767954964"
    )

    // When Rotlung Reanimator itself dies
    triggeredAbility {
        trigger = Triggers.Dies
        effect = zombieToken
    }

    // When another Cleric dies (any controller, not just yours)
    triggeredAbility {
        trigger = TriggerSpec(
                ZoneChangeEvent(
                    filter = GameObjectFilter.Creature.withSubtype(Subtype("Cleric")),
                    from = Zone.BATTLEFIELD,
                    to = Zone.GRAVEYARD
                ),
                TriggerBinding.OTHER
            )
        effect = zombieToken
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "164"
        artist = "Thomas M. Baxa"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87b29d1e-9c06-4ad1-8178-b3eaa212f6f1.jpg?1562927028"
    }
}
