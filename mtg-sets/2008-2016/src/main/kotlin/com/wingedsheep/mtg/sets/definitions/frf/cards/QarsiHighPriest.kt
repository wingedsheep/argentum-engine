package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Qarsi High Priest
 * {B}
 * Creature — Human Cleric
 * 0/2
 * {1}{B}, {T}, Sacrifice another creature: Manifest the top card of your library.
 *
 * The sacrifice is "another creature" ([Costs.SacrificeAnother]), so the Priest can't pay with itself;
 * the manifest is Mastery of the Unseen's activated effect behind a sacrifice cost.
 */
val QarsiHighPriest = card("Qarsi High Priest") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Cleric"
    power = 0
    toughness = 2
    oracleText = "{1}{B}, {T}, Sacrifice another creature: Manifest the top card of your library. (Put that " +
        "card onto the battlefield face down as a 2/2 creature. Turn it face up any time for its mana cost " +
        "if it's a creature card.)"

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{B}"),
            Costs.Tap,
            Costs.SacrificeAnother(GameObjectFilter.Creature),
        )
        effect = Patterns.Library.manifest()
        description = "{1}{B}, {T}, Sacrifice another creature: Manifest the top card of your library."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Viktor Titov"
        flavorText = "\"Hear me, nameless, and obey.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58c1bdbc-2bf4-4036-bf83-669c27458c6e.jpg?1783938695"
    }
}
