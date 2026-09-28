package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Piracy
 * {U}{U}
 * Sorcery
 *
 * Until end of turn, you may tap lands you don't control for mana. Spend this mana only to cast
 * spells.
 *
 * A turn-scoped grant on the caster: the {T} mana abilities of lands they don't control become
 * theirs to activate, and the mana those lands make carries the spells-only restriction.
 */
val Piracy = card("Piracy") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Until end of turn, you may tap lands you don't control for mana. " +
        "Spend this mana only to cast spells."

    spell {
        effect = Effects.TapForManaPermanentsYouDontControl(
            permanentFilter = GameObjectFilter.Land,
            restriction = ManaRestriction.SpellsOnly,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Bradley Williams"
        flavorText = "The sea gives, and the sea takes. Some just help with the taking."
        imageUri = "https://cards.scryfall.io/normal/front/d/a/daeab88f-2bfc-4f40-8eed-ee44a9e90bd7.jpg"
    }
}
