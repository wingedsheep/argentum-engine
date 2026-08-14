package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulbond
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Lightning Mauler
 * {1}{R}
 * Creature — Human Berserker
 * 2/1
 * Soulbond (You may pair this creature with another unpaired creature when either enters. They
 * remain paired for as long as you control both of them.)
 * As long as this creature is paired with another creature, both creatures have haste.
 *
 * The payoff is a plain [GrantKeyword] over `GroupFilter.soulbondPair()` — the scope covers "both
 * creatures" and is empty while unpaired, so "as long as this creature is paired" needs no
 * condition of its own (CR 702.95b/e).
 */
val LightningMauler = card("Lightning Mauler") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Berserker"
    oracleText = "Soulbond (You may pair this creature with another unpaired creature when either " +
        "enters. They remain paired for as long as you control both of them.)\n" +
        "As long as this creature is paired with another creature, both creatures have haste."
    power = 2
    toughness = 1

    soulbond()

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, GroupFilter.soulbondPair())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "144"
        artist = "Dave Kendall"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/241cc968-b93e-4fe3-a66d-7776d29aa023.jpg?1783940681"
    }
}
