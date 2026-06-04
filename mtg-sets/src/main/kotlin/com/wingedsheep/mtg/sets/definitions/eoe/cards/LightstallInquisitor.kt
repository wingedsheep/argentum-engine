package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Lightstall Inquisitor
 * {W}
 * Creature — Angel Wizard
 * 2/1
 * Vigilance
 * When this creature enters, each opponent exiles a card from their hand and may play
 * that card for as long as it remains exiled. Each spell cast this way costs {1} more
 * to cast. Each land played this way enters tapped.
 *
 * Implementation: ForEachPlayer(EachOpponent) iterates with controllerId rebound to the
 * iterated opponent. Inside, the opponent's hand is gathered, that opponent (as
 * Chooser.Controller within the iteration) selects one card, the card moves to their
 * exile, then GrantMayPlayFromExile + GrantPlayWithCostIncrease apply the permanent
 * cast-from-exile permission and the {1} surcharge. landEntersTapped=true on the
 * permission propagates the tapped-on-entry clause to PlayLandHandler.
 */
val LightstallInquisitor = card("Lightstall Inquisitor") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel Wizard"
    power = 2
    toughness = 1
    oracleText = "Vigilance\n" +
        "When this creature enters, each opponent exiles a card from their hand and may " +
        "play that card for as long as it remains exiled. Each spell cast this way costs " +
        "{1} more to cast. Each land played this way enters tapped."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ForEachPlayerEffect(
            players = Player.EachOpponent,
            effects = listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.You),
                    storeAs = "exileCandidates"
                ),
                SelectFromCollectionEffect(
                    from = "exileCandidates",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    storeSelected = "chosenCard",
                    prompt = "Choose a card to exile"
                ),
                MoveCollectionEffect(
                    from = "chosenCard",
                    destination = CardDestination.ToZone(Zone.EXILE)
                ),
                Effects.GrantMayPlayFromExile(
                    from = "chosenCard",
                    expiry = MayPlayExpiry.Permanent,
                    landEntersTapped = true
                ),
                Effects.GrantPlayWithCostIncrease(from = "chosenCard", amount = 1)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Arif Wijaya"
        flavorText = "Born of dead stars, the Astelli may fight for light, but they know the void as their womb."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/635245e9-c27f-4a51-a6f1-bae62e696542.jpg?1752946647"
        ruling("2025-07-25", "Playing the exiled card follows all normal timing restrictions.")
    }
}
