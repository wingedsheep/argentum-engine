package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.YouAttackedThisTurn
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Timely Hordemate
 * {3}{W}
 * Creature — Human Warrior
 * 3/2
 * Raid — When Timely Hordemate enters, if you attacked this turn, return target creature card
 * with mana value 2 or less from your graveyard to the battlefield.
 */
val TimelyHordemate = card("Timely Hordemate") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 2
    oracleText = "Raid — When Timely Hordemate enters, if you attacked this turn, return target creature card with mana value 2 or less from your graveyard to the battlefield."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target(
            "target", TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.Creature.manaValueAtMost(2).ownedByYou(),
                    zone = Zone.GRAVEYARD
                )
            )
        )
        effect = ConditionalEffect(
            condition = YouAttackedThisTurn,
            effect = MoveToZoneEffect(t, Zone.BATTLEFIELD)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dcb3c08e-b591-421a-898a-533021cbabd2.jpg?1562794596"
    }
}
