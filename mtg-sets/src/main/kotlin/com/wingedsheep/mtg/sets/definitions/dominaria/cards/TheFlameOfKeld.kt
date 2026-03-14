package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SourceFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Flame of Keld
 * {1}{R}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Discard your hand.
 * II — Draw two cards.
 * III — If a red source you control would deal damage to a permanent or player this turn,
 *        it deals that much damage plus 2 instead.
 */
val TheFlameOfKeld = card("The Flame of Keld") {
    manaCost = "{1}{R}"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Discard your hand.\n" +
        "II — Draw two cards.\n" +
        "III — If a red source you control would deal damage to a permanent or player this turn, " +
        "it deals that much damage plus 2 instead."

    sagaChapter(1) {
        effect = EffectPatterns.discardHand(EffectTarget.Controller)
    }

    sagaChapter(2) {
        effect = Effects.DrawCards(2)
    }

    sagaChapter(3) {
        effect = Effects.GrantDamageBonus(
            bonusAmount = 2,
            sourceFilter = SourceFilter.HasColor(Color.RED)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Lake Hurwitz"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/324399ed-3d6e-4b4c-8f3d-b7802e2ecadf.jpg?1562733690"
    }
}
