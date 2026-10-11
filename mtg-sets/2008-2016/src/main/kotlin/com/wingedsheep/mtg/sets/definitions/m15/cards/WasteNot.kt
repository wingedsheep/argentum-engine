package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Waste Not — Magic 2015 #122 (canonical printing)
 * {1}{B} · Enchantment
 *
 * Whenever an opponent discards a creature card, create a 2/2 black Zombie creature token.
 * Whenever an opponent discards a land card, add {B}{B}.
 * Whenever an opponent discards a noncreature, nonland card, draw a card.
 *
 * Three per-card discard triggers, each narrowed by `cardFilter`: a discard of several cards fires
 * each matching trigger once per matching card. The mana trigger is not a mana ability (it wasn't
 * triggered by one), so it uses the stack and the {B}{B} lands in the pool as it resolves.
 */
val WasteNot = card("Waste Not") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever an opponent discards a creature card, create a 2/2 black Zombie creature token.\n" +
        "Whenever an opponent discards a land card, add {B}{B}.\n" +
        "Whenever an opponent discards a noncreature, nonland card, draw a card."

    triggeredAbility {
        trigger = Triggers.anOpponent.discards(GameObjectFilter.Creature)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            imageUri = "https://cards.scryfall.io/normal/front/e/b/eb0a1ebd-a0ac-4a03-8ac2-4678d6fb03fa.jpg?1783939142",
        )
        description = "Whenever an opponent discards a creature card, create a 2/2 black Zombie creature token."
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.discards(GameObjectFilter.Land)
        effect = Effects.AddMana(Color.BLACK, 2)
        description = "Whenever an opponent discards a land card, add {B}{B}."
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.discards(GameObjectFilter.Noncreature and GameObjectFilter.Nonland)
        effect = Effects.DrawCards(1)
        description = "Whenever an opponent discards a noncreature, nonland card, draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "122"
        artist = "Matt Stewart"
        flavorText = "Designed by the Magic community"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/241d8f7d-3981-47c1-b7b8-748277fa452f.jpg?1783939178"
    }
}
