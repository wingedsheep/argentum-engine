package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Volcanic Spite
 * {1}{R}
 * Instant
 * Volcanic Spite deals 3 damage to target creature, planeswalker, or battle. You may put a card
 * from your hand on the bottom of your library. If you do, draw a card.
 *
 * The optional tuck is `chooseUpTo(1)` from hand; [Effects.IfYouDo] draws only when a card actually
 * moved, so declining (or an empty hand) draws nothing.
 */
val VolcanicSpite = card("Volcanic Spite") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Volcanic Spite deals 3 damage to target creature, planeswalker, or battle. " +
        "You may put a card from your hand on the bottom of your library. If you do, draw a card."

    spell {
        val t = target(TargetFilter.CreaturePlaneswalkerOrBattle)
        effect = Effects.DealDamage(3, t) then Effects.IfYouDo(
            action = Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                val chosen = chooseUpTo(1, from = hand, prompt = "You may put a card from your hand on the bottom of your library")
                toLibraryBottom(chosen)
            },
            then = Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "170"
        artist = "Kevin Sidharta"
        flavorText = "None may approach the Ashen Idol without an offering—an edict fortuitously absent from Phyrexian intelligence concerning the world of Azgol."
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69490262-afc2-4692-8fac-b771a972c8f8.jpg?1783916979"
    }
}
