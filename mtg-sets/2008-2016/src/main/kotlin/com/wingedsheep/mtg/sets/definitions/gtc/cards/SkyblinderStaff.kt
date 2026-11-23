package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Skyblinder Staff — Gatecrash #238
 * {1} · Artifact — Equipment
 *
 * Equipped creature gets +1/+0 and can't be blocked by creatures with flying.
 * Equip {3}
 */
val SkyblinderStaff = card("Skyblinder Staff") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+0 and can't be blocked by creatures with flying.\n" +
        "Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(1, 0, Filters.EquippedCreature)
    }

    staticAbility {
        ability = CantBeBlockedBy(
            blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING),
            filter = Filters.EquippedCreature
        )
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "238"
        artist = "Mark Winters"
        flavorText = "\"If bats are the only thing between me and a mountain of Orzhov coins, then I'm about to be a very rich thief.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1602ee8-019d-4dde-8d31-042207017615.jpg?1783940090"
        ruling("2013-01-24", "The equipped creature can still be blocked by a creature with reach.")
        ruling(
            "2013-01-24",
            "If the equipped creature is blocked by a creature and then that creature gains flying, the equipped " +
                "creature will remain blocked."
        )
    }
}
