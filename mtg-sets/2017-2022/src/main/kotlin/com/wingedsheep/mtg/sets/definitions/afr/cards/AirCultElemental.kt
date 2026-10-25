package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Air-Cult Elemental
 * {4}{U}{U}
 * Creature — Elemental
 * 2/5
 * Flying
 * Whirlwind — When this creature enters, return up to one other target creature to its owner's hand.
 */
val AirCultElemental = card("Air-Cult Elemental") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    oracleText = "Flying\nWhirlwind — When this creature enters, return up to one other target creature to its owner's hand."
    power = 2
    toughness = 5

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.OtherCreature, optional = true)
        effect = Effects.ReturnToHand(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Kari Christensen"
        flavorText = "Elemental Evil distorts the natural elements, limiting them to aspects of destruction."
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fba82564-a9a9-4ded-a5fa-0e5fab2a3faa.jpg?1783926520"
    }
}
