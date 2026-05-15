package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wildsear, Scouring Maw {3}{R}{G}
 * Legendary Creature — Elemental Wolf
 * 6/6
 *
 * Trample
 * Enchantment spells you cast from your hand have cascade.
 *
 * Modeled as a triggered ability on Wildsear: whenever the controller casts an
 * enchantment spell from their hand, Wildsear's cascade trigger fires and the
 * [com.wingedsheep.sdk.scripting.effects.CascadeEffect] reads the triggering
 * spell's mana value to walk the library. This avoids needing to literally grant
 * the CASCADE keyword to spells on the stack — the resulting game behavior is
 * the same since cascade is itself a triggered ability (CR 702.85a).
 */
val WildsearScouringMaw = card("Wildsear, Scouring Maw") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Elemental Wolf"
    power = 6
    toughness = 6
    oracleText = "Trample\n" +
        "Enchantment spells you cast from your hand have cascade. " +
        "(Whenever you cast an enchantment spell from your hand, exile cards from " +
        "the top of your library until you exile a nonland card that costs less. " +
        "You may cast it without paying its mana cost. Put the exiled cards on the " +
        "bottom in a random order.)"

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.YouCastEnchantmentFromHand
        effect = Effects.Cascade
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "8"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48494ef9-5eb3-4b55-ad1d-b32bbed89ca5.jpg?1721428165"
    }
}
