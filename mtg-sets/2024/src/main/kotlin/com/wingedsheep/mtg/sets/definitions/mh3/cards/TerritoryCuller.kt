package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Territory Culler
 * {4}{G}
 * Creature — Eldrazi
 * 7/5
 *
 * Devoid
 * Reach
 * Landfall — Whenever a land you control enters, look at the top card of your library. If it's a
 * creature card, you may reveal it and put it into your hand. If you don't put the card into your
 * hand, you may put it into your graveyard.
 *
 * The landfall resolution is the Archghoul of Thraben peek pipeline: gather the top card (a look,
 * not a move), offer up to one *creature* card to reveal into hand, then offer whatever wasn't taken
 * — creature or not — for the optional trip to the graveyard. A card declined at both steps stays on
 * top, undisturbed.
 */
val TerritoryCuller = card("Territory Culler") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi"
    power = 7
    toughness = 5
    oracleText = "Devoid (This card has no color.)\nReach\nLandfall — Whenever a land you control " +
        "enters, look at the top card of your library. If it's a creature card, you may reveal it " +
        "and put it into your hand. If you don't put the card into your hand, you may put it into " +
        "your graveyard."

    keywords(Keyword.DEVOID, Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(1))
            val (toHandCards, notTaken) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.Creature,
                prompt = "Reveal the creature card and put it into your hand?",
                selectedLabel = "Reveal and put into your hand",
                remainderLabel = "Leave it"
            )
            toHand(toHandCards, revealed = true)
            val (toGraveyardCards, staysOnTop) = chooseUpToSplit(
                1,
                from = notTaken,
                prompt = "Put the card into your graveyard?",
                selectedLabel = "Put into your graveyard",
                remainderLabel = "Leave on top of your library"
            )
            toGraveyard(toGraveyardCards)
            toLibraryTop(staysOnTop, order = CardOrder.Preserve)
        }
        description = "Landfall — Whenever a land you control enters, look at the top card of your " +
            "library. If it's a creature card, you may reveal it and put it into your hand. If you " +
            "don't put the card into your hand, you may put it into your graveyard."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "173"
        artist = "Tiffany Turrill"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c249aa6-c65c-4572-8e2e-c3899638ecce.jpg?1783911255"

        ruling(
            "2024-06-07",
            "You don't have to reveal the card even if it's a creature card. You may leave it on top " +
                "of your library or put it into your graveyard."
        )
    }
}
