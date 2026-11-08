package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Gorgon Flail
 * {2}
 * Artifact — Equipment
 *
 * Equipped creature gets +1/+1 and has deathtouch.
 * Equip {2}
 */
val GorgonFlail = card("Gorgon Flail") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1 and has deathtouch. " +
        "(Any amount of damage it deals to a creature is enough to destroy that creature.)\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(1, 1, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.DEATHTOUCH, Filters.EquippedCreature)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "211"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eefc48cb-79aa-4e28-a736-ae9e4e2bc97b.jpg?1783942356"
    }
}
