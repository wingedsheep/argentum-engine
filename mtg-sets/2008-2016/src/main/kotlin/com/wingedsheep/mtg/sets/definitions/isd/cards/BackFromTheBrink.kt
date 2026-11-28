package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Back from the Brink
 * {4}{U}{U}
 * Enchantment
 * Exile a creature card from your graveyard and pay its mana cost: Create a token that's a copy of
 * that card. Activate only as a sorcery.
 *
 * The cost is [Costs.ExileAndPayItsManaCost]: the creature card picked sets the mana paid (its {X}
 * as 0), and only cards the payer can afford are offered. Paying it isn't casting the card, so cast
 * cost reductions don't apply. By resolution the card is in exile, so [CardSource.ExiledAsCost]
 * names the one this activation paid with — Dollhouse of Horrors' shape, without its exceptions.
 */
val BackFromTheBrink = card("Back from the Brink") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Exile a creature card from your graveyard and pay its mana cost: Create a token " +
        "that's a copy of that card. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.ExileAndPayItsManaCost(GameObjectFilter.Creature)
        timing = TimingRule.SorcerySpeed
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.ExiledAsCost)
            run(Effects.CreateTokenCopyOfTarget(target = exiled.asTarget))
        }
        description = "Exile a creature card from your graveyard and pay its mana cost: Create a " +
            "token that's a copy of that card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "44"
        artist = "Anthony Palumbo"
        flavorText = "On Innistrad, death is just a career change."
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4bba140-5c06-4542-9ae0-b2517104ab7c.jpg?1783940981"
    }
}
