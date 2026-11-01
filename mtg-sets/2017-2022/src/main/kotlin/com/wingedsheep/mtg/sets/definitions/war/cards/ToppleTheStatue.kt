package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Topple the Statue — War of the Spark #35 (canonical printing)
 * {2}{W} · Instant
 *
 * Tap target permanent. If it's an artifact, destroy it.
 * Draw a card.
 *
 * Artifact-ness is checked at resolution, after the tap (Dispatch's shape with a target-matches
 * condition). The draw happens whenever the target is still legal, artifact or not.
 */
val ToppleTheStatue = card("Topple the Statue") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Tap target permanent. If it's an artifact, destroy it.\nDraw a card."

    spell {
        val t = target(TargetFilter.Permanent)
        effect = Effects.Tap(t) then Effects.If(
            Conditions.TargetMatchesFilter(GameObjectFilter.Artifact, t),
            Effects.Destroy(t),
            descriptionOverride = "If it's an artifact, destroy it"
        ) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "35"
        artist = "Sidharth Chaturvedi"
        flavorText = "Bolas cared little when his statue fell, thinking it a useful distraction, " +
            "underestimating the surge of resolve it gave his opponents."
        imageUri = "https://cards.scryfall.io/normal/front/2/8/2829fa40-92e4-4017-aacd-9d9feae04aa2.jpg?1783933473"

        ruling(
            "2020-08-07",
            "If the target permanent is an illegal target when Topple the Statue tries to resolve, it doesn't " +
                "resolve. You won't draw a card. If the target is legal but isn't tapped, isn't destroyed, or is " +
                "neither tapped nor destroyed, you do draw a card."
        )
    }
}
