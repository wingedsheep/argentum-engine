package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Upriser Renegade — Kamigawa: Neon Dynasty #170 (canonical printing)
 * {1}{R} · Creature — Human Samurai · 1/3
 *
 * This creature gets +2/+0 for each other modified creature you control.
 *
 * "Modified" is [StatePredicate.IsModified] (CR 700.9: a counter, an Equipment, or an Aura its
 * controller controls); `excludeSelf` drops the Renegade itself from the count, so modifying it
 * adds nothing. The count is re-read on every
 * projection, so the bonus tracks modifications as they come and go.
 */
val UpriserRenegade = card("Upriser Renegade") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Samurai"
    power = 1
    toughness = 3
    oracleText = "This creature gets +2/+0 for each other modified creature you control. " +
        "(Equipment, Auras you control, and counters are modifications.)"

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature.withStatePredicate(StatePredicate.IsModified),
                excludeSelf = true,
            ).count() * 2,
            toughnessBonus = DynamicAmounts.fixed(0),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "170"
        artist = "David Auden Nash"
        flavorText = "\"My streets, my rules. You picked the wrong neighborhood for a fight.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62d878c2-a340-4751-adaa-e50b0bb85423.jpg?1783923855"
    }
}
