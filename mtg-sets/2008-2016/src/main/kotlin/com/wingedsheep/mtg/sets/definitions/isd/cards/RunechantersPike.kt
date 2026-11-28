package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Runechanter's Pike
 * {2}
 * Artifact — Equipment
 * Equipped creature has first strike and gets +X/+0, where X is the number of instant and sorcery
 * cards in your graveyard.
 * Equip {2}
 *
 * The bonus is a Layer 7c [GrantDynamicStats] recomputed at projection (as Cranial Ram), counting
 * instant and sorcery cards in the Pike controller's graveyard (as Spellheart Chimera).
 */
val RunechantersPike = card("Runechanter's Pike") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has first strike and gets +X/+0, where X is the number of " +
        "instant and sorcery cards in your graveyard.\n" +
        "Equip {2}"

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE, Filters.EquippedCreature)
    }
    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.InstantOrSorcery),
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }
    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "231"
        artist = "John Avon"
        flavorText = "As sharp as its bearer's words of faith."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f54e38b-b4a0-4406-a635-7a5ab3722f25.jpg?1783940900"
    }
}
