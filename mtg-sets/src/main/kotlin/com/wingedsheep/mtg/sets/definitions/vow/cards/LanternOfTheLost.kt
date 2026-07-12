package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Lantern of the Lost
 * {1}
 * Artifact
 *
 * When this artifact enters, exile target card from a graveyard.
 * {1}, {T}, Exile this artifact: Exile all cards from all graveyards, then draw a card.
 */
val LanternOfTheLost = card("Lantern of the Lost") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "When this artifact enters, exile target card from a graveyard.\n" +
        "{1}, {T}, Exile this artifact: Exile all cards from all graveyards, then draw a card."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val exiled = target("target card in a graveyard", Targets.CardInGraveyard)
        effect = Effects.Move(exiled, Zone.EXILE)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.ExileSelf)
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.Each,
                    filter = GameObjectFilter.Any,
                ),
                storeAs = "allGraveyards",
            ),
            MoveCollectionEffect(
                from = "allGraveyards",
                destination = CardDestination.ToZone(Zone.EXILE),
            ),
            Effects.DrawCards(1),
        )
        description = "{1}, {T}, Exile this artifact: Exile all cards from all graveyards, then draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "259"
        artist = "Chris Cold"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2303f11-2c82-44d5-893a-8e71dece7746.jpg?1782703014"
    }
}
