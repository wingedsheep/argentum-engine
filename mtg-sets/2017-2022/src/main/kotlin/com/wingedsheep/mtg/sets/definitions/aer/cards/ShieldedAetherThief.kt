package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Shielded Aether Thief
 * {1}{U}
 * Creature — Vedalken Rogue
 * 0/4
 * Flash
 * Whenever this creature blocks, you get {E} (an energy counter).
 * {T}, Pay {E}{E}{E}: Draw a card.
 */
val ShieldedAetherThief = card("Shielded Aether Thief") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Vedalken Rogue"
    power = 0
    toughness = 4
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "Whenever this creature blocks, you get {E} (an energy counter).\n" +
        "{T}, Pay {E}{E}{E}: Draw a card."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.blocks()
        effect = Effects.GetEnergy(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 3))
        effect = Effects.DrawCards(1)
        description = "{T}, Pay {E}{E}{E}: Draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "44"
        artist = "Lake Hurwitz"
        flavorText = "Conflict breeds opportunity."
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e34be31b-eeb4-40e5-acf7-6cd0ba6d4bcf.jpg?1783936768"
        ruling(
            "2017-02-09",
            "If Shielded Aether Thief somehow blocks more than one creature at once, you get only one {E}."
        )
    }
}
