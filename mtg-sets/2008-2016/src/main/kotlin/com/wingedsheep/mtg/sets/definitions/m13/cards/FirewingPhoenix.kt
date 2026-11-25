package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Firewing Phoenix
 * {3}{R}
 * Creature — Phoenix
 * 4/2
 * Flying
 * {1}{R}{R}{R}: Return this card from your graveyard to your hand.
 *
 * The return ability is activated from the graveyard (`activateFromZone = Zone.GRAVEYARD`), the
 * same shape as Aetherdrift's Deathless Pilot.
 */
val FirewingPhoenix = card("Firewing Phoenix") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Phoenix"
    oracleText = "Flying\n{1}{R}{R}{R}: Return this card from your graveyard to your hand."
    power = 4
    toughness = 2

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{1}{R}{R}{R}")
        effect = Effects.ReturnToHandFromGraveyard(EffectTarget.Self)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "131"
        artist = "James Paick"
        flavorText = "\"When a phoenix nests in a volcano, we rejoice, for it means a hundred years of war.\"\n—Golmak, Keldon warlord"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8824674-ced2-448e-9bf0-03c1c43a5315.jpg"
    }
}
