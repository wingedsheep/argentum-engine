package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Vitu-Ghazi Guildmage
 * {G}{W}
 * Creature — Dryad Shaman
 * 2/2
 * {4}{G}{W}: Create a 3/3 green Centaur creature token.
 * {2}{G}{W}: Populate. (Create a token that's a copy of a creature token you control.)
 *
 * Populate is [Effects.CreateTokenCopyOfChosenPermanent] over creature tokens you control
 * (chosen on resolution, not targeted), as on Growing Ranks and Horncaller's Chant.
 *
 * Canonical printing: Return to Ravnica (earliest); reprinted in C19.
 */
val VituGhaziGuildmage = card("Vitu-Ghazi Guildmage") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Dryad Shaman"
    power = 2
    toughness = 2
    oracleText = "{4}{G}{W}: Create a 3/3 green Centaur creature token.\n" +
        "{2}{G}{W}: Populate. (Create a token that's a copy of a creature token you control.)"

    activatedAbility {
        cost = Costs.Mana("{4}{G}{W}")
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Centaur"),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{2}{G}{W}")
        effect = Effects.CreateTokenCopyOfChosenPermanent(GameObjectFilter.Creature.token())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "Jason Chan"
        flavorText = "\"Preachers of freedom forget that conformity can be beautiful.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e54f8e61-550f-4493-b8ba-65f81b2457d3.jpg?1783940330"
        ruling(
            "2024-01-12",
            "Populate doesn't target the creature token you're copying. You choose that creature token as you're taking the populate action. You can choose any creature token you control."
        )
        ruling(
            "2024-01-12",
            "If you control no creature tokens when you populate, nothing will happen."
        )
    }
}
