package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Eject the Warp Core
 * {1}{B}
 * Instant
 *
 * Kicker—Sacrifice an artifact or creature.
 * Target creature gets -2/-2 until end of turn. If this spell was kicked, that creature gets -6/-6
 * until end of turn instead.
 *
 * Vicious Offering's shape with a wider sacrifice filter (Goblin Barrage's "artifact or ...").
 */
val EjectTheWarpCore = card("Eject the Warp Core") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Kicker—Sacrifice an artifact or creature. (You may sacrifice an artifact or creature in addition to any other costs as you cast this spell.)\n" +
        "Target creature gets -2/-2 until end of turn. If this spell was kicked, that creature gets -6/-6 until end of turn instead."

    keywordAbility(
        KeywordAbility.kicker(
            Costs.additional.SacrificePermanent(filter = GameObjectFilter.Artifact or GameObjectFilter.Creature)
        )
    )

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = WasKicked,
            then = Effects.ModifyStats(-6, -6, t, Duration.EndOfTurn),
            otherwise = Effects.ModifyStats(-2, -2, t, Duration.EndOfTurn)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Pierre Raveneau"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04a479be-8093-4775-a5d9-8b0e1683bf12.jpg?1785981475"
    }
}
