package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
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
 * The Torment of Gollum
 * {3}{B}
 * Sorcery
 *
 * Target opponent reveals their hand. You choose a nonland card from it. That player discards that card.
 * Amass Orcs 2.
 */
val TheTormentOfGollum = card("The Torment of Gollum") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You choose a nonland card from it. That player " +
        "discards that card.\n" +
        "Amass Orcs 2. (Put two +1/+1 counters on an Army you control. It's also an Orc. If you don't " +
        "control an Army, create a 0/0 black Orc Army creature token first.)"

    spell {
        val opponent = target("target opponent", TargetOpponent())
        effect = Effects.Composite(
            listOf(
                RevealHandEffect(opponent),
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                    storeAs = "hand"
                ),
                SelectFromCollectionEffect(
                    from = "hand",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Controller,
                    filter = GameObjectFilter.Nonland,
                    storeSelected = "toDiscard",
                    prompt = "Choose a nonland card to discard"
                ),
                MoveCollectionEffect(
                    from = "toDiscard",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                    moveType = MoveType.Discard
                ),
                Effects.Amass(2, "Orc")
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Nino Is"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba2c8c25-1fa9-4cc4-a378-5eccc25bacf0.jpg?1686968743"
    }
}
