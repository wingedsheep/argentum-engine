package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyDrawAmount
import com.wingedsheep.sdk.scripting.ModifyLifeGain
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Phial of Galadriel
 * {3}
 * Legendary Artifact
 *
 * If you would draw a card while you have no cards in hand, draw two cards instead.
 * If you would gain life while you have 5 or less life, you gain twice that much life instead.
 * {T}: Add one mana of any color.
 *
 * The two replacements compose from existing primitives: `ModifyDrawAmount(+1)` gated on an empty
 * hand, and `ModifyLifeGain(×2)` gated on the new `restrictions` field (life ≤ 5).
 */
val PhialOfGaladriel = card("Phial of Galadriel") {
    manaCost = "{3}"
    typeLine = "Legendary Artifact"
    oracleText = "If you would draw a card while you have no cards in hand, draw two cards instead.\n" +
        "If you would gain life while you have 5 or less life, you gain twice that much life instead.\n" +
        "{T}: Add one mana of any color."

    replacementEffect(
        ModifyDrawAmount(
            modifier = 1,
            restrictions = listOf(Conditions.CardsInHandAtMost(0)),
            appliesTo = EventPattern.DrawEvent(player = Player.You),
        )
    )

    replacementEffect(
        ModifyLifeGain(
            multiplier = 2,
            appliesTo = EventPattern.LifeGainEvent(player = Player.You),
            restrictions = listOf(Conditions.LifeAtMost(5)),
        )
    )

    activatedAbility {
        cost = Costs.Tap
        effect = AddManaOfChoiceEffect(
            colorSet = ManaColorSet.AnyColor,
            amount = DynamicAmount.Fixed(1),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "248"
        artist = "Andrea Piparo"
        flavorText = "\"May it be a light to you in dark places, when all other lights go out.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac6d60fe-681b-495e-813c-c8418f3f29e5.jpg?1686970260"
    }
}
