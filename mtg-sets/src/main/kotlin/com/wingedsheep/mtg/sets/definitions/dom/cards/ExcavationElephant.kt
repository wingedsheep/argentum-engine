package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.Effects

/**
 * Excavation Elephant
 * {4}{W}
 * Creature — Elephant
 * 3/5
 * Kicker {1}{W}
 * When this creature enters, if it was kicked, return target artifact card
 * from your graveyard to your hand.
 */
val ExcavationElephant = card("Excavation Elephant") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elephant"
    power = 3
    toughness = 5
    oracleText = "Kicker {1}{W}\nWhen this creature enters, if it was kicked, return target artifact card from your graveyard to your hand."

    keywordAbility(KeywordAbility.kicker("{1}{W}"))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        val t = target("target", TargetObject(
            filter = TargetFilter(
                GameObjectFilter.Artifact.ownedByYou(),
                zone = Zone.GRAVEYARD
            )
        ))
        effect = Effects.Move(
            target = t,
            destination = Zone.HAND
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Viktor Titov"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/760ce08a-49d3-4cdf-bbe2-33dd8a6a7966.jpg?1562737910"
    }
}
