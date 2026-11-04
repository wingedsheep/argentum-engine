package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardsInZonesCantBeTargeted
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Silent Gravestone — Rivals of Ixalan #182
 * {1} · Artifact
 *
 * Cards in graveyards can't be the targets of spells or abilities.
 * {4}, {T}: Exile this artifact and all cards from all graveyards. Draw a card.
 *
 * Implementation:
 *  - The graveyard lock is [CardsInZonesCantBeTargeted] (default: every card in every graveyard),
 *    as on Dennick, Pious Apprentice and Ground Seal.
 *  - Exiling the Gravestone is part of the effect, not the cost (it stays on the battlefield until
 *    the ability resolves). The self-exile reads `fromZone = BATTLEFIELD`; if the Gravestone has
 *    already died in response it is a card in a graveyard and the sweep exiles it anyway.
 *    Then the Lantern of the Lost sweep exiles every graveyard card, then a card is drawn.
 */
val SilentGravestone = card("Silent Gravestone") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Cards in graveyards can't be the targets of spells or abilities.\n" +
        "{4}, {T}: Exile this artifact and all cards from all graveyards. Draw a card."

    staticAbility {
        ability = CardsInZonesCantBeTargeted()
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.Pipeline {
            run(Effects.Move(EffectTarget.Self, Zone.EXILE, fromZone = Zone.BATTLEFIELD))
            val allGraveyards = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.Each,
                    filter = GameObjectFilter.Any,
                )
            )
            exile(allGraveyards)
            run(Effects.DrawCards(1))
        }
        description = "{4}, {T}: Exile this artifact and all cards from all graveyards. Draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "182"
        artist = "Suzanne Helmigh"
        flavorText = "Made to ward off the Grim Captain and silence the lure of undeath."
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff60f685-a17e-4be6-bbb6-d14a053533ef.jpg?1783935265"
        ruling(
            "2018-01-19",
            "Only spells and abilities that target cards in graveyards will be affected. Spells and abilities " +
                "that affect cards in graveyards without targeting them (such as Extract from Darkness) can " +
                "still affect those cards."
        )
        ruling("2018-01-19", "Silent Gravestone isn't exiled until its activated ability resolves.")
    }
}
