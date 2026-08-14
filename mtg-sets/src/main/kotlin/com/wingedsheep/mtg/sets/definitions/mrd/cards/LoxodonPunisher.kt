package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Loxodon Punisher — Mirrodin #14
 * {3}{W} · Creature — Elephant Soldier · 2/2
 *
 * This creature gets +2/+2 for each Equipment attached to it.
 *
 * The Winter Soldier shape: a [GrantDynamicStatsEffect] over [GroupFilter.source] whose bonus is
 * `Multiply(equipmentAttachedToSelf(), 2)`. The attachment count is read off *projected* subtypes,
 * so a permanent that becomes — or stops being — an Equipment is counted correctly, Auras and
 * Fortifications are excluded, and the bonus recomputes continuously as Equipment is attached or
 * falls off rather than being a snapshot taken at equip time.
 */
val LoxodonPunisher = card("Loxodon Punisher") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elephant Soldier"
    power = 2
    toughness = 2
    oracleText = "This creature gets +2/+2 for each Equipment attached to it."

    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmount.Multiply(DynamicAmounts.equipmentAttachedToSelf(), 2),
            toughnessBonus = DynamicAmount.Multiply(DynamicAmounts.equipmentAttachedToSelf(), 2)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Terese Nielsen"
        flavorText = "The loxodons believe punishment comes in two steps: pain and atonement. " +
            "They carry a weapon for each."
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df2b90b8-8306-4543-b9f4-3cfd033f5ca5.jpg?1783944561"
    }
}
