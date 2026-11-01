package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Planewide Celebration — War of the Spark #172
 * {5}{G}{G}
 * Sorcery
 * Choose four. You may choose the same mode more than once.
 * • Create a 2/2 Citizen creature token that's all colors.
 * • Return target permanent card from your graveyard to your hand.
 * • Proliferate.
 * • You gain 4 life.
 *
 * "Choose four" with repetition = `chooseCount = 4`, `minChooseCount = 4`, `allowRepeat = true`
 * (the Cosmium Confluence shape). Each instance of the return mode announces its own target.
 */
val PlanewideCelebration = card("Planewide Celebration") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose four. You may choose the same mode more than once.\n" +
        "• Create a 2/2 Citizen creature token that's all colors.\n" +
        "• Return target permanent card from your graveyard to your hand.\n" +
        "• Proliferate.\n" +
        "• You gain 4 life."

    spell {
        modal(chooseCount = 4, minChooseCount = 4, allowRepeat = true) {
            mode("Create a 2/2 Citizen creature token that's all colors") {
                effect = Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = Color.entries.toSet(),
                    creatureTypes = setOf("Citizen"),
                )
            }
            mode("Return target permanent card from your graveyard to your hand") {
                val card = target(TargetFilter.PermanentInYourGraveyard)
                effect = Effects.ReturnToHand(card)
            }
            mode("Proliferate") {
                effect = Effects.Proliferate()
            }
            mode("You gain 4 life") {
                effect = Effects.GainLife(4)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "172"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11016840-56b6-4759-bde6-3dc0e339e87e.jpg?1783933409"
    }
}
