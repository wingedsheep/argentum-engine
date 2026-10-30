package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Nylea's Intervention
 * {X}{G}{G}
 * Sorcery
 * Choose one —
 * • Search your library for up to X land cards, reveal them, put them into your hand, then shuffle.
 * • Nylea's Intervention deals twice X damage to each creature with flying.
 *
 * The green Intervention: both modes read the one X paid at cast time. The tutor is the
 * dynamic-count [Patterns.Library.searchLibrary] ("up to X", revealed, to hand, then shuffle); the
 * sweep is a single `X * 2` damage event to each flier (ruling: one instance of twice X, not two of X).
 */
val NyleasIntervention = card("Nylea's Intervention") {
    manaCost = "{X}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Search your library for up to X land cards, reveal them, put them into your hand, then shuffle.\n" +
        "• Nylea's Intervention deals twice X damage to each creature with flying."

    spell {
        modal(chooseCount = 1) {
            mode("Search your library for up to X land cards, reveal them, put them into your hand, then shuffle") {
                effect = Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.Land,
                    count = DynamicAmounts.xValue(),
                    destination = SearchDestination.HAND,
                    reveal = true
                )
            }
            mode("Nylea's Intervention deals twice X damage to each creature with flying") {
                effect = Patterns.Group.dealDamageToAll(
                    DynamicAmounts.xValue() * 2,
                    GroupFilter.AllCreatures.withKeyword(Keyword.FLYING)
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "188"
        artist = "Zezhou Chen"
        flavorText = "\"I bring a wilderness no law can tame.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/a/daa2f963-9d16-4224-b24e-b6a79f2b9d75.jpg?1783931532"
        ruling(
            "2020-01-24",
            "The second mode of Nylea's Intervention has it deal damage to each creature with flying equal to " +
                "twice X. It doesn't deal X damage to them then deal X damage again."
        )
    }
}
