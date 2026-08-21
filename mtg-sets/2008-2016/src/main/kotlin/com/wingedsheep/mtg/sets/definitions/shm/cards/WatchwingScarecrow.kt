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
 * Watchwing Scarecrow
 * {4}
 * Artifact Creature — Scarecrow
 * 2 / 4
 *
 * This creature has vigilance as long as you control a white creature.
 * This creature has flying as long as you control a blue creature.
 *
 * - Two independent conditional statics, one per granted keyword — the clauses have separate
 *   conditions and either can be on without the other.
 * - [GroupFilter.source] scopes each grant to this creature alone, and the conditions re-evaluate
 *   continuously: losing your last blue creature takes flying straight back off, mid-combat
 *   included (an already-declared block stays legal — blocking restrictions are only checked as
 *   blockers are declared).
 * - The colour checks read *creatures you control*, not permanents; a blue artifact or land does
 *   not turn flying on. [GameObjectFilter] filtering reads projected colour, so an animated or
 *   colour-shifted creature counts.
 */
val WatchwingScarecrow = card("Watchwing Scarecrow") {
    manaCost = "{4}"
    typeLine = "Artifact Creature — Scarecrow"
    power = 2
    toughness = 4
    oracleText = "This creature has vigilance as long as you control a white creature.\n" +
        "This creature has flying as long as you control a blue creature."

    // This creature has vigilance as long as you control a white creature.
    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.source())
        condition = Conditions.YouControl(GameObjectFilter.Creature.withColor(Color.WHITE))
    }

    // This creature has flying as long as you control a blue creature.
    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, GroupFilter.source())
        condition = Conditions.YouControl(GameObjectFilter.Creature.withColor(Color.BLUE))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "268"
        artist = "Chuck Lukacs"
        flavorText = "The wings are held in place by wicker rods. The rods are held in place by pure faith."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c7774fd-3460-46e3-9c9c-7aa70f2ff6d8.jpg?1783942708"
    }
}
