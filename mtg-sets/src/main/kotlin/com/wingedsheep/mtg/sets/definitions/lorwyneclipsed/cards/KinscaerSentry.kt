package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Kinscaer Sentry
 * {1}{W}
 * Creature — Kithkin Soldier
 * 2/2
 *
 * First strike, lifelink
 * Whenever this creature attacks, you may put a creature card with mana value X
 * or less from your hand onto the battlefield tapped and attacking, where X is
 * the number of attacking creatures you control.
 */
val KinscaerSentry = card("Kinscaer Sentry") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Kithkin Soldier"
    power = 2
    toughness = 2
    oracleText = "First strike, lifelink\nWhenever this creature attacks, you may put a creature card with mana value X or less from your hand onto the battlefield tapped and attacking, where X is the number of attacking creatures you control."

    keywords(Keyword.FIRST_STRIKE, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = CompositeEffect(listOf(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Creature),
                storeAs = "handCreatures"
            ),
            FilterCollectionEffect(
                from = "handCreatures",
                filter = CollectionFilter.ManaValueAtMost(DynamicAmounts.attackingCreaturesYouControl()),
                storeMatching = "eligible"
            ),
            SelectFromCollectionEffect(
                from = "eligible",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                storeSelected = "chosen",
                prompt = "Put a creature card onto the battlefield tapped and attacking",
                selectedLabel = "Put onto the battlefield tapped and attacking"
            ),
            MoveCollectionEffect(
                from = "chosen",
                destination = CardDestination.ToZone(Zone.BATTLEFIELD, Player.You, ZonePlacement.TappedAndAttacking)
            )
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Kev Fang"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/333bf101-14e8-4753-99bc-9174f42c4122.jpg?1767951770"

        ruling("2025-11-17", "Although the creatures put onto the battlefield with Kinscaer Sentry enter as attacking creatures, they were never declared as attacking creatures. Abilities that trigger whenever a creature attacks won't trigger when the creatures enter attacking.")
        ruling("2025-11-17", "The value of X is calculated only once, as Kinscaer Sentry's last ability resolves.")
        ruling("2025-11-17", "If a card in your hand has {X} in its mana cost, X is 0 for the purpose of determining its mana value.")
        ruling("2025-11-17", "You choose the player, planeswalker, or battle the creature you put onto the battlefield is attacking. It doesn't have to attack the same player, planeswalker, or battle as Kinscaer Sentry.")
    }
}
