package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Riptide Survivor
 * {2}{U}
 * Creature — Human Wizard
 * 2/1
 * Morph {1}{U}{U}
 * When Riptide Survivor is turned face up, discard two cards, then draw three cards.
 */
val RiptideSurvivor = card("Riptide Survivor") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 1
    oracleText = "Morph {1}{U}{U}\nWhen Riptide Survivor is turned face up, discard two cards, then draw three cards."

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = Effects.Discard(2, EffectTarget.Controller)
            .then(Effects.DrawCards(3))
    }

    morph = "{1}{U}{U}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "45"
        artist = "Glen Angus"
        flavorText = "Rootwater showed him wonders that air dwellers could scarcely comprehend."
        imageUri = "https://cards.scryfall.io/large/front/7/5/7515187f-4821-400d-b78f-cec173df6b84.jpg?1562530669"
    }
}
