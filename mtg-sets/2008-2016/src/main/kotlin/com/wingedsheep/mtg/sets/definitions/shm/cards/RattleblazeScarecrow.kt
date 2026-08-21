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
 * Rattleblaze Scarecrow
 * {6}
 * Artifact Creature — Scarecrow
 * 5 / 3
 *
 * This creature has persist as long as you control a black creature. (When this creature dies, if
 * it had no -1/-1 counters on it, return it to the battlefield under its owner's control with a
 * -1/-1 counter on it.)
 * This creature has haste as long as you control a red creature.
 *
 * - Two independent conditional statics, one per granted keyword — the clauses have separate
 *   conditions and either can be on without the other.
 * - Persist is engine-live ([Keyword.PERSIST] is read by the death-trigger detector), so granting
 *   the keyword is the whole implementation. What matters is *when* the grant is checked: persist
 *   is a leaves-the-battlefield trigger, so the game uses the Scarecrow's last known information —
 *   it persists only if you controlled a black creature as it died.
 * - The colour checks read *creatures you control*, not permanents; a black artifact or land does
 *   not turn the ability on. [GameObjectFilter] filtering reads projected colour, so an animated
 *   or colour-shifted creature counts.
 */
val RattleblazeScarecrow = card("Rattleblaze Scarecrow") {
    manaCost = "{6}"
    typeLine = "Artifact Creature — Scarecrow"
    power = 5
    toughness = 3
    oracleText = "This creature has persist as long as you control a black creature. (When this creature dies, if it had no -1/-1 counters on it, return it to the battlefield under its owner's control with a -1/-1 counter on it.)\n" +
        "This creature has haste as long as you control a red creature."

    // This creature has persist as long as you control a black creature.
    staticAbility {
        ability = GrantKeyword(Keyword.PERSIST, GroupFilter.source())
        condition = Conditions.YouControl(GameObjectFilter.Creature.withColor(Color.BLACK))
    }

    // This creature has haste as long as you control a red creature.
    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, GroupFilter.source())
        condition = Conditions.YouControl(GameObjectFilter.Creature.withColor(Color.RED))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "259"
        artist = "Trevor Hairsine"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b46425f-516c-42f5-8df1-79af17d02a4a.jpg?1783942710"
    }
}
