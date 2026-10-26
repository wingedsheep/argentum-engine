package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary

/**
 * Korlessa, Scale Singer
 * {G}{U}
 * Legendary Creature — Dragon Bard
 * 1/4
 *
 * You may look at the top card of your library any time.
 * You may cast Dragon spells from the top of your library.
 *
 * Vizier of the Menagerie's two library statics, the cast permission narrowed to Dragon spells
 * (Hakoda's `Any.withSubtype(…)` shape, so a kindred Dragon spell qualifies too). It is a *cast*
 * permission only: normal timing applies, all costs are paid, and the top card isn't in your
 * hand (rulings).
 */
val KorlessaScaleSinger = card("Korlessa, Scale Singer") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Dragon Bard"
    power = 1
    toughness = 4
    oracleText = "You may look at the top card of your library any time.\n" +
        "You may cast Dragon spells from the top of your library."

    staticAbility {
        ability = LookAtTopOfLibrary()
    }

    staticAbility {
        ability = CastSpellTypesFromTopOfLibrary(GameObjectFilter.Any.withSubtype(Subtype.DRAGON))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "280"
        artist = "Jesper Ejsing"
        flavorText = "\"Would you like a song of wing-wind and fire, or one of gentle heartbeats atop a golden hoard?\""
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe2d08ac-6c24-4158-87a1-c59d1cd447a8.jpg?1783922690"
        ruling("2022-06-10", "You can look at the top card of your library whenever you want (with one restriction; see below), even if you don't have priority. This action doesn't use the stack. Knowing what that card is becomes part of the information you have access to, just like you can look at the cards in your hand.")
        ruling("2022-06-10", "If the top card of your library changes while you're casting a spell, playing a land, or activating an ability, you can't look at the new top card until you finish doing so. This means that if you cast a spell from the top of your library, you can't look at the next one until you're done paying for that spell.")
        ruling("2022-06-10", "Korlessa doesn't change when you can cast Dragon spells. Normally, this means during your main phase when the stack is empty, although flash may change this.")
        ruling("2022-06-10", "You'll still pay all costs for the spell, including additional costs. You may also pay alternative costs if any are available.")
        ruling("2022-06-10", "The top card of your library isn't in your hand, so you can't take other actions that would normally be allowed from your hand, such as discarding it due to an effect or activating a cycling ability.")
    }
}
