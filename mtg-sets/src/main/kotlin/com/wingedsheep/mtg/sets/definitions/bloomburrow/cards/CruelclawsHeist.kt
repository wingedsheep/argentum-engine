package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Cruelclaw's Heist
 * {B}{B}
 * Sorcery
 *
 * Gift a card (You may promise an opponent a gift as you cast this spell.
 * If you do, they draw a card before its other effects.)
 *
 * Target opponent reveals their hand. You choose a nonland card from it.
 * Exile that card. If the gift was promised, you may cast that card for as
 * long as it remains exiled, and mana of any type can be spent to cast it.
 *
 * Gift is modeled as a modal choice. Mode 1 = no gift (exile to exile zone),
 * Mode 2 = gift (opponent draws, exile with permanent cast-from-exile permission).
 */
val CruelclawsHeist = card("Cruelclaw's Heist") {
    manaCost = "{B}{B}"
    typeLine = "Sorcery"
    oracleText = "Gift a card (You may promise an opponent a gift as you cast this spell. If you do, they draw a card before its other effects.)\n" +
        "Target opponent reveals their hand. You choose a nonland card from it. Exile that card. If the gift was promised, you may cast that card for as long as it remains exiled, and mana of any type can be spent to cast it."

    // Common pipeline for both modes: reveal hand, choose nonland, exile
    val revealChooseExile = listOf(
        RevealHandEffect(EffectTarget.ContextTarget(0)),
        GatherCardsEffect(
            source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0), GameObjectFilter.Nonland),
            storeAs = "nonlandCards"
        ),
        SelectFromCollectionEffect(
            from = "nonlandCards",
            selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
            chooser = Chooser.Controller,
            storeSelected = "chosenCard",
            prompt = "Choose a nonland card to exile"
        ),
        MoveCollectionEffect(
            from = "chosenCard",
            destination = CardDestination.ToZone(Zone.EXILE, Player.ContextPlayer(0))
        )
    )

    spell {
        effect = ModalEffect.chooseOne(
            // Mode 1: No gift — reveal, choose nonland, exile (can't cast it)
            Mode.withTarget(
                CompositeEffect(revealChooseExile),
                Targets.Opponent,
                "Don't promise a gift — exile a nonland card from target opponent's hand"
            ),
            // Mode 2: Gift a card — opponent draws, then reveal, choose nonland, exile
            //         with permanent cast-from-exile permission
            Mode.withTarget(
                CompositeEffect(
                    listOf(DrawCardsEffect(1, EffectTarget.ContextTarget(0))) +
                    revealChooseExile +
                    listOf(GrantMayPlayFromExileEffect(from = "chosenCard", expiry = MayPlayExpiry.Permanent)) +
                    listOf(Effects.GiftGiven())
                ),
                Targets.Opponent,
                "Promise a gift — an opponent draws a card, then exile a nonland card from target opponent's hand (you may cast it from exile)"
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/cab4539a-0157-4cbe-b50f-6e2575df74e9.jpg?1721426377"
        ruling("2024-07-26", "You pay all costs and follow all timing rules for a spell cast this way. For example, if the exiled card is a sorcery, you may cast it only during your main phase while the stack is empty.")
        ruling("2024-07-26", "For instants and sorceries with gift, the gift is given to the appropriate opponent as part of the resolution of the spell. This happens before any of the spell's other effects would take place.")
    }
}
