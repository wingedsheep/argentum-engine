package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
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
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Boughside Wanderers
 * {4}{G}{G}
 * Creature — Elf Scout
 * 4/4
 * When this creature enters, look at the top four cards of your library. You may reveal a permanent
 * card from among them and put it into your hand. Put the rest on the bottom of your library in a
 * random order.
 * Landfall — Whenever a land you control enters, this creature gets +2/+2 until end of turn.
 *
 * The dig is the Staunch Crewmate idiom: gather the top four, [SelectionMode.ChooseUpTo] one card
 * matching [CardPredicate.IsPermanent] ("you may reveal" — declining is legal even with hits
 * present), move the pick to hand `revealed = true`, and bottom the remainder with
 * [CardOrder.Random] so the leftovers aren't secretly ordered.
 */
val BoughsideWanderers = card("Boughside Wanderers") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Scout"
    oracleText = "When this creature enters, look at the top four cards of your library. You may " +
        "reveal a permanent card from among them and put it into your hand. Put the rest on the " +
        "bottom of your library in a random order.\n" +
        "Landfall — Whenever a land you control enters, this creature gets +2/+2 until end of turn."
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            GatherCardsEffect(CardSource.TopOfLibrary(DynamicAmount.Fixed(4)), storeAs = "looked"),
            SelectFromCollectionEffect(
                from = "looked",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                filter = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsPermanent)),
                storeSelected = "kept",
                storeRemainder = "rest",
                selectedLabel = "Put in hand",
                remainderLabel = "Put on bottom"
            ),
            MoveCollectionEffect(from = "kept", destination = CardDestination.ToZone(Zone.HAND), revealed = true),
            MoveCollectionEffect(
                from = "rest",
                destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                order = CardOrder.Random
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        description = "Landfall — Whenever a land you control enters, this creature gets +2/+2 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "121"
        artist = "Irina Nordsol"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71bec005-2925-4944-be16-2cc5eb30f5d6.jpg?1785497158"
    }
}
