package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Port of Karfell
 * Land
 * This land enters tapped.
 * {T}: Add {U}.
 * {3}{U}{B}{B}, {T}, Sacrifice this land: Mill four cards, then return a creature card from your
 * graveyard to the battlefield tapped.
 *
 * One of Kaldheim's five "realm" lands. The returned creature isn't targeted: it is chosen on
 * resolution, after the mill, from every creature card in your graveyard — one just milled or one
 * that was already there. With no creature card in the graveyard, the mill still happens.
 */
val PortOfKarfell = card("Port of Karfell") {
    manaCost = ""
    colorIdentity = "UB"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {U}.\n" +
        "{3}{U}{B}{B}, {T}, Sacrifice this land: Mill four cards, then return a creature card from " +
        "your graveyard to the battlefield tapped. (To mill a card, put the top card of your library " +
        "into your graveyard.)"

    replacementEffect(EntersTapped())

    // {T}: Add {U}.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE, 1)
        manaAbility = true
    }

    // {3}{U}{B}{B}, {T}, Sacrifice this land: Mill four cards, then return a creature card from
    // your graveyard to the battlefield tapped.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}{U}{B}{B}"),
            Costs.Tap,
            Costs.SacrificeSelf
        )
        effect = Effects.Pipeline {
            mill(4)
            val creatures = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature))
            val chosen = chooseExactly(
                1,
                from = creatures,
                prompt = "Choose a creature card to return to the battlefield tapped"
            )
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
        }
        description = "Mill four cards, then return a creature card from your graveyard to the battlefield tapped."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "265"
        artist = "Mauricio Calle"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb9465a6-d009-4ecd-9fd1-d046547de902.jpg?1783928172"
        ruling(
            "2021-02-05",
            "The last activated ability doesn't target any creature cards in graveyards. You may return a " +
                "creature card that was just milled or one that was already there."
        )
    }
}
