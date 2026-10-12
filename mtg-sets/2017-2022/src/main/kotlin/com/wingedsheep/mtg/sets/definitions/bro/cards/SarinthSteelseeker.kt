package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Sarinth Steelseeker
 * {1}{G}
 * Creature — Human Artificer Scout
 * 1/2
 *
 * Whenever an artifact you control enters, look at the top card of your library. If it's a land
 * card, you may reveal it and put it into your hand. If you don't put the card into your hand, you
 * may put it into your graveyard.
 *
 * The Archghoul of Thraben / Territory Culler peek pipeline: gather the top card (a look, not a
 * move), offer up to one *land* card to reveal into hand, then offer whatever wasn't taken — land or
 * not — for the optional trip to the graveyard. A card declined at both steps stays on top.
 */
val SarinthSteelseeker = card("Sarinth Steelseeker") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Artificer Scout"
    power = 1
    toughness = 2
    oracleText = "Whenever an artifact you control enters, look at the top card of your library. " +
        "If it's a land card, you may reveal it and put it into your hand. If you don't put the " +
        "card into your hand, you may put it into your graveyard."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Artifact.youControl()).enters()
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(1))
            val (toHandCards, notTaken) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.Land,
                prompt = "Reveal the land card and put it into your hand?",
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
        description = "Whenever an artifact you control enters, look at the top card of your " +
            "library. If it's a land card, you may reveal it and put it into your hand. If you " +
            "don't put the card into your hand, you may put it into your graveyard."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "189"
        artist = "Olivier Bernard"
        flavorText = "\"This metal junk is ugly, but it's got its uses.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d96761b2-eb9c-4963-a496-ba53a01f4b17.jpg?1783920040"

        ruling(
            "2022-10-14",
            "You don't have to reveal the card if it's a land card. You may choose not to reveal it " +
                "and instead put it into your graveyard."
        )
    }
}
