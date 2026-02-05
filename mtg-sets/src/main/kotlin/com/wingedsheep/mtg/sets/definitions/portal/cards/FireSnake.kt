package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.scripting.Zone
import com.wingedsheep.sdk.targeting.TargetPermanent

/**
 * Fire Snake
 * {4}{R}
 * Creature — Snake
 * 3/1
 * When Fire Snake dies, destroy target land.
 */
val FireSnake = card("Fire Snake") {
    manaCost = "{4}{R}"
    typeLine = "Creature — Snake"
    power = 3
    toughness = 1

    triggeredAbility {
        trigger = Triggers.Dies
        target = TargetPermanent(filter = TargetFilter.Land)
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.Graveyard, byDestruction = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "127"
        artist = "Andrew Robinson"
        flavorText = "Its death leaves scorched earth behind."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4c36e32-59e8-4e3d-903e-a264211f2a82.jpg"
    }
}
