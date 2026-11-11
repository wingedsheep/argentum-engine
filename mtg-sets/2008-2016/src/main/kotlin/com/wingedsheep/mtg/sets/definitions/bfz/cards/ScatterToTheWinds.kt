package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scatter to the Winds
 * {1}{U}{U}
 * Instant
 * Counter target spell.
 * Awaken 3—{4}{U}{U} (If you cast this spell for {4}{U}{U}, also put three +1/+1 counters on
 * target land you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)
 *
 * The `spell { }` block holds only the printed spell: casting for the awaken cost adds the land
 * target after the spell target and runs the awaken ability after the counter.
 */
val ScatterToTheWinds = card("Scatter to the Winds") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell.\n" +
        "Awaken 3—{4}{U}{U} (If you cast this spell for {4}{U}{U}, also put three +1/+1 counters on " +
        "target land you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)"

    keywordAbility(KeywordAbility.awaken(3, "{4}{U}{U}"))

    spell {
        val spell = target(TargetFilter.SpellOnStack)
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "85"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d73ad49f-fe15-4fe5-9731-fd71d31c1e7f.jpg?1783938207"
    }
}
