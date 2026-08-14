package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Honeymoon Hearse
 * {2}{R}
 * Artifact — Vehicle
 * 5/5
 *
 * Trample
 * Tap two untapped creatures you control: This Vehicle becomes an artifact creature until end of turn.
 *
 * A Vehicle with no Crew keyword: the animate ability is written out longhand instead, so the cost is
 * a flat "tap two creatures" rather than crew's total-power threshold. The effect is the same shape
 * the engine builds for crew — [Effects.BecomeCreature] on self with the printed base P/T until end
 * of turn — and no type needs adding, since a Vehicle is already an artifact. Trample is printed on
 * the card, so it applies for free once the Hearse is a creature.
 */
val HoneymoonHearse = card("Honeymoon Hearse") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Vehicle"
    power = 5
    toughness = 5
    oracleText = "Trample\n" +
        "Tap two untapped creatures you control: This Vehicle becomes an artifact creature until end of turn."

    keywords(Keyword.TRAMPLE)

    activatedAbility {
        cost = Costs.TapPermanents(count = 2, filter = GameObjectFilter.Creature.youControl())
        effect = Effects.BecomeCreature(EffectTarget.Self, power = 5, toughness = 5)
        description = "This Vehicle becomes an artifact creature until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Raoul Vitale"
        flavorText = "A carriage crafted from the finest materials. Horses bred from the finest stock. " +
            "Skulls taken from the finest foes."
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04e491a1-b195-45bb-bf06-345eaee30b81.jpg?1783924835"
    }
}
