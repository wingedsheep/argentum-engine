package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Ancestral Vision
 * Sorcery (no mana cost — can only be played via suspend)
 *
 * Suspend 4—{U} (Rather than cast this card from your hand, pay {U} and exile it with four
 * time counters on it. At the beginning of your upkeep, remove a time counter. When the last
 * is removed, you may cast it without paying its mana cost.)
 * Target player draws three cards.
 *
 * Printed with no mana cost — CR 202.1b/118.6: it represents an unpayable cost and can't be cast
 * normally, only suspended (or cast by some other alternative-cost/free-cast effect). Blue is a
 * printed color indicator (CR 204), not derived from a mana cost that doesn't exist, so
 * `colorIndicator` carries it explicitly alongside `colorIdentity` (Scryfall ruling, 2013-06-07).
 *
 * Suspend itself is the new mechanic here (CR 702.62 / CR 116.2f special action): see
 * [KeywordAbility.Suspend] plus the engine's `SuspendCardFromHandHandler` / `SuspendEnumerator`,
 * which exile the card with time counters outside the stack. The countdown and eventual free
 * cast are handled by the engine's synthesized `Suspend.countdownAbility`, granted to any
 * exiled card carrying the suspended marker — shared with the runtime-grant path (Taigam,
 * Master Opportunist).
 */
val AncestralVision = card("Ancestral Vision") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U"
    typeLine = "Sorcery"
    oracleText = "Suspend 4—{U} (Rather than cast this card from your hand, pay {U} and exile it " +
        "with four time counters on it. At the beginning of your upkeep, remove a time counter. " +
        "When the last is removed, you may cast it without paying its mana cost.)\n" +
        "Target player draws three cards."

    spell {
        val t = target("target", TargetPlayer())
        effect = Effects.DrawCards(3, t)
    }

    keywordAbility(KeywordAbility.suspend("{U}", 4))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "48"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bccedc4d-38c7-4bf3-9ca7-4febd6c49d3d.jpg?1783943248"
    }
}
