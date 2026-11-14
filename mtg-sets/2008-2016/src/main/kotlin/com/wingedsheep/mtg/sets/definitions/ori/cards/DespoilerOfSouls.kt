package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Despoiler of Souls
 * {B}{B}
 * Creature — Horror
 * 3/1
 *
 * This creature can't block.
 * {B}{B}, Exile two other creature cards from your graveyard: Return this card from your graveyard
 * to the battlefield.
 *
 * The recursion is activated from the graveyard, and the exile cost excludes the Despoiler itself
 * ([Costs.ExileAnotherFromGraveyard]) — it can't pay for its own return. No timing restriction:
 * it can be activated at instant speed.
 */
val DespoilerOfSouls = card("Despoiler of Souls") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror"
    oracleText = "This creature can't block.\n" +
        "{B}{B}, Exile two other creature cards from your graveyard: Return this card from your " +
        "graveyard to the battlefield."
    power = 3
    toughness = 1

    staticAbility {
        ability = CantBlock()
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{B}{B}"),
            Costs.ExileAnotherFromGraveyard(2, GameObjectFilter.Creature),
        )
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "93"
        artist = "Greg Staples"
        flavorText = "Rats and vultures devour. It desecrates."
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04a09fe4-d7a0-4065-968d-0837c3eafda0.jpg?1783938342"
    }
}
