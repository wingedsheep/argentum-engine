package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thopter Mechanic
 * {1}{U}
 * Creature — Human Artificer
 * 2/1
 * Whenever you draw your second card each turn, put a +1/+1 counter on this creature.
 * When this creature dies, create a 1/1 colorless Thopter artifact creature token with flying.
 */
val ThopterMechanic = card("Thopter Mechanic") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Artificer"
    power = 2
    toughness = 1
    oracleText = "Whenever you draw your second card each turn, put a +1/+1 counter on this creature.\n" +
        "When this creature dies, create a 1/1 colorless Thopter artifact creature token with flying."

    triggeredAbility {
        trigger = Triggers.you.drawsNth(2)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "68"
        artist = "Joshua Raphael"
        flavorText = "\"You want me to install a seat belt? Please. You're lucky to get a seat!\""
        imageUri = "https://cards.scryfall.io/normal/front/7/5/7577b5c7-91ed-4201-bdfd-0919142124eb.jpg"
    }
}
