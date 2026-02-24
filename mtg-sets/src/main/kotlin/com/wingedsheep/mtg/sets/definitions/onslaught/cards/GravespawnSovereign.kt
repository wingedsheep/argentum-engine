package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect

/**
 * Gravespawn Sovereign
 * {4}{B}{B}
 * Creature — Zombie
 * 3/3
 * Tap five untapped Zombies you control: Put target creature card from a graveyard onto the battlefield under your control.
 */
val GravespawnSovereign = card("Gravespawn Sovereign") {
    manaCost = "{4}{B}{B}"
    typeLine = "Creature — Zombie"
    power = 3
    toughness = 3
    oracleText = "Tap five untapped Zombies you control: Put target creature card from a graveyard onto the battlefield under your control."

    activatedAbility {
        cost = Costs.TapPermanents(5, GameObjectFilter.Creature.withSubtype("Zombie"))
        val t = target("target", Targets.CreatureCardInGraveyard)
        effect = MoveToZoneEffect(t, Zone.BATTLEFIELD)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "152"
        artist = "Adam Rex"
        flavorText = "The Cabal never expected its creations to create servants of their own."
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e18dc249-a343-4198-bef9-e8092a2bac15.jpg?1562948670"
    }
}
