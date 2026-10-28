package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Concerted Defense — Zendikar Rising #52
 * {U} · Instant
 *
 * Counter target noncreature spell unless its controller pays {1} plus an additional {1} for each
 * creature in your party.
 *
 * The tax is `1 + partySize()`, read when the spell resolves (the party is the caster's).
 */
val ConcertedDefense = card("Concerted Defense") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target noncreature spell unless its controller pays {1} plus an additional {1} " +
        "for each creature in your party. (Your party consists of up to one each of Cleric, Rogue, " +
        "Warrior, and Wizard.)"

    spell {
        target(TargetFilter.NoncreatureSpellOnStack)
        effect = Effects.CounterUnlessDynamicPays(1 + DynamicAmounts.partySize())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "52"
        artist = "Chris Rallis"
        flavorText = "\"You mess with one of us, you mess with all of us!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/3/235c108d-3902-4c2e-919c-a5449cd2dc3c.jpg"
    }
}
