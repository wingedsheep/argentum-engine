package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.MustBeBlocked
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Slayer's Cleaver
 * {3}
 * Artifact — Equipment
 * Equipped creature gets +3/+1 and must be blocked by an Eldrazi if able.
 * Equip {4}
 *
 * The block requirement is the blocker-filtered "if able" form of [MustBeBlocked]: only an Eldrazi
 * obeys it, one is enough, and with no Eldrazi able to block (untapped, cost-free) nothing is forced.
 */
val SlayersCleaver = card("Slayer's Cleaver") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +3/+1 and must be blocked by an Eldrazi if able.\n" +
        "Equip {4}"

    staticAbility {
        ability = ModifyStats(3, 1, GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = MustBeBlocked(
            filter = GroupFilter.attachedCreature(),
            blockers = GameObjectFilter.Creature.withSubtype("Eldrazi"),
        )
    }

    equipAbility("{4}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "198"
        artist = "Daniel Ljunggren"
        flavorText = "\"Don't stop chopping until the pieces stop wiggling.\"\n—Rem Karolus, Slayer of Eldrazi"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9de4a83a-3a0c-4174-ba65-a6e100383b32.jpg?1783937422"
    }
}
