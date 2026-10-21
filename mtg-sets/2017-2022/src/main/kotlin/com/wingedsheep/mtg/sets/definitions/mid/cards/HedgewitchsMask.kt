package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hedgewitch's Mask
 * {W}
 * Artifact — Equipment
 * Equipped creature gets +1/+1.
 * Equipped creature can't be blocked by creatures with power 4 or greater.
 * Equip {2}
 *
 * The evasion is scoped to the equipped creature ([GroupFilter.attachedCreature]); the default
 * `GroupFilter.source()` would put it on the Equipment itself.
 */
val HedgewitchsMask = card("Hedgewitch's Mask") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1.\n" +
        "Equipped creature can't be blocked by creatures with power 4 or greater.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(1, 1)
    }
    staticAbility {
        ability = CantBeBlockedBy(
            GameObjectFilter.Creature.powerAtLeast(4),
            GroupFilter.attachedCreature()
        )
    }
    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "23"
        artist = "Ovidio Cartagena"
        flavorText = "In Kessig, even celebration wears a fearsome face."
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97321072-1f7f-413b-9246-e6b3ec7a8c30.jpg"
    }
}
