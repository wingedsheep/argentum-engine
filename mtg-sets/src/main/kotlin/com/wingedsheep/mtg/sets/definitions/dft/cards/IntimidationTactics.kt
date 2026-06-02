package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Intimidation Tactics
 * {B}
 * Sorcery
 * Target opponent reveals their hand. You choose an artifact or creature card from it. Exile that card.
 * Cycling {3}
 */
val IntimidationTactics = card("Intimidation Tactics") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose an artifact or creature card from it. Exile that card.\n" +
        "Cycling {3} ({3}, Discard this card: Draw a card.)"

    spell {
        val t = target("target", TargetOpponent())
        effect = Effects.Composite(
            listOf(
                RevealHandEffect(t),
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                    storeAs = "hand"
                ),
                SelectFromCollectionEffect(
                    from = "hand",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Controller,
                    filter = GameObjectFilter.Artifact or GameObjectFilter.Creature,
                    storeSelected = "toExile",
                    prompt = "Choose an artifact or creature card to exile"
                ),
                MoveCollectionEffect(
                    from = "toExile",
                    destination = CardDestination.ToZone(Zone.EXILE, Player.ContextPlayer(0))
                )
            )
        )
    }

    keywordAbility(KeywordAbility.cycling("{3}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Cristi Balanescu"
        flavorText = "\"This is still a negotiation. We haven't broken anything... yet.\"\n—Far Fortune"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b4e6022-44d2-4dfe-8f7a-51581e298f23.jpg?1738356324"
    }
}
