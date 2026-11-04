package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Admiral's Order
 * {1}{U}{U}
 * Instant
 *
 * Raid — If you attacked this turn, you may pay {U} rather than pay this spell's mana cost.
 * Counter target spell.
 *
 * Raid is a conditional alternative cost: [SelfAlternativeCost] gated on
 * [Conditions.YouAttackedThisTurn], checked when the spell is cast.
 */
val AdmiralsOrder = card("Admiral's Order") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Raid — If you attacked this turn, you may pay {U} rather than pay this spell's mana cost.\n" +
        "Counter target spell."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{U}"),
        condition = Conditions.YouAttackedThisTurn,
    )

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Slawomir Maniak"
        flavorText = "\"Damn the cannons, mates. Charge!\""
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80dc0310-afd9-49b4-b58f-a0e91120c38c.jpg?1783935329"
    }
}
