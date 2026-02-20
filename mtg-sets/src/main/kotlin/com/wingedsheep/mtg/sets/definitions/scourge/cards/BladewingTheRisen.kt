package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Bladewing the Risen
 * {3}{B}{B}{R}{R}
 * Legendary Creature — Zombie Dragon
 * 4/4
 * Flying
 * When Bladewing the Risen enters the battlefield, you may return target Dragon
 * permanent card from your graveyard to the battlefield.
 * {B}{R}: Dragon creatures get +1/+1 until end of turn.
 */
val BladewingTheRisen = card("Bladewing the Risen") {
    manaCost = "{3}{B}{B}{R}{R}"
    typeLine = "Legendary Creature — Zombie Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying\nWhen Bladewing the Risen enters the battlefield, you may return target Dragon permanent card from your graveyard to the battlefield.\n{B}{R}: Dragon creatures get +1/+1 until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        optional = true
        target = TargetObject(
            filter = TargetFilter(
                GameObjectFilter.Permanent.withSubtype("Dragon").ownedByYou(),
                zone = Zone.GRAVEYARD
            )
        )
        effect = MoveToZoneEffect(
            target = EffectTarget.ContextTarget(0),
            destination = Zone.BATTLEFIELD
        )
    }

    activatedAbility {
        cost = Costs.Mana("{B}{R}")
        effect = Effects.ModifyStatsForAll(
            power = 1,
            toughness = 1,
            filter = GroupFilter.allCreaturesWithSubtype("Dragon")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "136"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2bd3e13d-53f8-42bf-aa83-09a9ca94a9f0.jpg?1562527045"
    }
}
