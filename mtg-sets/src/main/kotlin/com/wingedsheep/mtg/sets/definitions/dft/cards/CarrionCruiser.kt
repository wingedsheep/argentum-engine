package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Carrion Cruiser — Aetherdrift #78
 * {2}{B} · Artifact — Vehicle · 3/2
 *
 * When this Vehicle enters, mill two cards. Then return a creature or Vehicle card from your
 * graveyard to your hand.
 * Crew 1
 *
 * The two clauses resolve in printed order (CR 608.2), so the mill runs first and the two cards it
 * just put into the graveyard are legal picks for the return — which is the whole point of the card.
 * "a creature or Vehicle card" is *not* a target, so the pick is a resolution-time choice over a
 * gathered graveyard pool rather than a `TargetRequirement`; targeting would have locked the choice
 * in when the trigger went on the stack, before the mill.
 *
 * The return is mandatory (no "may"), hence `chooseExactly(1)`: with an empty pool it selects
 * nothing and moves on, and with exactly one candidate it resolves without prompting.
 */
val CarrionCruiser = card("Carrion Cruiser") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Artifact — Vehicle"
    oracleText = "When this Vehicle enters, mill two cards. Then return a creature or Vehicle card " +
        "from your graveyard to your hand. (To mill two cards, put the top two cards of your library " +
        "into your graveyard.)\n" +
        "Crew 1 (Tap any number of creatures you control with total power 1 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Pipeline(
            descriptionOverride = "Mill two cards. Then return a creature or Vehicle card from your " +
                "graveyard to your hand."
        ) {
            // "mill two cards, ..."
            run(Patterns.Library.mill(2))
            // "... Then return a creature or Vehicle card from your graveyard to your hand."
            val graveyard = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.CreatureOrVehicle)
            )
            val chosen = chooseExactly(
                1,
                from = graveyard,
                prompt = "Return a creature or Vehicle card from your graveyard to your hand",
                selectedLabel = "Return to hand"
            )
            toHand(chosen)
        }
    }

    keywordAbility(KeywordAbility.crew(1))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc00fdee-3d24-4360-a4d2-ffd3c08a462d.jpg?1783907898"
    }
}
