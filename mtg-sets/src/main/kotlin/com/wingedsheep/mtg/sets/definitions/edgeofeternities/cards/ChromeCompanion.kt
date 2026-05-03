package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Chrome Companion
 * {2}
 * Artifact Creature — Dog
 * Whenever this creature becomes tapped, you gain 1 life.
 * {2}, {T}: Put target card from a graveyard on the bottom of its owner's library.
 */
val ChromeCompanion = card("Chrome Companion") {
    manaCost = "{2}"
    typeLine = "Artifact Creature — Dog"
    power = 2
    toughness = 1
    oracleText = "Whenever this creature becomes tapped, you gain 1 life.\n{2}, {T}: Put target card from a graveyard on the bottom of its owner's library."

    // Triggered ability: Gain 1 life when this creature becomes tapped
    triggeredAbility {
        trigger = Triggers.BecomesTapped
        effect = Effects.GainLife(1)
    }

    // Activated ability: {2}, {T}: Put target card from a graveyard on the bottom of its owner's library
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val cardInGraveyard = target(
            "target card from a graveyard",
            TargetObject(filter = TargetFilter(GameObjectFilter.Any))
        )
        effect = MoveToZoneEffect(
            target = cardInGraveyard,
            destination = Zone.LIBRARY,
            placement = ZonePlacement.Bottom
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "236"
        artist = "Gray Highsmith"
        flavorText = "Liberation Station's morale greatly improved after Rover took up residence."
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8ef269a0-1cb9-4901-81e6-43db3ae3756c.jpg?1752947525"
    }
}
