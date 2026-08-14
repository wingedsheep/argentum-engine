package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CrewSaddleContribution
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Deathless Pilot — Aetherdrift #82
 * {1}{B} · Creature — Zombie Pilot · 2/2
 */
val DeathlessPilot = card("Deathless Pilot") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Pilot"
    oracleText = "This creature saddles Mounts and crews Vehicles as though its power were 2 greater.\n" +
        "{3}{B}: Return this card from your graveyard to your hand."
    power = 2
    toughness = 2

    staticAbility {
        ability = CrewSaddleContribution(modifier = 2)
    }

    activatedAbility {
        cost = Costs.Mana("{3}{B}")
        effect = Effects.Move(EffectTarget.Self, Zone.HAND)
        activateFromZone = Zone.GRAVEYARD
        description = "{3}{B}: Return this card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Justin Cornell"
        flavorText = "Amonkhet champions become markedly more reckless after dying."
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e704fb95-17b7-432a-831c-18abe7d9cc73.jpg?1783907896"
    }
}
