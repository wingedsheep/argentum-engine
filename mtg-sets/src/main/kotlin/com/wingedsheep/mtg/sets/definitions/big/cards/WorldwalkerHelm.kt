package com.wingedsheep.mtg.sets.definitions.big.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreateAdditionalToken
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.events.ControllerFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Worldwalker Helm
 * {2}{U}
 * Artifact
 *
 * If you would create one or more artifact tokens, instead create those tokens
 * plus an additional Map token. (It's an artifact with "{1}, {T}, Sacrifice this
 * token: Target creature you control explores. Activate only as a sorcery.")
 * {1}{U}, {T}: Create a token that's a copy of target artifact token you control.
 */
val WorldwalkerHelm = card("Worldwalker Helm") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "If you would create one or more artifact tokens, instead create those tokens plus an additional Map token. " +
        "(It's an artifact with \"{1}, {T}, Sacrifice this token: Target creature you control explores. Activate only as a sorcery.\")\n" +
        "{1}{U}, {T}: Create a token that's a copy of target artifact token you control."

    // "If you would create one or more artifact tokens, instead create those tokens
    // plus an additional Map token." Per the ruling this applies to ALL artifact
    // tokens you create (not just Map tokens) and adds one Map per creation event.
    replacementEffect(
        CreateAdditionalToken(
            additionalTokenType = "Map",
            additionalTokenCount = 1,
            // Per the ruling, creation-effect properties (e.g. "tapped") apply to both the
            // original tokens and the added Map.
            inheritTapped = true,
            appliesTo = EventPattern.TokenCreationEvent(
                controller = ControllerFilter.You,
                tokenFilter = GameObjectFilter.Artifact,
            ),
        )
    )

    // "{1}{U}, {T}: Create a token that's a copy of target artifact token you control."
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.Tap)
        val artifactToken = target(
            "target artifact token you control",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.Artifact.token().youControl())),
        )
        effect = Effects.CreateTokenCopyOfTarget(artifactToken)
        timing = TimingRule.InstantSpeed
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "7"
        artist = "Camille Alquier"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b74ad496-05bc-4c5a-9027-b14df9c387ab.jpg?1739804167"
    }
}
