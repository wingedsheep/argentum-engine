package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.emerge
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Drownyard Behemoth
 * {9}
 * Creature — Eldrazi Crab
 * 5/7
 *
 * Flash
 * Emerge {7}{U}
 * This creature has hexproof as long as it entered this turn.
 *
 * Implementation notes:
 * - Emerge via the `emerge(cost)` helper; flash makes the emerge cast legal at instant speed too.
 * - The hexproof grant is a self-scoped conditional static gated on `Conditions.SourceEnteredThisTurn`
 *   (the Crew Captain shape), so it switches off at the next turn without any cleanup.
 */
val DrownyardBehemoth = card("Drownyard Behemoth") {
    manaCost = "{9}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Crab"
    power = 5
    toughness = 7
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "Emerge {7}{U} (You may cast this spell by sacrificing a creature and paying the emerge cost " +
        "reduced by that creature's mana value.)\n" +
        "This creature has hexproof as long as it entered this turn."

    keywords(Keyword.FLASH)

    emerge("{7}{U}")

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HEXPROOF, GroupFilter.source()),
            condition = Conditions.SourceEnteredThisTurn
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "4"
        artist = "Jason Kang"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/082690e8-07b9-4a91-b779-e0123a69ee12.jpg?1783937527"
    }
}
