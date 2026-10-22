package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Devious Cover-Up
 * {2}{U}{U}
 * Instant
 * Counter target spell. If that spell is countered this way, exile it instead of putting it into
 * its owner's graveyard. You may shuffle up to four target cards from your graveyard into your
 * library.
 *
 * The spell target is declared first: the counter reads the first chosen target. The graveyard
 * clause is an optional `count = 4` requirement over your own graveyard; each chosen card goes to
 * your library and then your library is shuffled once — unconditionally, since the rulings say
 * choosing zero cards still shuffles your library. The steps are sequential, so an uncounterable
 * spell still lets the shuffle happen. Devious Cover-Up is on the stack while being cast, so it can
 * never be one of its own graveyard targets.
 */
val DeviousCoverUp = card("Devious Cover-Up") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell. If that spell is countered this way, exile it instead of " +
        "putting it into its owner's graveyard. You may shuffle up to four target cards from your " +
        "graveyard into your library."

    spell {
        target(TargetFilter.SpellOnStack)
        val (g0, g1, g2, g3) = targets(
            TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD),
            count = 4,
            optional = true,
        )
        effect = Effects.CounterSpellToExile() then
            Effects.Move(g0, Zone.LIBRARY) then
            Effects.Move(g1, Zone.LIBRARY) then
            Effects.Move(g2, Zone.LIBRARY) then
            Effects.Move(g3, Zone.LIBRARY) then
            Effects.ShuffleLibrary()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "35"
        artist = "Micah Epstein"
        flavorText = "First, dispose of the evidence. Then dispose of your knowledge of its disposal."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21ac6b0a-b1a5-439d-b65e-5f04e1826c80.jpg?1783934190"
        ruling("2018-10-05", "Devious Cover-Up can't target itself at all. You can't try to counter it with itself and shuffle cards in, and you can't use its effect to shuffle itself into your library.")
        ruling("2018-10-05", "You may target zero cards in your graveyard and still shuffle your library.")
        ruling("2018-10-05", "A spell that can't be countered is a legal target for Devious Cover-Up. The spell won't be countered when Devious Cover-Up resolves, but you'll still shuffle cards into your library.")
    }
}
