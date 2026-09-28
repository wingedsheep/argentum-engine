package com.wingedsheep.mtg.sets.definitions.mom.cards

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
 * Final Flourish
 * {1}{B}
 * Instant
 * Kicker—Sacrifice an artifact or creature.
 * Target creature gets -2/-2 until end of turn. If this spell was kicked,
 * that creature gets -6/-6 until end of turn instead.
 */
val FinalFlourish = card("Final Flourish") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Kicker—Sacrifice an artifact or creature. (You may sacrifice an artifact or creature in addition to any other costs as you cast this spell.)\nTarget creature gets -2/-2 until end of turn. If this spell was kicked, that creature gets -6/-6 until end of turn instead."

    keywordAbility(
        KeywordAbility.kicker(
            Costs.additional.SacrificePermanent(
                filter = GameObjectFilter.Artifact.or(GameObjectFilter.Creature)
            )
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
        collectorNumber = "104"
        artist = "Raluca Marinescu"
        flavorText = "Strixhaven braced for another Oriq assault, but something far worse arrived."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09a4dcdb-6939-4d9f-8921-549e0ddc9f63.jpg?1783917010"
    }
}
