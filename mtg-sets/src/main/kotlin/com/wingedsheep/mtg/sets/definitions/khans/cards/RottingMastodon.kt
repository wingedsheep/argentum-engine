package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val RottingMastodon = card("Rotting Mastodon") {
    manaCost = "{4}{B}"
    typeLine = "Creature — Zombie Elephant"
    power = 2
    toughness = 8

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "87"
        artist = "Nils Hamm"
        flavorText = "Mastodons became extinct long ago, but foul forces of the Gurmag Swamp sometimes animate their decaying remains. The Sultai happily exploit such creatures but consider them inferior to their own necromantic creations."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/1564a20a-0e57-4ced-9eda-7acff74274e7.jpg?1562782961"
    }
}
