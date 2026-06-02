package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Grumgully, the Generous {1}{R}{G}
 * Legendary Creature — Goblin Shaman
 * 3/3
 *
 * Each other non-Human creature you control enters with an additional
 * +1/+1 counter on it.
 */
val GrumgullyTheGenerous = card("Grumgully, the Generous") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Goblin Shaman"
    power = 3
    toughness = 3
    oracleText = "Each other non-Human creature you control enters with an additional " +
        "+1/+1 counter on it."

    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmount.Fixed(1),
            otherOnly = true,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().notSubtype(Subtype.HUMAN),
                to = Zone.BATTLEFIELD,
            ),
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "253"
        artist = "Milivoj Ćeran"
        flavorText = "\"Does it matter what it is? Take it and be grateful!\""
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8d7ac51-450e-473e-93e1-3fa9b3b6e2d4.jpg?1721429470"
        ruling("2019-10-04", "A non-Human creature you control that would enter the battlefield with no +1/+1 counters on it enters with one +1/+1 counter on it.")
        ruling("2019-10-04", "Any other non-Human creatures that enter the battlefield at the same time as Grumgully won't get a +1/+1 counter.")
        ruling("2019-10-04", "If a non-Human creature enters the battlefield as a copy of a Human creature, it won't get a +1/+1 counter. Similarly, if a Human enters as a non-Human creature, it will get a +1/+1 counter.")
    }
}
