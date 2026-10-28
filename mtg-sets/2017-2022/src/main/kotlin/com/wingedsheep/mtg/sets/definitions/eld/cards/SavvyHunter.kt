package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Savvy Hunter — Throne of Eldraine #200
 * {1}{B}{G} · Creature — Human Warrior · 3/3
 *
 * Whenever this creature attacks or blocks, create a Food token.
 * Sacrifice two Foods: Draw a card.
 *
 * "Attacks or blocks" is two triggers sharing one effect (Burning Sun Cavalry's shape). The Food
 * cost is subtype-based (`GameObjectFilter.Any.withSubtype("Food")`, Samwise Gamgee's shape), so
 * any Food artifact — token or not — can be sacrificed.
 */
val SavvyHunter = card("Savvy Hunter") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature attacks or blocks, create a Food token. (It's an artifact with " +
        "\"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "Sacrifice two Foods: Draw a card."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.CreateFood()
    }

    triggeredAbility {
        trigger = Triggers.self.blocks()
        effect = Effects.CreateFood()
    }

    activatedAbility {
        cost = Costs.SacrificeMultiple(2, GameObjectFilter.Any.withSubtype("Food"))
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "200"
        artist = "Dan Dos Santos"
        flavorText = "None of the other villagers dare venture into the wilds, so she feeds the whole " +
            "hamlet herself."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15c98441-2b31-4e48-a399-f36dffcfa41d.jpg?1783932594"
    }
}
