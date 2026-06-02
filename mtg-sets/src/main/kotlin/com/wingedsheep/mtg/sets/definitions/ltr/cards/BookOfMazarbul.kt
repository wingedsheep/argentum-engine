package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.GroupPatterns

/**
 * Book of Mazarbul
 * {2}{R}
 * Enchantment — Saga
 *
 * I — Amass Orcs 1.
 * II — Amass Orcs 2.
 * III — Creatures you control get +1/+0 and gain menace until end of turn.
 */
val BookOfMazarbul = card("Book of Mazarbul") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Amass Orcs 1. (Put a +1/+1 counter on an Army you control. It's also an Orc. If you don't " +
        "control an Army, create a 0/0 black Orc Army creature token first.)\n" +
        "II — Amass Orcs 2.\n" +
        "III — Creatures you control get +1/+0 and gain menace until end of turn."

    sagaChapter(1) {
        effect = Effects.Amass(1, "Orc")
    }
    sagaChapter(2) {
        effect = Effects.Amass(2, "Orc")
    }
    sagaChapter(3) {
        effect = GroupPatterns.modifyStatsForAll(1, 0, GroupFilter.AllCreaturesYouControl)
            .then(GroupPatterns.grantKeywordToAll(Keyword.MENACE, GroupFilter.AllCreaturesYouControl))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04dcef75-6f98-4233-ae32-6fe41724c8e0.jpg?1688569131"
    }
}
