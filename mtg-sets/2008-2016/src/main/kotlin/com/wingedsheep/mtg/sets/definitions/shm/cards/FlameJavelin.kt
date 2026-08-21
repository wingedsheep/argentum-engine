package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Flame Javelin
 * {2/R}{2/R}{2/R}
 * Instant
 *
 * ({2/R} can be paid with any two mana or with {R}. This card's mana value is 6.)
 * Flame Javelin deals 4 damage to any target.
 *
 * - Monocoloured hybrid ("twobrid") goes into `manaCost` verbatim; the parser derives the mana
 *   value of 6 from the three {2/R} symbols, so nothing is overridden here.
 * - The first line is reminder text for the twobrid symbol and carries no rules content — it is
 *   kept in `oracleText` because that is what Scryfall prints.
 * - No `damageSource` is passed: the spell itself is the source, which is the facade's default.
 */
val FlameJavelin = card("Flame Javelin") {
    manaCost = "{2/R}{2/R}{2/R}"
    typeLine = "Instant"
    oracleText = "({2/R} can be paid with any two mana or with {R}. This card's mana value is 6.)\n" +
        "Flame Javelin deals 4 damage to any target."

    spell {
        val t = target("target", Targets.Any)
        effect = Effects.DealDamage(4, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Trevor Hairsine"
        flavorText = "Gyara Spearhurler would have been renowned for her deadly accuracy, if it weren't for her deadly accuracy."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a567b570-81e4-4068-929c-9ce406fe7474.jpg?1783942749"
    }
}
