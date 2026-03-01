package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Willbender
 * {1}{U}
 * Creature — Human Wizard
 * 1/2
 * Morph {1}{U} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)
 * When this creature is turned face up, change the target of target spell or ability with a single target.
 */
val Willbender = card("Willbender") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 2
    oracleText = "Morph {1}{U} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, change the target of target spell or ability with a single target."

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        target = Targets.SpellOrAbilityWithSingleTarget
        effect = Effects.ChangeTarget()
    }

    morph = "{1}{U}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "58"
        artist = "Eric Peterson"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb33b35b-33c9-4d59-9ed6-7ad40ea82cb0.jpg?1562945680"
        ruling("2004-10-04", "The trigger activates when using the Morph ability to flip the card face up or when an effect flips it. Revealing the card or leaving the battlefield does not trigger it.")
        ruling("2013-04-15", "If a spell with split second is on the stack, you can still respond by turning this creature face up and targeting that spell with the trigger.")
        ruling("2018-03-16", "You don't choose the new target for the spell until Willbender's triggered ability resolves.")
    }
}
