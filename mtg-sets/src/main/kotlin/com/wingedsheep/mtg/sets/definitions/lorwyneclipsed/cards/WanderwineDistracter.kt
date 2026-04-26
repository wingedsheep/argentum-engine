package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val WanderwineDistracter = card("Wanderwine Distracter") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Merfolk Wizard"
    power = 4
    toughness = 3
    oracleText = "Whenever this creature becomes tapped, target creature an opponent controls gets -3/-0 until end of turn."

    triggeredAbility {
        trigger = Triggers.BecomesTapped
        val victim = target("creature", Targets.CreatureOpponentControls)
        effect = Effects.ModifyStats(-3, 0, victim)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Warren Mahy"
        flavorText = "Merrow glamers have disarmed many a foe when a shield has failed."
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbf593a7-d4ae-4771-926a-3c1b2c8c901a.jpg?1767732580"
    }
}
