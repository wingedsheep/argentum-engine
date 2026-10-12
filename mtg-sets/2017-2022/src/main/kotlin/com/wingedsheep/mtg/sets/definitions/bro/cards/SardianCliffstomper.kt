package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sardian Cliffstomper — The Brothers' War #151 (canonical printing)
 * {1}{R} · Creature — Minotaur Barbarian · 0/4
 *
 * As long as it's your turn and you control four or more Mountains, this creature gets +X/+0,
 * where X is the number of Mountains you control.
 *
 * Magmatic Channeler's shape: a `ConditionalStaticAbility` over a self-scoped `GrantDynamicStats`.
 * The condition is the conjunction of the turn clause and the Mountain threshold; the bonus is a
 * live count, so it tracks Mountains entering or leaving while the condition holds. "Mountains" is
 * the land subtype, so nonbasic lands with the Mountain type count.
 */
val SardianCliffstomper = card("Sardian Cliffstomper") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Barbarian"
    power = 0
    toughness = 4
    oracleText = "As long as it's your turn and you control four or more Mountains, this creature " +
        "gets +X/+0, where X is the number of Mountains you control."

    val mountains = GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantDynamicStats(
                filter = GroupFilter.source(),
                powerBonus = DynamicAmounts.battlefield(Player.You, mountains).count(),
                toughnessBonus = DynamicAmounts.fixed(0),
            ),
            condition = Conditions.All(
                Conditions.IsYourTurn,
                Conditions.YouControlAtLeast(4, mountains),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "151"
        artist = "Joseph Weston"
        flavorText = "When Urza betrayed the Sardian dwarves, they called upon the might of their neighbors for aid."
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1c42d56-5b0e-4624-8831-77280f8d56cf.jpg?1783920062"
    }
}
