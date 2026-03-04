package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.TurnFaceDownEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Weaver of Lies
 * {5}{U}{U}
 * Creature — Beast
 * 4/4
 * Morph {4}{U} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)
 * When this creature is turned face up, turn any number of target creatures with morph abilities other than
 * this creature face down.
 */
val WeaverOfLies = card("Weaver of Lies") {
    manaCost = "{5}{U}{U}"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 4
    oracleText = "Morph {4}{U} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen this creature is turned face up, turn any number of target creatures with morph abilities other than this creature face down."

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        val t = target("creatures with morph abilities other than this creature", TargetPermanent(
            count = 20,
            optional = true,
            filter = TargetFilter(GameObjectFilter.Creature.withMorph().faceUp()).other()
        ))
        effect = ForEachTargetEffect(
            effects = listOf(TurnFaceDownEffect(EffectTarget.ContextTarget(0)))
        )
    }

    morph = "{4}{U}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "57"
        artist = "Luca Zontini"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/12172d0e-0c73-4482-9f83-2c23ace9b7a0.jpg?1562898647"
        ruling("2004-10-04", "The trigger occurs when you use the Morph ability to turn the card face up, or when an effect turns it face up. It will not trigger on being revealed or on leaving the battlefield.")
    }
}
