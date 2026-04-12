package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost

/**
 * Wild Unraveling
 * {U}{U}
 * Instant
 *
 * As an additional cost to cast this spell, blight 2 or pay {1}.
 * (To blight 2, put two -1/-1 counters on a creature you control.)
 * Counter target spell.
 */
val WildUnraveling = card("Wild Unraveling") {
    manaCost = "{U}{U}"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, blight 2 or pay {1}. " +
        "(To blight 2, put two -1/-1 counters on a creature you control.)\n" +
        "Counter target spell."

    additionalCost(AdditionalCost.BlightOrPay(blightAmount = 2, alternativeManaCost = "{1}"))

    spell {
        target = Targets.Spell
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Jabari Weathers"
        flavorText = "As wild magic ripped into his arm, the path that was once so clear became shadowed by his own fear."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01522fec-9136-4fa6-91a3-370a8bb08b42.jpg?1767871807"
    }
}
