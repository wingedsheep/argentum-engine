package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Red Sun's Twilight
 * {X}{R}{R}
 * Sorcery
 * Destroy up to X target artifacts. If X is 5 or more, for each artifact destroyed this way,
 * create a token that's a copy of it. Those tokens gain haste. Exile them at the beginning of the
 * next end step.
 *
 * [moveTracked] with [MoveType.Destroy] records only the artifacts that actually went to the
 * graveyard, so an indestructible (or regenerated) artifact yields no token. Each token copies the
 * destroyed card's printed characteristics (Foggy Swamp Visions / Nahiri, the Unforgiving shape).
 */
val RedSunsTwilight = card("Red Sun's Twilight") {
    manaCost = "{X}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy up to X target artifacts. If X is 5 or more, for each artifact destroyed this way, create a token that's a copy of it. Those tokens gain haste. Exile them at the beginning of the next end step."

    spell {
        targets(TargetFilter.Artifact, optional = true, dynamicMaxCount = DynamicAmounts.xValue())
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val destroyed = moveTracked(
                chosen,
                CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = MoveType.Destroy,
            )
            run(
                Effects.If(
                    condition = Conditions.CompareAmounts(
                        DynamicAmounts.xValue(),
                        ComparisonOperator.GTE,
                        5
                    ),
                    then = Effects.ForEachInCollection(
                        collection = destroyed,
                        effect = Effects.CreateTokenCopyOfTarget(
                            target = EffectTarget.IterationEntity,
                            addedKeywords = setOf(Keyword.HASTE),
                            exileAtStep = Step.END,
                        ),
                    )
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "145"
        artist = "Julian Kok Joon Wen"
        flavorText = "\"Where once there was apathy, Urabrask brought passion, and the Great Work truly began.\"\n—Monument inscription"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7cac1827-69af-469d-b88c-fb9f2f33866a.jpg?1783918024"
        ruling("2023-02-04", "If X is 5 or more and any of the target artifacts become illegal, you create a token only for each of the artifacts that is still a legal target.")
        ruling("2023-02-04", "Each token gains haste after it has been created. If something copies one of these tokens, the copy won't have haste, and you won't exile it at the beginning of the next end step.")
        ruling("2023-02-04", "If the copied artifact is a token, the token that's created copies the original characteristics of that token as stated by the effect that created the token.")
    }
}
