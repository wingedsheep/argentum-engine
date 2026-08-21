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
 * Wingrattle Scarecrow
 * {3}
 * Artifact Creature — Scarecrow
 * 2 / 2
 *
 * This creature has flying as long as you control a blue creature.
 * This creature has persist as long as you control a black creature. (When this creature dies, if
 * it had no -1/-1 counters on it, return it to the battlefield under its owner's control with a
 * -1/-1 counter on it.)
 *
 * - Two independent conditional statics, one per granted keyword — the clauses have separate
 *   conditions and either can be on without the other.
 * - Persist is engine-live ([Keyword.PERSIST] is read by the death-trigger detector), so granting
 *   the keyword is the whole implementation. Because persist is a leaves-the-battlefield trigger,
 *   the game uses last known information: the Scarecrow persists only if you controlled a black
 *   creature as it died.
 * - The colour checks read *creatures you control*, not permanents; a blue artifact or land does
 *   not turn flying on. [GameObjectFilter] filtering reads projected colour, so an animated or
 *   colour-shifted creature counts.
 */
val WingrattleScarecrow = card("Wingrattle Scarecrow") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Scarecrow"
    power = 2
    toughness = 2
    oracleText = "This creature has flying as long as you control a blue creature.\n" +
        "This creature has persist as long as you control a black creature. (When this creature dies, if it had no -1/-1 counters on it, return it to the battlefield under its owner's control with a -1/-1 counter on it.)"

    // This creature has flying as long as you control a blue creature.
    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, GroupFilter.source())
        condition = Conditions.YouControl(GameObjectFilter.Creature.withColor(Color.BLUE))
    }

    // This creature has persist as long as you control a black creature.
    staticAbility {
        ability = GrantKeyword(Keyword.PERSIST, GroupFilter.source())
        condition = Conditions.YouControl(GameObjectFilter.Creature.withColor(Color.BLACK))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "270"
        artist = "Trevor Hairsine"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61c5d86e-1ce1-4386-b70f-73b24351d6ae.jpg?1783942707"
    }
}
