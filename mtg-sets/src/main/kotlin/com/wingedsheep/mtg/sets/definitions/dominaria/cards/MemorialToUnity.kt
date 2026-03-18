package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Memorial to Unity
 * Land
 * This land enters tapped.
 * {T}: Add {G}.
 * {2}{G}, {T}, Sacrifice this land: Look at the top five cards of your library.
 * You may reveal a creature card from among them and put it into your hand.
 * Then put the rest on the bottom of your library in a random order.
 */
val MemorialToUnity = card("Memorial to Unity") {
    typeLine = "Land"
    oracleText = "Memorial to Unity enters the battlefield tapped.\n{T}: Add {G}.\n{2}{G}, {T}, Sacrifice Memorial to Unity: Look at the top five cards of your library. You may reveal a creature card from among them and put it into your hand. Then put the rest on the bottom of your library in a random order."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}"), Costs.Tap, Costs.SacrificeSelf)
        effect = CompositeEffect(listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(5)),
                storeAs = "looked"
            ),
            SelectFromCollectionEffect(
                from = "looked",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                filter = GameObjectFilter.Creature,
                storeSelected = "kept",
                storeRemainder = "rest",
                selectedLabel = "Put in hand",
                remainderLabel = "Put on bottom"
            ),
            MoveCollectionEffect(
                from = "kept",
                destination = CardDestination.ToZone(Zone.HAND)
            ),
            MoveCollectionEffect(
                from = "rest",
                destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom)
            )
        ))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "245"
        artist = "Cliff Childs"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/994d903b-0d54-4af4-898f-213e716e9ed4.jpg?1562740012"
    }
}
