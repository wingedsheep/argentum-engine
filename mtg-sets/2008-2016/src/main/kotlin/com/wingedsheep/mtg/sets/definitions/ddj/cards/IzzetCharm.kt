package com.wingedsheep.mtg.sets.definitions.ddj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Izzet Charm — Duel Decks: Izzet vs. Golgari #21
 * {U}{R} · Instant · Uncommon
 *
 * Choose one —
 * • Counter target noncreature spell unless its controller pays {2}.
 * • Izzet Charm deals 2 damage to target creature.
 * • Draw two cards, then discard two cards.
 *
 * Canonical printing: Duel Decks: Izzet vs. Golgari (2012-09-07) predates Return to Ravnica,
 * which carries a Printing row.
 */
val IzzetCharm = card("Izzet Charm") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Counter target noncreature spell unless its controller pays {2}.\n" +
        "• Izzet Charm deals 2 damage to target creature.\n" +
        "• Draw two cards, then discard two cards."

    spell {
        modal(chooseCount = 1) {
            mode("Counter target noncreature spell unless its controller pays {2}") {
                target(TargetFilter.NoncreatureSpellOnStack)
                effect = Effects.CounterUnlessPays("{2}")
            }
            mode("Izzet Charm deals 2 damage to target creature") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.DealDamage(2, creature)
            }
            mode("Draw two cards, then discard two cards") {
                effect = Effects.DrawCards(2) then Effects.Discard(2)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61289196-a56b-4d24-b340-9cf067c77f45.jpg?1783940416"
    }
}
