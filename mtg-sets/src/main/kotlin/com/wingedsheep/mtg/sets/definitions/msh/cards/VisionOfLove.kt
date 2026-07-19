package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect

/**
 * Vision of Love (MSH #158) — {1}{R} Instant
 *
 * You may sacrifice an artifact or discard a card. If you do, draw two cards.
 *
 * Implementation notes:
 * - "You may … or …" is the Nimble Hobbit idiom: a [MayEffect] yes/no wrapping a
 *   [ChooseActionEffect] whose two [EffectChoice]s are the sacrifice and the discard. Each
 *   option carries a [FeasibilityCheck], so an option the controller can't perform is hidden
 *   (no artifact → only the discard is offered; neither → nothing happens and no cards are
 *   drawn), which is exactly the "If you do" gate.
 * - The draw therefore rides *inside* each branch rather than on an outer `IfYouDo`: the
 *   payoff is reachable only along a branch whose cost was actually paid, so a declined or
 *   impossible choice can never draw. (An outer action-outcome gate would have to score a
 *   sacrifice and a discard-pipeline with one criterion; keeping the draw per-branch is the
 *   faithful reading and needs no new SDK type.)
 */
val VisionOfLove = card("Vision of Love") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "You may sacrifice an artifact or discard a card. If you do, draw two cards."

    spell {
        effect = MayEffect(
            effect = ChooseActionEffect(
                choices = listOf(
                    EffectChoice(
                        label = "Sacrifice an artifact",
                        effect = SacrificeEffect(filter = GameObjectFilter.Artifact) then
                            Effects.DrawCards(2),
                        feasibilityCheck = FeasibilityCheck.ControlsPermanentMatching(
                            filter = GameObjectFilter.Artifact
                        ),
                    ),
                    EffectChoice(
                        label = "Discard a card",
                        effect = Patterns.Hand.discardCards(1) then Effects.DrawCards(2),
                        feasibilityCheck = FeasibilityCheck.HasCardsInZone(zone = Zone.HAND),
                    ),
                )
            ),
            descriptionOverride = "You may sacrifice an artifact or discard a card. " +
                "If you do, draw two cards.",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "158"
        artist = "Lixin Yin"
        flavorText = "Love transcends all barriers."
        imageUri = "https://cards.scryfall.io/normal/front/5/6/5604cee1-a2ea-48ed-88f4-6878bd053fc8.jpg?1783902922"
    }
}
