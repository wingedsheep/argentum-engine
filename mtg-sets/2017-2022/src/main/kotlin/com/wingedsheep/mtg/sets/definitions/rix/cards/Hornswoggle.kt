package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hornswoggle
 * {2}{U}
 * Instant
 *
 * Counter target creature spell. You create a Treasure token.
 *
 * The Treasure isn't conditional on the counter succeeding: a creature spell that can't be countered
 * stays on the stack and you still get the Treasure (ruling 2018-01-19), the same shape as Spell Swindle.
 */
val Hornswoggle = card("Hornswoggle") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target creature spell. You create a Treasure token. " +
        "(It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    spell {
        target(TargetFilter.CreatureSpellOnStack)
        effect = Effects.CounterSpell() then Effects.CreateTreasure()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "39"
        artist = "Deruchenko Alexander"
        flavorText = "Being a merciful pirate, she saved the vampire's gold from falling overboard."
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b10b8f15-b323-44d8-85a7-ed662a40889d.jpg?1783935324"
        ruling("2018-01-19", "Hornswoggle can target a spell that can't be countered, such as Nezahal, Primal Tide. When Hornswoggle resolves, that spell won't be countered, but you'll still get a Treasure.")
    }
}
