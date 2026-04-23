package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Ajani, Outland Chaperone
 * {1}{W}{W}
 * Legendary Planeswalker — Ajani
 * Starting Loyalty: 3
 *
 * +1: Create a 1/1 green and white Kithkin creature token.
 * −2: Ajani deals 4 damage to target tapped creature.
 * −8: Look at the top X cards of your library, where X is your life total. You may put
 *     any number of nonland permanent cards with mana value 3 or less from among them
 *     onto the battlefield. Then shuffle.
 */
val AjaniOutlandChaperone = card("Ajani, Outland Chaperone") {
    manaCost = "{1}{W}{W}"
    typeLine = "Legendary Planeswalker — Ajani"
    startingLoyalty = 3
    oracleText = "+1: Create a 1/1 green and white Kithkin creature token.\n" +
        "−2: Ajani deals 4 damage to target tapped creature.\n" +
        "−8: Look at the top X cards of your library, where X is your life total. " +
        "You may put any number of nonland permanent cards with mana value 3 or less " +
        "from among them onto the battlefield. Then shuffle."

    // +1: Create a 1/1 green and white Kithkin creature token.
    loyaltyAbility(+1) {
        effect = CreateTokenEffect(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN, Color.WHITE),
            creatureTypes = setOf("Kithkin"),
            imageUri = "https://cards.scryfall.io/normal/front/2/e/2ed11e1b-2289-48d2-8d96-ee7e590ecfd4.jpg?1767955680"
        )
    }

    // −2: Ajani deals 4 damage to target tapped creature.
    loyaltyAbility(-2) {
        val tapped = target("creature", Targets.TappedCreature)
        effect = Effects.DealDamage(4, tapped)
    }

    // −8: Look at the top X cards of your library, where X is your life total. You may
    // put any number of nonland permanent cards with mana value 3 or less from among
    // them onto the battlefield. Then shuffle.
    loyaltyAbility(-8) {
        effect = CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.YourLifeTotal),
                    storeAs = "looked"
                ),
                SelectFromCollectionEffect(
                    from = "looked",
                    selection = SelectionMode.ChooseAnyNumber,
                    filter = GameObjectFilter.NonlandPermanent.manaValueAtMost(3),
                    showAllCards = true,
                    storeSelected = "toBattlefield",
                    storeRemainder = "rest",
                    prompt = "You may put any number of nonland permanent cards with mana value 3 or less onto the battlefield",
                    selectedLabel = "Put onto the battlefield",
                    remainderLabel = "Shuffle into your library"
                ),
                MoveCollectionEffect(
                    from = "toBattlefield",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                ),
                MoveCollectionEffect(
                    from = "rest",
                    destination = CardDestination.ToZone(Zone.LIBRARY)
                ),
                ShuffleLibraryEffect()
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "4"
        artist = "Daren Bader"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/6124a691-ae83-4d22-a177-0aee65b47064.jpg?1767951721"
    }
}
