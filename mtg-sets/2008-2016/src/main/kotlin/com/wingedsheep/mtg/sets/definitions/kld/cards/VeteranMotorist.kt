package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** Veteran Motorist — Kaladesh #188. */
val VeteranMotorist = card("Veteran Motorist") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Dwarf Pilot"
    power = 3
    toughness = 1
    oracleText = "When this creature enters, scry 2.\nWhenever this creature crews a Vehicle, that Vehicle gets +1/+1 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Scry(2)
    }

    triggeredAbility {
        trigger = Triggers.self.crews()
        effect = Effects.ModifyStats(1, 1, EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "188"
        artist = "Christopher Moeller"
        flavorText = "He's set to compete in every race at the Fair, and he plans to win them all."
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f5be9c1-cb28-42bd-b159-5548124ba8d1.jpg?1783937165"
        ruling("2016-09-20", "Veteran Motorist's last ability triggers when it becomes tapped to activate a crew ability. Even if Veteran Motorist leaves the battlefield before its triggered ability resolves, that Vehicle still gets +1/+1 until end of turn.")
        ruling("2016-09-20", "Veteran Motorist's last ability resolves before the crew ability that it was tapped to activate. The +1/+1 bonus will be in effect as the Vehicle becomes an artifact creature.")
    }
}
