package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.scripting.ZonePlacement
import com.wingedsheep.sdk.targeting.TargetObject

/**
 * Aphetto Vulture
 * {4}{B}{B}
 * Creature — Zombie Bird
 * 3/2
 * Flying
 * When Aphetto Vulture dies, you may put target Zombie card from your graveyard
 * on top of your library.
 */
val AphettoVulture = card("Aphetto Vulture") {
    manaCost = "{4}{B}{B}"
    typeLine = "Creature — Zombie Bird"
    power = 3
    toughness = 2
    oracleText = "Flying\nWhen Aphetto Vulture dies, you may put target Zombie card from your graveyard on top of your library."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.Dies
        optional = true
        target = TargetObject(
            filter = TargetFilter(
                GameObjectFilter.Any.withSubtype("Zombie").ownedByYou(),
                zone = Zone.GRAVEYARD
            )
        )
        effect = MoveToZoneEffect(
            target = EffectTarget.ContextTarget(0),
            destination = Zone.LIBRARY,
            placement = ZonePlacement.Top
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "Tony Szczudlo"
        flavorText = "It feeds on the crawling dead and nests in the standing dead."
        imageUri = "https://cards.scryfall.io/large/front/1/0/107492b9-03a8-4d53-a0cf-4814ffbec409.jpg?1562898947"
    }
}
