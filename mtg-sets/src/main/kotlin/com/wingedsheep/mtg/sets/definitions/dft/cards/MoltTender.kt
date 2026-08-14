package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Molt Tender
 * {G}
 * Creature — Insect Druid — Uncommon (DFT #171)
 * 1/1
 *
 * {T}: Mill a card.
 * {T}, Exile a card from your graveyard: Add one mana of any color.
 *
 * Two separate tap abilities that compete for the same untap — the first fuels the graveyard, the
 * second spends it. Only the second is a mana ability (`manaAbility = true`): it has no target and
 * could add mana, so the exile-from-graveyard additional cost doesn't disqualify it (CR 605.1a) and
 * it resolves without using the stack. The first ability adds no mana, so it is an ordinary
 * activated ability that uses the stack.
 */
val MoltTender = card("Molt Tender") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect Druid"
    power = 1
    toughness = 1
    oracleText = "{T}: Mill a card. (Put the top card of your library into your graveyard.)\n" +
        "{T}, Exile a card from your graveyard: Add one mana of any color."

    activatedAbility {
        cost = Costs.Tap
        effect = Patterns.Library.mill(1)
        description = "{T}: Mill a card."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.ExileFromGraveyard(1))
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        description = "{T}, Exile a card from your graveyard: Add one mana of any color."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "171"
        artist = "Filip Burburan"
        flavorText = "The Speedbrood regards molting as a selfless act worthy of the highest of " +
            "honors. It allows one to be born anew for the good of the brood."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f800bf4e-4bfb-45b6-950b-c76952f52bb1.jpg?1783907868"
    }
}
