package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Reality Smasher
 * {4}{C}
 * Creature — Eldrazi
 * 5/5
 * ({C} represents colorless mana.)
 * Trample, haste
 * Whenever this creature becomes the target of a spell an opponent controls, counter that spell
 * unless its controller discards a card.
 *
 * The trigger is the printed form of Ward—Discard a card narrowed to opponents' *spells*
 * (abilities don't trigger it): `becomesTarget(byOpponent, spellsOnly)` paired with the ward
 * counter aimed at the targeting spell, so an empty hand can't pay and the spell is countered.
 */
val RealitySmasher = card("Reality Smasher") {
    manaCost = "{4}{C}"
    colorIdentity = ""
    typeLine = "Creature — Eldrazi"
    power = 5
    toughness = 5
    oracleText = "({C} represents colorless mana.)\n" +
        "Trample, haste\n" +
        "Whenever this creature becomes the target of a spell an opponent controls, counter that spell " +
        "unless its controller discards a card."

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.becomesTarget(byOpponent = true, spellsOnly = true)
        effect = Effects.CounterTargetingSourceUnlessPays(WardCost.Discard())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Jason Rainville"
        flavorText = "As easy to stop as it is to comprehend."
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52d4b652-a830-4fd4-94bb-c17c227f2928.jpg?1783937929"
        ruling(
            "2016-01-22",
            "The last ability will trigger due to any spell controlled by an opponent (including an Aura spell) that targets Reality Smasher."
        )
    }
}
