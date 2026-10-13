package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shadow Prophecy
 * {2}{B}
 * Instant
 * Domain — Look at the top X cards of your library, where X is the number of basic land types
 * among lands you control. Put up to two of them into your hand and the rest into your
 * graveyard. You lose 2 life.
 *
 * X is [DynamicAmounts.domain], counted at resolution. "Up to two" is a
 * [SelectionMode.ChooseUpTo] over every looked-at card (unfiltered), so taking zero or one is
 * legal; whatever is not taken goes to the graveyard. The life loss happens regardless of X —
 * with no basic land types you look at nothing and still lose 2 life.
 */
val ShadowProphecy = card("Shadow Prophecy") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Domain — Look at the top X cards of your library, where X is the number of basic land types among lands you control. Put up to two of them into your hand and the rest into your graveyard. You lose 2 life."

    spell {
        effect = Patterns.Library.lookAtTopAndTakeMatching(
            count = DynamicAmounts.domain(),
            filter = GameObjectFilter.Any,
            prompt = "Put up to two of them into your hand",
            selection = SelectionMode.ChooseUpTo(DynamicAmounts.fixed(2)),
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.GRAVEYARD),
            restOrder = CardOrder.Preserve
        ) then Effects.LoseLife(2, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Rovina Cai"
        flavorText = "\"Lord Windgrace, grant me wisdom.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb078448-9e9f-4994-8aff-7e7c4fb62efc.jpg?1783921327"
    }
}
