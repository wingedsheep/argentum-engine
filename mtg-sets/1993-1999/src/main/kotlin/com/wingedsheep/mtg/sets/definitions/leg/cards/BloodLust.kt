package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Blood Lust
 * {1}{R}
 * Instant
 *
 * If target creature has toughness 5 or greater, it gets +4/-4 until end of turn. Otherwise, it gets
 * +4/-X until end of turn, where X is its toughness minus 1.
 *
 * Both branches collapse to one toughness penalty of `min(4, toughness - 1)`: at toughness 5 or more
 * the minimum is 4, below it the minimum is toughness minus 1. The target's (projected) toughness is
 * read once as the spell resolves and the penalty is a fixed amount from then on — per the 2005
 * ruling, a later drop in toughness (Nightmare losing a Swamp) is not compensated for.
 */
val BloodLust = card("Blood Lust") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "If target creature has toughness 5 or greater, it gets +4/-4 until end of turn. " +
        "Otherwise, it gets +4/-X until end of turn, where X is its toughness minus 1."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(
            power = DynamicAmounts.fixed(4),
            toughness = -DynamicAmounts.min(
                DynamicAmounts.fixed(4),
                DynamicAmounts.toughnessOf(creature) - 1,
            ),
            target = creature,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "135"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fbbf1a9c-8b94-4ee7-92db-65b531149990.jpg?1783948059"
        ruling(
            "2005-11-01",
            "Has changed slightly in functionality, with the old continuous nature of the toughness " +
                "reduction gone away; the toughness change remains static. For example, if you have three " +
                "Swamps and Nightmare on the battlefield and then cast Blood Lust on it, it will become 7/1. " +
                "If one of your Swamps is subsequently destroyed, your Nightmare will become 6/0 and be put " +
                "in the graveyard."
        )
    }
}
