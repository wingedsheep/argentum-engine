package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
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
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Despise
 * {B}
 * Sorcery
 * Target opponent reveals their hand. You choose a creature or planeswalker card from it.
 * That player discards that card.
 */
val Despise = card("Despise") {
    manaCost = "{B}"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a creature or planeswalker card from it. That player discards that card."

    spell {
        val t = target("target", TargetOpponent())
        effect = CompositeEffect(
            listOf(
                // 1. Reveal opponent's hand
                RevealHandEffect(t),
                // 2. Gather all cards from opponent's hand
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                    storeAs = "hand"
                ),
                // 3. Controller chooses a creature or planeswalker card
                SelectFromCollectionEffect(
                    from = "hand",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Controller,
                    filter = GameObjectFilter.Creature or GameObjectFilter.Planeswalker,
                    storeSelected = "toDiscard",
                    prompt = "Choose a creature or planeswalker card to discard"
                ),
                // 4. Move chosen card to opponent's graveyard
                MoveCollectionEffect(
                    from = "toDiscard",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                    moveType = MoveType.Discard
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "Todd Lockwood"
        flavorText = "\"You have returned from fire, traitor. This time I will see you leave as ashes.\"\n—Zurgo, to Sarkhan Vol"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64216ce3-945e-42d8-9127-cf11c687da67.jpg?1564775472"
    }
}
