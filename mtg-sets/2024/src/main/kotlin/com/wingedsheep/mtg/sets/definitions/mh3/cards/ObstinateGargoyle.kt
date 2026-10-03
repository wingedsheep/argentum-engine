package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.conditions.SourceIsModified
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Obstinate Gargoyle (MH3 #195)
 * {1}{W}{B}
 * Artifact Creature — Gargoyle
 * 2/2
 * This creature has flying as long as it's modified. (Equipment, Auras you control, and counters
 * are modifications.)
 * Persist
 *
 * - "As long as it's modified" is [SourceIsModified] (CR 700.9) gating a self-grant of flying, the
 *   same shape as Skyward Spider.
 * - Persist is engine-live ([Keyword.PERSIST] is read by the death-trigger detector). The persist
 *   return brings it back with a -1/-1 counter, which makes it modified — so the returned
 *   Gargoyle flies.
 */
val ObstinateGargoyle = card("Obstinate Gargoyle") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Artifact Creature — Gargoyle"
    power = 2
    toughness = 2
    oracleText = "This creature has flying as long as it's modified. (Equipment, Auras you control, and counters are modifications.)\n" +
        "Persist (When this creature dies, if it had no -1/-1 counters on it, return it to the battlefield under its owner's control with a -1/-1 counter on it.)"

    // This creature has flying as long as it's modified.
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FLYING, GroupFilter.source()),
            condition = SourceIsModified
        )
    }

    keywords(Keyword.PERSIST)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "195"
        artist = "Craig J Spearing"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40cf39f2-7382-405d-a14b-7eb8726cd38a.jpg?1783911248"
        ruling("2024-06-07", "Once Obstinate Gargoyle has been blocked, causing it to gain flying by modifying it won't cause it to stop being blocked.")
        ruling("2024-06-07", "An Aura controlled by another player does not cause a creature you control to be modified.")
        ruling("2024-06-07", "A creature with a counter on it is considered modified no matter what kind of counter it is or which player put it on that creature.")
        ruling("2024-06-07", "A creature that is equipped is considered modified no matter who controls the Equipment that's attached to it.")
    }
}
