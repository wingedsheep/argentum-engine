package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Necromancer's Stockpile — Magic 2015 #108 (canonical printing)
 * {1}{B} · Enchantment
 *
 * {1}{B}, Discard a creature card: Draw a card. If the discarded card was a Zombie card, create a
 * tapped 2/2 black Zombie creature token.
 *
 * The discard is a cost, so the card is already in the graveyard when the ability resolves; the
 * activation records it and `Conditions.DiscardedCardMatches` reads its characteristics from there.
 */
val NecromancersStockpile = card("Necromancer's Stockpile") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "{1}{B}, Discard a creature card: Draw a card. If the discarded card was a Zombie card, " +
        "create a tapped 2/2 black Zombie creature token."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.Discard(GameObjectFilter.Creature))
        effect = Effects.DrawCards(1) then
            Effects.If(
                condition = Conditions.DiscardedCardMatches(GameObjectFilter.Any.withSubtype("Zombie")),
                then = Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.BLACK),
                    creatureTypes = setOf("Zombie"),
                    tapped = true,
                ),
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "108"
        artist = "Seb McKinnon"
        flavorText = "The experiments decided to perform some research of their own."
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2ba07e86-fdef-4f51-808e-7780883eefe3.jpg?1783939181"
    }
}
