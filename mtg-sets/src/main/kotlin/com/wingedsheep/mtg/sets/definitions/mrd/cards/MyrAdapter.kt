package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Myr Adapter — Mirrodin #210
 * {3} · Artifact Creature — Myr · 1/1
 *
 * This creature gets +1/+1 for each Equipment attached to it.
 *
 * The Champion of the Flame / Winter Soldier shape, restricted to Equipment: a
 * [GrantDynamicStatsEffect] over [GroupFilter.source] whose bonus is
 * [DynamicAmounts.equipmentAttachedToSelf] — the *Equipment-only* attachment count, so Auras
 * hung on the Adapter (Inertia Bubble, Relic Bane) don't feed it. The count is read from
 * projected subtypes and recomputed continuously, so the bonus tracks Equipment being attached,
 * unattached, or ceasing to be an Equipment mid-turn.
 *
 * Bonus, not base-setting: it stacks additively with anything the Equipment itself grants
 * (Bonesplitter's +2/+0 makes the Adapter a 4/2, not a 3/3), and applies in layer 7c alongside
 * every other +N/+N.
 */
val MyrAdapter = card("Myr Adapter") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Myr"
    power = 1
    toughness = 1
    oracleText = "This creature gets +1/+1 for each Equipment attached to it."

    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.equipmentAttachedToSelf(),
            toughnessBonus = DynamicAmounts.equipmentAttachedToSelf()
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "210"
        artist = "Ben Thompson"
        flavorText = "\"The simplest way to plan ahead is merely to be ready for everything.\"\n" +
            "—Pontifex, elder researcher"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d6ddde9-4427-4a0f-b05c-9cfd886aad2d.jpg?1783944512"
    }
}
