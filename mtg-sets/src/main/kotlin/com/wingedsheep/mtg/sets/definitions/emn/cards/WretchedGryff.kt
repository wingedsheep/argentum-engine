package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.emerge
import com.wingedsheep.sdk.model.Rarity

/**
 * Wretched Gryff
 * {7}
 * Creature — Eldrazi Hippogriff
 * 3/4
 *
 * Emerge {5}{U}
 * When you cast this spell, draw a card.
 * Flying
 *
 * Implementation notes:
 * - Emerge is the engine keyword (CR 702.119) via the `emerge(cost)` helper; the alternative cost,
 *   the creature sacrifice, and the generic reduction by that creature's mana value all live in the
 *   engine's alternative-cost pipeline.
 * - The draw is a *cast* trigger, not an enters-the-battlefield trigger — it resolves before the
 *   creature itself, and still resolves if the spell is countered.
 */
val WretchedGryff = card("Wretched Gryff") {
    manaCost = "{7}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Hippogriff"
    power = 3
    toughness = 4
    oracleText = "Emerge {5}{U} (You may cast this spell by sacrificing a creature and paying the " +
        "emerge cost reduced by that creature's mana value.)\n" +
        "When you cast this spell, draw a card.\nFlying"

    keywords(Keyword.FLYING)

    emerge("{5}{U}")

    triggeredAbility {
        trigger = Triggers.WhenYouCastThisSpell()
        effect = Effects.DrawCards(1)
        description = "When you cast this spell, draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "12"
        artist = "Darek Zabrocki"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d65efec-018f-485c-906c-460379b4af87.jpg?1783937525"
    }
}
