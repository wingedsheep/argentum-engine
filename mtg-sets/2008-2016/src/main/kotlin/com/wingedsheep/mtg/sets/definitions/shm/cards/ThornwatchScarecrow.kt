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
 * Thornwatch Scarecrow
 * {6}
 * Artifact Creature — Scarecrow
 * 4 / 4
 *
 * This creature has wither as long as you control a green creature. (It deals damage to creatures
 * in the form of -1/-1 counters.)
 * This creature has vigilance as long as you control a white creature.
 *
 * - Two independent conditional statics, one per granted keyword — the clauses have separate
 *   conditions and either can be on without the other.
 * - [GroupFilter.source] scopes each grant to this creature alone, and the conditions re-evaluate
 *   continuously: the keyword switches off the instant the last green or white creature leaves.
 * - The colour checks read *creatures you control*, not permanents; a green artifact or land does
 *   not turn wither on. [GameObjectFilter] filtering reads projected colour, so an animated or
 *   colour-shifted creature counts.
 */
val ThornwatchScarecrow = card("Thornwatch Scarecrow") {
    manaCost = "{6}"
    typeLine = "Artifact Creature — Scarecrow"
    power = 4
    toughness = 4
    oracleText = "This creature has wither as long as you control a green creature. (It deals damage to creatures in the form of -1/-1 counters.)\n" +
        "This creature has vigilance as long as you control a white creature."

    // This creature has wither as long as you control a green creature.
    staticAbility {
        ability = GrantKeyword(Keyword.WITHER, GroupFilter.source())
        condition = Conditions.YouControl(GameObjectFilter.Creature.withColor(Color.GREEN))
    }

    // This creature has vigilance as long as you control a white creature.
    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.source())
        condition = Conditions.YouControl(GameObjectFilter.Creature.withColor(Color.WHITE))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "265"
        artist = "Chuck Lukacs"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/3489ac0a-cbdb-43d5-be73-6cb65ae54a20.jpg?1783942708"
    }
}
