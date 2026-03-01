package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crippling Chill
 * {2}{U}
 * Instant
 * Tap target creature. It doesn't untap during its controller's next untap step.
 * Draw a card.
 */
val CripplingChill = card("Crippling Chill") {
    manaCost = "{2}{U}"
    typeLine = "Instant"
    oracleText = "Tap target creature. It doesn't untap during its controller's next untap step.\nDraw a card."

    spell {
        target = Targets.Creature
        effect = Effects.Tap(EffectTarget.ContextTarget(0)) then
            GrantKeywordUntilEndOfTurnEffect(AbilityFlag.DOESNT_UNTAP.name, EffectTarget.ContextTarget(0), Duration.UntilYourNextTurn) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "35"
        artist = "Torstein Nordstrand"
        flavorText = "\"In the silence of the ice, even dreams become still.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acf53a79-7573-43c2-bd3a-93abea58ba80.jpg?1562791864"
    }
}
