package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Shadow Urchin
 * {2}{B/R}
 * Creature — Ouphe
 * 3/4
 *
 * Whenever this creature attacks, blight 1.
 * (Put a -1/-1 counter on a creature you control.)
 * Whenever a creature you control with one or more counters on it dies, exile that many
 * cards from the top of your library. Until your next end step, you may play those cards.
 */
val ShadowUrchin = card("Shadow Urchin") {
    manaCost = "{2}{B/R}"
    typeLine = "Creature — Ouphe"
    power = 3
    toughness = 4
    oracleText = "Whenever this creature attacks, blight 1. (Put a -1/-1 counter on a creature you control.)\n" +
        "Whenever a creature you control with one or more counters on it dies, exile that many cards " +
        "from the top of your library. Until your next end step, you may play those cards."

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = EffectPatterns.blight(1)
    }

    triggeredAbility {
        trigger = Triggers.YourCreatureDies
        triggerCondition = Compare(
            DynamicAmount.LastKnownTotalCounterCount,
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(1)
        )
        effect = CompositeEffect(listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.LastKnownTotalCounterCount),
                storeAs = "exiledCards"
            ),
            MoveCollectionEffect(
                from = "exiledCards",
                destination = CardDestination.ToZone(Zone.EXILE)
            ),
            GrantMayPlayFromExileEffect("exiledCards", untilEndOfNextTurn = true)
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "242"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e54c39b-6149-467b-a9a8-7ad09ca0cbd4.jpg?1767952415"
    }
}
