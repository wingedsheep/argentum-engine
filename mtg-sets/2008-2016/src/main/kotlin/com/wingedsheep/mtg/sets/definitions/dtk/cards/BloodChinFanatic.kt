package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Blood-Chin Fanatic
 * {1}{B}{B}
 * Creature — Orc Warrior
 * 3 / 3
 *
 * {1}{B}, Sacrifice another Warrior creature: Target player loses X life and you gain X life,
 * where X is the sacrificed creature's power.
 *
 * "Another Warrior creature" is [Costs.SacrificeAnother] (which excludes the Fanatic itself) over
 * the creature filter narrowed by subtype. Both halves read [DynamicAmounts.sacrificedPower] —
 * the sacrificed creature's last-known power snapshotted when the cost was paid, counters
 * included — so the gain is X, not "the life lost this way" ([Effects.DrainLife]'s shape).
 */
val BloodChinFanatic = card("Blood-Chin Fanatic") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Orc Warrior"
    power = 3
    toughness = 3
    oracleText = "{1}{B}, Sacrifice another Warrior creature: Target player loses X life and you gain X life, where X is the sacrificed creature's power."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{B}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature.withSubtype(Subtype.WARRIOR)),
        )
        val player = target(Targets.Player)
        effect = Effects.LoseLife(DynamicAmounts.sacrificedPower(), player) then
            Effects.GainLife(DynamicAmounts.sacrificedPower())
        description = "Target player loses X life and you gain X life, where X is the sacrificed Warrior's power."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "David Palumbo"
        flavorText = "\"We are as brutal as our dragonlord. We strike like wild lightning and feast on human flesh.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4eb00d55-de56-4bbb-9545-e8cfb25a4f23.jpg?1783938601"
        ruling("2015-02-25", "Use the creature's power as it last existed on the battlefield, including any +1/+1 counters it had, when determining the value of X.")
    }
}
