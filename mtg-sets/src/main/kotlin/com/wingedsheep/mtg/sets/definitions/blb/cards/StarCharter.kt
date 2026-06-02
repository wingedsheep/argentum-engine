package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Star Charter {3}{W}
 * Creature — Bat Cleric
 * 3/1
 *
 * Flying
 * At the beginning of your end step, if you gained or lost life this turn,
 * look at the top four cards of your library. You may reveal a creature card
 * with power 3 or less from among them and put it into your hand. Put the rest
 * on the bottom of your library in a random order.
 */
val StarCharter = card("Star Charter") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bat Cleric"
    oracleText = "Flying\nAt the beginning of your end step, if you gained or lost life this turn, look at the top four cards of your library. You may reveal a creature card with power 3 or less from among them and put it into your hand. Put the rest on the bottom of your library in a random order."
    power = 3
    toughness = 1

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.YouGainedOrLostLifeThisTurn
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(4)),
                    storeAs = "looked"
                ),
                SelectFromCollectionEffect(
                    from = "looked",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    filter = GameObjectFilter.Creature.powerAtMost(3),
                    storeSelected = "kept",
                    storeRemainder = "rest",
                    prompt = "You may reveal a creature card with power 3 or less and put it into your hand",
                    showAllCards = true
                ),
                MoveCollectionEffect(
                    from = "kept",
                    destination = CardDestination.ToZone(Zone.HAND)
                ),
                MoveCollectionEffect(
                    from = "rest",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                    order = CardOrder.Random
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "33"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e209237-00f7-4bf0-8287-ccde02ce8e8d.jpg?1721425964"
    }
}
