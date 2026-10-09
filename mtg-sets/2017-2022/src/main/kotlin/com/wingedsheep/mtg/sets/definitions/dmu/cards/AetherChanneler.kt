package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val AetherChanneler = card("Aether Channeler") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, choose one —\n" +
        "• Create a 1/1 white Bird creature token with flying.\n" +
        "• Return another target nonland permanent to its owner's hand.\n" +
        "• Draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Modal(modes = listOf(
            Mode.noTarget(
                Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Bird"),
                    keywords = setOf(Keyword.FLYING),
                    imageUri = "https://cards.scryfall.io/normal/front/5/f/5f3034f6-145f-4e60-9e55-c4054fd8e70f.jpg?1783921132"
                ),
                "Create a 1/1 white Bird creature token with flying"
            ),
            mode("Return another target nonland permanent to its owner's hand") {
                val permanent = target(TargetFilter.OtherNonlandPermanent)
                effect = Effects.ReturnToHand(permanent)
            },
            Mode.noTarget(Effects.DrawCards(1), "Draw a card")
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60afeb75-2c1e-4634-8c83-88b1dddb77c2.jpg?1783921355"
    }
}
