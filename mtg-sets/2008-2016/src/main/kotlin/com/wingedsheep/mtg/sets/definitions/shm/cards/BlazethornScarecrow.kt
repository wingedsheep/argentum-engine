package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Blazethorn Scarecrow
 * {5}
 * Artifact Creature — Scarecrow
 * 3 / 3
 *
 * This creature has haste as long as you control a red creature.
 * This creature has wither as long as you control a green creature. (It deals damage to creatures
 * in the form of -1/-1 counters.)
 *
 * - Two independent conditional statics, one per granted keyword — the two clauses have separate
 *   conditions and either can be on without the other.
 * - [GroupFilter.source] is the "this creature" scope: the grant applies only to the Scarecrow
 *   itself, and it re-evaluates continuously, so the keyword switches off the moment the last red
 *   or green creature leaves.
 * - The colour checks read *creatures you control*, not permanents — Shadowmoor's cycle is worded
 *   "a red creature", so a red artifact or land doesn't turn the ability on. Filtering by colour
 *   goes through [GameObjectFilter] so the projected (post-continuous-effect) colour is what
 *   counts, which is what makes an animated or colour-shifted creature switch it on.
 */
val BlazethornScarecrow = card("Blazethorn Scarecrow") {
    manaCost = "{5}"
    typeLine = "Artifact Creature — Scarecrow"
    power = 3
    toughness = 3
    oracleText = "This creature has haste as long as you control a red creature.\n" +
        "This creature has wither as long as you control a green creature. (It deals damage to creatures in the form of -1/-1 counters.)"

    // This creature has haste as long as you control a red creature.
    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, GroupFilter.source())
        condition = Conditions.YouControl(GameObjectFilter.Creature.withColor(Color.RED))
    }

    // This creature has wither as long as you control a green creature.
    staticAbility {
        ability = GrantKeyword(Keyword.WITHER, GroupFilter.source())
        condition = Conditions.YouControl(GameObjectFilter.Creature.withColor(Color.GREEN))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "246"
        artist = "Dave Kendall"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44793043-82b4-415b-a9ab-d564fdbcd314.jpg?1783942713"
    }
}
