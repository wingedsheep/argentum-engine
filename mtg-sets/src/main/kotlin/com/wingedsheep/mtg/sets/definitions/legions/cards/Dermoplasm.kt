package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dermoplasm
 * {2}{U}
 * Creature — Shapeshifter
 * 1/1
 * Flying
 * Morph {2}{U}{U}
 * When this creature is turned face up, you may put a creature card with a morph
 * ability from your hand onto the battlefield face up. If you do, return this
 * creature to its owner's hand.
 */
val Dermoplasm = card("Dermoplasm") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Shapeshifter"
    power = 1
    toughness = 1
    oracleText = "Flying\nMorph {2}{U}{U} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, you may put a creature card with a morph ability from your hand onto the battlefield face up. If you do, return this creature to its owner's hand."

    keywords(Keyword.FLYING)
    morph = "{2}{U}{U}"

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = ReflexiveTriggerEffect(
            action = EffectPatterns.putFromHand(
                filter = GameObjectFilter.Creature.withMorph()
            ),
            optional = true,
            reflexiveEffect = MoveToZoneEffect(EffectTarget.Self, Zone.HAND)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "35"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf2f5dca-e01f-41e3-bb6f-a60162118c6d.jpg?1562936638"
        ruling("2004-10-04", "The trigger occurs when you use the Morph ability to turn the card face up, or when an effect turns it face up. It will not trigger on being revealed or on leaving the battlefield.")
    }
}
