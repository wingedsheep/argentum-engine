package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Getaway Car
 * {3}
 * Artifact — Vehicle
 * 4/3
 * Haste
 * Whenever this Vehicle attacks or blocks, return up to one target creature that crewed it this
 * turn to its owner's hand.
 * Crew 1
 *
 * "Attacks or blocks" is two triggered abilities sharing one effect (the Merfolk Skyscout shape).
 * The target is restricted to this Vehicle's crewers via the source-relative
 * `crewedOrSaddledSourceThisTurn` filter (the Turtle Van / Subterranean Schooner shape).
 */
val GetawayCar = card("Getaway Car") {
    manaCost = "{3}"
    typeLine = "Artifact — Vehicle"
    oracleText = "Haste\n" +
        "Whenever this Vehicle attacks or blocks, return up to one target creature that crewed it this turn to its owner's hand.\n" +
        "Crew 1 (Tap any number of creatures you control with total power 1 or more: This Vehicle becomes an artifact creature until end of turn.)"
    power = 4
    toughness = 3

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val crewer = target(TargetFilter(GameObjectFilter.Creature.crewedOrSaddledSourceThisTurn()), optional = true)
        effect = Effects.ReturnToHand(crewer)
        description = "Whenever this Vehicle attacks, return up to one target creature that crewed it this turn to its owner's hand."
    }

    triggeredAbility {
        trigger = Triggers.self.blocks()
        val crewer = target(TargetFilter(GameObjectFilter.Creature.crewedOrSaddledSourceThisTurn()), optional = true)
        effect = Effects.ReturnToHand(crewer)
        description = "Whenever this Vehicle blocks, return up to one target creature that crewed it this turn to its owner's hand."
    }

    keywordAbility(KeywordAbility.crew(1))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "237"
        artist = "Donato Giancola"
        flavorText = "For those who need to make a quick departure without attracting undue attention."
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a6d2fa3-3f5a-4b6f-8b7e-6a8bf65c07ad.jpg?1783923063"
    }
}
