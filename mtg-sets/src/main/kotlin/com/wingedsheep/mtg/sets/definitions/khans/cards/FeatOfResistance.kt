package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Feat of Resistance
 * {1}{W}
 * Instant
 * Put a +1/+1 counter on target creature you control. It gains protection from the color
 * of your choice until end of turn.
 */
val FeatOfResistance = card("Feat of Resistance") {
    manaCost = "{1}{W}"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter on target creature you control. It gains protection from the color of your choice until end of turn."

    spell {
        val t = target("target", Targets.CreatureYouControl)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, t)
            .then(Effects.ChooseColorAndGrantProtectionToTarget(t))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "David Gaillet"
        flavorText = "Dragons are extinct on Tarkir, but Abzan magic still emulates their endurance."
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6173466-30db-4b95-a556-dd69e03b731e.jpg?1562792351"
    }
}
