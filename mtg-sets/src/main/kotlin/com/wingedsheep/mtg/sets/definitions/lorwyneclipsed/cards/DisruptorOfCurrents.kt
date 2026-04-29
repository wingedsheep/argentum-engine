package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Disruptor of Currents
 * {3}{U}{U}
 * Creature — Merfolk Wizard
 * 3/3
 *
 * Flash
 * Convoke
 * When this creature enters, return up to one other target nonland permanent to its owner's hand.
 */
val DisruptorOfCurrents = card("Disruptor of Currents") {
    manaCost = "{3}{U}{U}"
    typeLine = "Creature — Merfolk Wizard"
    power = 3
    toughness = 3
    oracleText = "Flash\n" +
        "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "When this creature enters, return up to one other target nonland permanent to its owner's hand."

    keywords(Keyword.FLASH, Keyword.CONVOKE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val permanent = target(
            "up to one other target nonland permanent",
            TargetPermanent(
                optional = true,
                filter = TargetFilter.NonlandPermanent.copy(excludeSelf = true)
            )
        )
        effect = Effects.ReturnToHand(permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "47"
        artist = "Pauline Voss"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c6dbbaa-6844-4d7c-abbb-472a83bb99ab.jpg?1767659142"
    }
}
