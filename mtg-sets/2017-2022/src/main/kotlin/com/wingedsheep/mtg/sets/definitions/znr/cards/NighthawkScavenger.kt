package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Nighthawk Scavenger
 * {1}{B}{B}
 * Creature — Vampire Rogue
 * 1+✶/3
 *
 * Flying, deathtouch, lifelink
 * Nighthawk Scavenger's power is equal to 1 plus the number of card types among cards in your
 * opponents' graveyards.
 *
 * The Tarmogoyf characteristic-defining shape restricted to [Player.EachOpponent]: the aggregate
 * flattens every opponent's graveyard into one list before taking distinct types, so a creature in
 * each of two opponents' graveyards counts once. Only power is characteristic-defining; the "1 plus"
 * is the power offset and toughness is the printed 3.
 */
val NighthawkScavenger = card("Nighthawk Scavenger") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Rogue"
    oracleText = "Flying, deathtouch, lifelink\n" +
        "Nighthawk Scavenger's power is equal to 1 plus the number of card types among cards in your opponents' graveyards."
    toughness = 3
    dynamicPower(
        DynamicAmounts.zone(Player.EachOpponent, Zone.GRAVEYARD).distinctTypes(),
        offset = 1,
    )

    keywords(Keyword.FLYING, Keyword.DEATHTOUCH, Keyword.LIFELINK)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "115"
        artist = "Heonhwa"
        flavorText = "Under Drana's leadership, vampires are finding new strength in unity."
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb348f07-1d48-4877-8a56-60655d64a3f7.jpg?1783929373"

        ruling("2020-09-25", "The ability that defines Nighthawk Scavenger's power applies in all zones, not just the battlefield.")
        ruling("2020-09-25", "Nighthawk Scavenger counts card types, not cards. If the only card in your opponents' graveyards is a single artifact creature card, Nighthawk Scavenger will be 3/3. If the only cards in those graveyards are ten artifact cards and ten creature cards, Nighthawk Scavenger will still be 3/3.")
    }
}
