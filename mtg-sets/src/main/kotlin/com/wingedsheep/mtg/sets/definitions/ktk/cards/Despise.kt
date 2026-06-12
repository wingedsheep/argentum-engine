package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.dsl.Effects

/**
 * Despise
 * {B}
 * Sorcery
 * Target opponent reveals their hand. You choose a creature or planeswalker card from it.
 * That player discards that card.
 */
val Despise = card("Despise") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a creature or planeswalker card from it. That player discards that card."

    spell {
        val t = target("target", TargetOpponent())
        effect = Effects.Pipeline {
            // 1. Reveal opponent's hand
            run(RevealHandEffect(t))
            // 2. Gather all cards from opponent's hand
            val hand = gather(
                CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                name = "hand"
            )
            // 3. Controller chooses a creature or planeswalker card
            val toDiscard = chooseExactly(
                1,
                from = hand,
                chooser = Chooser.Controller,
                filter = GameObjectFilter.Creature or GameObjectFilter.Planeswalker,
                prompt = "Choose a creature or planeswalker card to discard",
                alwaysPrompt = true,
                showAllCards = true,
                name = "toDiscard"
            )
            // 4. Move chosen card to opponent's graveyard
            move(
                toDiscard,
                CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                moveType = MoveType.Discard
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "Todd Lockwood"
        flavorText = "\"You have returned from fire, traitor. This time I will see you leave as ashes.\"\n—Zurgo, to Sarkhan Vol"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64216ce3-945e-42d8-9127-cf11c687da67.jpg?1564775472"
    }
}
