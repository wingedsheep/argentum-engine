package com.wingedsheep.mtg.sets.definitions.c19.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Bone Miser
 * {4}{B}
 * Creature — Zombie Wizard
 * 4/4
 * Whenever you discard a creature card, create a 2/2 black Zombie creature token.
 * Whenever you discard a land card, add {B}{B}.
 * Whenever you discard a noncreature, nonland card, draw a card.
 *
 * Three per-card `Triggers.you.discards(filter)` triggers. A creature land would trip both the
 * creature and the land trigger, as the printed text says.
 */
val BoneMiser = card("Bone Miser") {
    manaCost = "{4}{B}"
    typeLine = "Creature — Zombie Wizard"
    power = 4
    toughness = 4
    oracleText = "Whenever you discard a creature card, create a 2/2 black Zombie creature token.\n" +
        "Whenever you discard a land card, add {B}{B}.\n" +
        "Whenever you discard a noncreature, nonland card, draw a card."

    triggeredAbility {
        trigger = Triggers.you.discards(GameObjectFilter.Creature)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            imageUri = "https://cards.scryfall.io/normal/front/5/9/59bad855-ff86-4f98-bf18-0982c7aea9c5.jpg?1783932829",
        )
        description = "Create a 2/2 black Zombie creature token."
    }

    triggeredAbility {
        trigger = Triggers.you.discards(GameObjectFilter.Land)
        effect = Effects.AddMana(Color.BLACK, 2)
        description = "Add {B}{B}."
    }

    triggeredAbility {
        trigger = Triggers.you.discards(GameObjectFilter.Noncreature and GameObjectFilter.Nonland)
        effect = Effects.DrawCards(1)
        description = "Draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7fa6901-e74f-448c-9d26-99a692092512.jpg?1783932810"
    }
}
