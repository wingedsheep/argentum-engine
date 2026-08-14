package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Gadwick's First Duel
 * {1}{U}
 * Enchantment — Saga
 *
 * I — Create a Cursed Role token attached to up to one target creature.
 * II — Scry 2.
 * III — Copy the next instant or sorcery spell with mana value 3 or less you cast this turn.
 */
val GadwicksFirstDuel = card("Gadwick's First Duel") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Create a Cursed Role token attached to up to one target creature. (If you control " +
        "another Role on it, put that one into the graveyard. Enchanted creature is 1/1.)\n" +
        "II — Scry 2.\n" +
        "III — When you next cast an instant or sorcery spell with mana value 3 or less this turn, " +
        "copy that spell. You may choose new targets for the copy."

    sagaChapter(1) {
        val creature = target(
            "up to one target creature",
            TargetCreature(optional = true, filter = TargetFilter.Creature)
        )
        effect = Effects.CreateRoleToken("Cursed Role", creature)
    }

    sagaChapter(2) {
        effect = Effects.Scry(2)
    }

    sagaChapter(3) {
        effect = Effects.CopyNextSpellCast(
            copies = 1,
            spellFilter = GameObjectFilter.InstantOrSorcery.manaValueAtMost(3)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af07c47f-8b4e-43cb-b469-2efb82aa5590.jpg?1783915120"

        ruling("2023-09-01", "If you don't choose a target for the first chapter ability, the Cursed Role token won't be created.")
        ruling("2023-09-01", "The copy is created on the stack, so it isn't cast.")
    }
}
