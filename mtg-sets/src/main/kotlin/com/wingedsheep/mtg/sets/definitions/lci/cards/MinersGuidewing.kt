package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Miner's Guidewing — {W}
 * Creature — Bird
 * 1/1
 * Flying, vigilance
 * When this creature dies, target creature you control explores.
 */
val MinersGuidewing = card("Miner's Guidewing") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird"
    oracleText = "Flying, vigilance\nWhen this creature dies, target creature you control explores. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on that creature, then put the card back or put it into your graveyard.)"
    power = 1
    toughness = 1

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.Dies
        val t = target("target creature you control", TargetCreature(filter = TargetFilter.CreatureYouControl))
        effect = Effects.Explore(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Allen Douglas"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/9048cd9d-df3f-4705-a5f4-e5b09760c631.jpg?1782694591"
    }
}
