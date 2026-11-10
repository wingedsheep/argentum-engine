package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Pick the Brain — Shadows over Innistrad #129
 * {2}{B} · Sorcery
 *
 * Target opponent reveals their hand. You choose a nonland card from it and exile that card.
 * Delirium — If there are four or more card types among cards in your graveyard, search that
 * player's graveyard, hand, and library for any number of cards with the same name as the exiled
 * card, exile those cards, then that player shuffles.
 *
 * Lobotomy's capture-a-name shape, with the follow-up search gated on delirium and Lost Legacy's
 * "any number" pick over the matches. Delirium is checked after the first exile, while the spell is
 * still on the stack, so it never counts its own sorcery type. With no nonland card to choose, the
 * captured name is empty: the delirium search finds nothing, but that player still shuffles.
 */
val PickTheBrain = card("Pick the Brain") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonland card from it and exile that card.\n" +
        "Delirium — If there are four or more card types among cards in your graveyard, search that player's " +
        "graveyard, hand, and library for any number of cards with the same name as the exiled card, exile " +
        "those cards, then that player shuffles."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val chosen = chooseExactly(
                1, from = hand,
                filter = GameObjectFilter.Nonland,
                prompt = "Choose a nonland card to exile",
                selectedLabel = "Exile",
                alwaysPrompt = true,
                showAllCards = true
            )
            val exiledName = storeCardName(chosen)
            exile(chosen, owner = opponent.asPlayer)
            ifThen(Conditions.Delirium()) {
                val matches = gather(
                    CardSource.FromMultipleZones(
                        zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                        player = opponent.asPlayer,
                        filter = GameObjectFilter.Any.namedFromVariable(exiledName)
                    ),
                    search = true
                )
                val toExile = chooseAnyNumber(
                    from = matches,
                    prompt = "Choose any number of cards with the same name to exile",
                    selectedLabel = "Exile"
                )
                exile(toExile, owner = opponent.asPlayer)
                run(Effects.ShuffleLibrary(target = opponent))
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "129"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53503e59-6b6c-4de1-8528-458ab94f4e9a.jpg?1783937767"
        ruling(
            "2016-04-08",
            "If there is no nonland card to choose, and you have four or more card types among cards in your " +
                "graveyard, you'll search that player's library even though you won't be able to find any cards. " +
                "That library will be shuffled."
        )
    }
}
