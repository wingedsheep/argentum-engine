package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Inspired Inventor
 * {2}{W}
 * Creature — Human Artificer
 * 2/2
 *
 * When this creature enters, choose one —
 * • You get {E}{E}{E} (three energy counters).
 * • Put a +1/+1 counter on target creature.
 * • Create a 1/1 colorless Servo artifact creature token.
 *
 * Only the counter mode targets, so the target is chosen only when that mode is picked (CR 700.2c).
 */
val InspiredInventor = card("Inspired Inventor") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Artificer"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, choose one —\n" +
        "• You get {E}{E}{E} (three energy counters).\n" +
        "• Put a +1/+1 counter on target creature.\n" +
        "• Create a 1/1 colorless Servo artifact creature token."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("You get three energy counters") {
                effect = Effects.GetEnergy(3)
            },
            mode("Put a +1/+1 counter on target creature") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
            },
            mode("Create a 1/1 colorless Servo artifact creature token") {
                effect = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    creatureTypes = setOf("Servo"),
                    artifactToken = true,
                    imageUri = "https://cards.scryfall.io/normal/front/2/3/23165814-3eb8-4d94-b9c8-ecc798afe7a0.jpg?1783911109"
                )
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Ryan Valle"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/9143fc7c-f4eb-4f06-97b9-f912237a055f.jpg?1783911300"
    }
}
