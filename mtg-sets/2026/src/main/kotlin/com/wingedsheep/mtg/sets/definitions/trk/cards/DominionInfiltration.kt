package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dominion Infiltration
 * {X}{R}{R}{R}
 * Sorcery
 * Choose X target creatures your opponents control. For each of them, create a token that's a copy
 * of that creature, except it has changeling. Those tokens gain haste. Exile those tokens at the
 * beginning of the next end step.
 *
 *  - `targets(…, exactly = X)` makes the announced X the target count (Doppelgang's shape); X = 0
 *    is a legal, empty cast. `ForEachTarget` skips a target that became illegal in response.
 *  - Changeling is a copy exception (`CopyExceptions.addedKeywords`), so it is part of the token's
 *    copiable values and lands in its base keywords — which is what makes it every creature type.
 *  - Haste is *gained*, not copiable: it is granted through the `CreatedTokens` collection the copy
 *    publishes, with no duration, so a later copy of the token doesn't inherit it.
 *  - `exileAtStep = Step.END` exiles them at the next end step, whoever's turn it is.
 */
val DominionInfiltration = card("Dominion Infiltration") {
    manaCost = "{X}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose X target creatures your opponents control. For each of them, create a token " +
        "that's a copy of that creature, except it has changeling. Those tokens gain haste. Exile those " +
        "tokens at the beginning of the next end step. (They're every creature type.)"

    spell {
        targets(TargetFilter.CreatureOpponentControls, exactly = DynamicAmounts.xValue())
        effect = Effects.ForEachTarget(
            Effects.CreateTokenCopyOfTarget(
                target = EffectTarget.ContextTarget(0),
                exceptions = CopyExceptions(addedKeywords = setOf(Keyword.CHANGELING)),
                exileAtStep = Step.END,
            ),
            Effects.ForEachInCollection(
                CollectionSlot.CreatedTokens,
                Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.Permanent),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "147"
        artist = "Jonathan Bartlett"
        flavorText = "There were only four changelings on Earth, but fear of them made a police state " +
            "out of paradise."
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38b7cc7c-e6f0-4460-83c0-0bcd1ab5b4cf.jpg?1793274258"
    }
}
