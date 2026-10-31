package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Insatiable Appetite — Throne of Eldraine #162
 * {1}{G} · Instant
 *
 * You may sacrifice a Food. If you do, target creature gets +5/+5 until end of turn. Otherwise,
 * that creature gets +3/+3 until end of turn.
 *
 * The target is chosen on cast; the sacrifice is a resolution-time choice (2019-10-04 ruling), so
 * it's an [Effects.MayPay] gate over `SacrificeOwn(Food)` as on Giant Opportunity. The gate only
 * offers "yes" while you control a Food; declining or being unable to pay runs the +3/+3 branch.
 */
val InsatiableAppetite = card("Insatiable Appetite") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "You may sacrifice a Food. If you do, target creature gets +5/+5 until end of turn. " +
        "Otherwise, that creature gets +3/+3 until end of turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.MayPay(
            cost = Effects.SacrificeOwn(GameObjectFilter.Any.withSubtype("Food")),
            then = Effects.ModifyStats(5, 5, creature),
            otherwise = Effects.ModifyStats(3, 3, creature),
            descriptionOverride = "You may sacrifice a Food. If you do, target creature gets +5/+5 " +
                "until end of turn. Otherwise, that creature gets +3/+3 until end of turn.",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "162"
        artist = "Nils Hamm"
        flavorText = "He ate them out of house and home and shed and barn and flock and herd."
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2357f2db-00c2-41f6-bd04-93f905dea461.jpg?1783932609"

        ruling(
            "2019-10-04",
            "You choose a target creature as you cast Insatiable Appetite, but you don't choose whether " +
                "to sacrifice a Food until the spell resolves."
        )
        ruling(
            "2019-10-04",
            "If the target creature is an illegal target by the time Insatiable Appetite tries to " +
                "resolve, the spell won't resolve. You won't sacrifice a Food."
        )
        ruling(
            "2024-11-08",
            "If an effect refers to a Food, it means any Food artifact, not just a Food artifact token."
        )
    }
}
