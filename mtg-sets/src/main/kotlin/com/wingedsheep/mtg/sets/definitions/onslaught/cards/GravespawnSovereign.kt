package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MoveToZoneEffect

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
        target = Targets.CreatureCardInGraveyard
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.BATTLEFIELD)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "152"
        artist = "Adam Rex"
        flavorText = "The Cabal never expected its creations to create servants of their own."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e786cca-41e1-41c1-a1af-f2a32f776bba.jpg?1562898189"
    }
}
