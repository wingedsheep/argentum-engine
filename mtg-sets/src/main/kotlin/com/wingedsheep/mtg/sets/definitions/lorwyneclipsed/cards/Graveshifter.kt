package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Graveshifter
 * {3}{B}
 * Creature — Shapeshifter
 * 2/2
 *
 * Changeling (This card is every creature type.)
 * When this creature enters, you may return target creature card from your graveyard to your hand.
 */
val Graveshifter = card("Graveshifter") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Shapeshifter"
    power = 2
    toughness = 2
    oracleText = "Changeling (This card is every creature type.)\n" +
        "When this creature enters, you may return target creature card from your graveyard to your hand."

    keywords(Keyword.CHANGELING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        optional = true
        val creature = target(
            "target creature card from your graveyard",
            TargetObject(filter = TargetFilter.CreatureInYourGraveyard)
        )
        effect = MoveToZoneEffect(creature, Zone.HAND)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "104"
        artist = "Deborah Garcia"
        flavorText = "We'll see a changeling at their funeral\n—Kithkin saying meaning \"they won't be forgotten\""
        imageUri = "https://cards.scryfall.io/normal/front/d/a/dadb02b9-d3a0-4b51-bbc3-53b2316cd70d.jpg?1767873656"
    }
}
