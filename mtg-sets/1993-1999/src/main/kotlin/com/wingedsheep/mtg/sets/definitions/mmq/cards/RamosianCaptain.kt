package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Ramosian Captain
 * {1}{W}{W}
 * Creature — Human Rebel
 * 2 / 2
 */
val RamosianCaptain = card("Ramosian Captain") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Rebel"
    oracleText = "First strike\n" +
        "{5}, {T}: Search your library for a Rebel permanent card with mana value 4 or less, put it onto the battlefield, then shuffle."
    power = 2
    toughness = 2
    keywords(Keyword.FIRST_STRIKE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Permanent.withSubtype("Rebel").manaValueAtMost(4),
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Matthew D. Wilson"
        flavorText = "The Cho-Arrim believe in leading by example."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e9d2e2a-c608-4787-bbd9-e1871f681b58.jpg"
    }
}
