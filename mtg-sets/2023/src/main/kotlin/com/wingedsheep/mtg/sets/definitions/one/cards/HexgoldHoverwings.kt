package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.forMirrodin
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hexgold Hoverwings — Phyrexia: All Will Be One #14
 * {3}{W} · Artifact — Equipment · Uncommon
 *
 * For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it.)
 * Equipped creature has flying.
 * Creatures you control that are equipped get +1/+0.
 * Equip {2}{W}
 *
 * The +1/+0 is a lord over every equipped creature you control (by any Equipment), not just the
 * one this Equipment is attached to — the Kemba, Kha Enduring shape.
 */
val HexgoldHoverwings = card("Hexgold Hoverwings") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it.)\n" +
        "Equipped creature has flying.\n" +
        "Creatures you control that are equipped get +1/+0.\n" +
        "Equip {2}{W}"

    forMirrodin()

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, Filters.EquippedCreature)
    }

    staticAbility {
        ability = ModifyStats(1, 0, GroupFilter(GameObjectFilter.Creature.youControl().equipped()))
    }

    equipAbility("{2}{W}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Kai Carpenter"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c498dbf9-c68c-400c-9349-c3c3fc8efcae.jpg?1783918081"
    }
}
