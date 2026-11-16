package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EquipmentAttachRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Gate Smasher
 * {3}
 * Artifact — Equipment
 * This Equipment can be attached only to a creature with toughness 4 or greater.
 * Equipped creature gets +3/+0 and has trample.
 * Equip {3}
 *
 * The restriction reads the would-be host's projected toughness: equip may still target any creature
 * you control, but resolves and does nothing on one with toughness 3 or less, and Gate Smasher falls
 * off as a state-based action once its host's toughness drops below 4.
 */
val GateSmasher = card("Gate Smasher") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "This Equipment can be attached only to a creature with toughness 4 or greater.\n" +
        "Equipped creature gets +3/+0 and has trample.\n" +
        "Equip {3}"

    staticAbility {
        ability = EquipmentAttachRestriction(GameObjectFilter.Creature.toughnessAtLeast(4))
    }

    staticAbility {
        ability = ModifyStats(3, 0, GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, GroupFilter.attachedCreature())
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "239"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/001e8222-8dea-4767-8c13-21fb7ba556d7.jpg?1783938568"
        ruling(
            "2015-02-25",
            "Gate Smasher's equip ability can target any creature. However, if that creature's toughness " +
                "is 3 or less as the equip ability resolves, Gate Smasher won't become attached to it."
        )
        ruling(
            "2015-02-25",
            "If Gate Smasher is attached to a creature whose toughness falls below 4, Gate Smasher will " +
                "become unattached as a state-based action."
        )
    }
}
