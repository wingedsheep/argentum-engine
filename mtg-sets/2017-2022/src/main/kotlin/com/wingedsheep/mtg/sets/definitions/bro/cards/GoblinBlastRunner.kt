package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Goblin Blast-Runner
 * {R}
 * Creature — Goblin
 * 1/2
 * This creature gets +2/+0 and has menace as long as you sacrificed a permanent this turn.
 *
 * One printed ability spanning two layers (P/T in 7c, keywords in 6), bundled in a
 * [CompositeStaticAbility] under one [ConditionalStaticAbility] — CR 613.6. The condition reads
 * the controller-scoped turn tracker, so it counts sacrifices made before this creature was on
 * the battlefield (per the ruling) and ignores opponents' sacrifices.
 */
val GoblinBlastRunner = card("Goblin Blast-Runner") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    power = 1
    toughness = 2
    oracleText = "This creature gets +2/+0 and has menace as long as you sacrificed a permanent this turn."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CompositeStaticAbility(
                listOf(
                    ModifyStats(powerBonus = 2, toughnessBonus = 0, filter = GroupFilter.source()),
                    GrantKeyword(Keyword.MENACE, GroupFilter.source())
                )
            ),
            condition = Conditions.YouSacrificedPermanentsThisTurn()
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "137"
        artist = "Joseph Weston"
        flavorText = "Most goblins can light a fuse and run, but only the cleverest remember to drop the bomb."
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd77c575-93a4-44ec-8940-adcbfc6e7a84.jpg?1783920067"
        ruling(
            "2022-10-14",
            "Goblin Blast-Runner doesn't need to have been on the battlefield at the time the " +
                "permanent was sacrificed in order to get the bonus."
        )
    }
}
