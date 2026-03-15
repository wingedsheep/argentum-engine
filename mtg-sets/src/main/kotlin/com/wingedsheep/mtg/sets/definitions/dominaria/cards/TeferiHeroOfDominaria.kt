package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.CreatePermanentGlobalTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.TapUntapCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Teferi, Hero of Dominaria
 * {3}{W}{U}
 * Legendary Planeswalker — Teferi
 * Starting Loyalty: 4
 *
 * +1: Draw a card. At the beginning of the next end step, untap up to two lands.
 * −3: Put target nonland permanent into its owner's library third from the top.
 * −8: You get an emblem with "Whenever you draw a card, exile target permanent an opponent controls."
 */
val TeferiHeroOfDominaria = card("Teferi, Hero of Dominaria") {
    manaCost = "{3}{W}{U}"
    typeLine = "Legendary Planeswalker — Teferi"
    startingLoyalty = 4
    oracleText = "+1: Draw a card. At the beginning of the next end step, untap up to two lands.\n\u22123: Put target nonland permanent into its owner's library third from the top.\n\u22128: You get an emblem with \"Whenever you draw a card, exile target permanent an opponent controls.\""

    // +1: Draw a card. At the beginning of the next end step, untap up to two lands.
    loyaltyAbility(+1) {
        effect = CompositeEffect(
            listOf(
                Effects.DrawCards(1),
                CreateDelayedTriggerEffect(
                    step = Step.END,
                    effect = CompositeEffect(
                        listOf(
                            GatherCardsEffect(
                                source = CardSource.ControlledPermanents(Player.You, GameObjectFilter.Land),
                                storeAs = "lands"
                            ),
                            SelectFromCollectionEffect(
                                from = "lands",
                                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(2)),
                                storeSelected = "toUntap"
                            ),
                            TapUntapCollectionEffect(
                                collectionName = "toUntap",
                                tap = false
                            )
                        )
                    )
                )
            )
        )
    }

    // −3: Put target nonland permanent into its owner's library third from the top.
    loyaltyAbility(-3) {
        val nonland = target("nonland", Targets.NonlandPermanent)
        effect = Effects.PutIntoLibraryNthFromTop(nonland, positionFromTop = 2)
    }

    // −8: You get an emblem with "Whenever you draw a card, exile target permanent an opponent controls."
    loyaltyAbility(-8) {
        effect = CreatePermanentGlobalTriggeredAbilityEffect(
            ability = TriggeredAbility.create(
                trigger = Triggers.YouDraw.event,
                binding = Triggers.YouDraw.binding,
                effect = Effects.Exile(EffectTarget.ContextTarget(0)),
                targetRequirement = Targets.PermanentOpponentControls
            ),
            descriptionOverride = "Whenever you draw a card, exile target permanent an opponent controls."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "207"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d10b752-d9cb-419d-a5c4-d4ee1acb655e.jpg?1562736365"
        ruling("2018-04-27", "You don't decide which two lands to untap until the next end step.")
        ruling("2018-04-27", "You choose the target for the triggered ability of Teferi's emblem after you've seen the card you drew.")
    }
}
