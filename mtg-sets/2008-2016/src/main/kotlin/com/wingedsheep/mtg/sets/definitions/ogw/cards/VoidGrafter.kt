package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Void Grafter
 * {1}{G}{U}
 * Creature — Eldrazi Drone
 * 2/4
 * Devoid (This card has no color.)
 * Flash (You may cast this spell any time you could cast an instant.)
 * When this creature enters, another target creature you control gains hexproof until end of turn.
 */
val VoidGrafter = card("Void Grafter") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Eldrazi Drone"
    power = 2
    toughness = 4
    oracleText = "Devoid (This card has no color.)\n" +
        "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "When this creature enters, another target creature you control gains hexproof until end of turn."

    keywords(Keyword.DEVOID, Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.GrantKeyword(Keyword.HEXPROOF, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "150"
        artist = "Alejandro Mirabal"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c2e37e2-a4bd-4f90-9191-3125784a3369.jpg"
    }
}
