package com.wingedsheep.mtg.sets.definitions.fdn.cards

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
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Duress
 * {B}
 * Sorcery
 *
 * Target opponent reveals their hand. You choose a noncreature, nonland card from it.
 * That player discards that card.
 */
val Duress = card("Duress") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a noncreature, nonland card from it. That player discards that card."

    spell {
        val opponent = target("target opponent", TargetOpponent())
        effect = CompositeEffect(
            listOf(
                RevealHandEffect(opponent),
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                    storeAs = "opponentHand"
                ),
                SelectFromCollectionEffect(
                    from = "opponentHand",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Controller,
                    filter = GameObjectFilter.Noncreature and GameObjectFilter.Nonland,
                    storeSelected = "toDiscard",
                    prompt = "Choose a noncreature, nonland card to discard",
                    alwaysPrompt = true,
                    showAllCards = true
                ),
                MoveCollectionEffect(
                    from = "toDiscard",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                    moveType = MoveType.Discard
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "606"
        artist = "PINDURSKI"
        flavorText = "\"Give of yourself, for *compleation* awaits the cleansed.\"\n—Nokar, priest of Sheoldred"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34c3a894-ee75-4db9-a69f-711bb3cc150a.jpg?1730490899"
    }
}
