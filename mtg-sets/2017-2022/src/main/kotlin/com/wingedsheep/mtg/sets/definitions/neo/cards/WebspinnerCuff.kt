package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Webspinner Cuff — Kamigawa: Neon Dynasty #214 (canonical printing)
 * {2}{G} · Artifact Creature — Equipment Spider · 1/4
 *
 * Reach
 * Equipped creature gets +1/+4 and has reach.
 * Reconfigure {4}
 */
val WebspinnerCuff = card("Webspinner Cuff") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Artifact Creature — Equipment Spider"
    power = 1
    toughness = 4
    oracleText = "Reach\nEquipped creature gets +1/+4 and has reach.\n" +
        "Reconfigure {4} ({4}: Attach to target creature you control; or unattach from a creature. " +
        "Reconfigure only as a sorcery. While attached, this isn't a creature.)"

    keywords(Keyword.REACH)

    staticAbility {
        ability = ModifyStats(+1, +4, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.REACH, Filters.EquippedCreature)
    }

    reconfigure("{4}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "214"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e73cc6a7-0a18-488c-b52a-787da95a2e6b.jpg?1783923837"
    }
}
