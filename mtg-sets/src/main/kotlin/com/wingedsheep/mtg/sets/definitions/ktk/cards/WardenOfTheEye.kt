package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.Effects

/**
 * Warden of the Eye
 * {2}{U}{R}{W}
 * Creature — Djinn Wizard
 * 3/3
 * When this creature enters, return target noncreature, nonland card from your graveyard to your hand.
 */
val WardenOfTheEye = card("Warden of the Eye") {
    manaCost = "{2}{U}{R}{W}"
    colorIdentity = "WUR"
    typeLine = "Creature — Djinn Wizard"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, return target noncreature, nonland card from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target(
            "target", TargetObject(
                filter = TargetFilter(
                    baseFilter = (GameObjectFilter.Companion.Noncreature and GameObjectFilter.Companion.Nonland).ownedByYou(),
                    zone = Zone.GRAVEYARD
                )
            )
        )
        effect = Effects.Move(t, Zone.HAND)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "212"
        artist = "Howard Lyon"
        flavorText = "The wardens guard the sacred documents of Tarkir's history, though they are forbidden to read the words."
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04135bf7-2bcf-4a92-80f0-6d5eefca551b.jpg?1562781945"
    }
}
