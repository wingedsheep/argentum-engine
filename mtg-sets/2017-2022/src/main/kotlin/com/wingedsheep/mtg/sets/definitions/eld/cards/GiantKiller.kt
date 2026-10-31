package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Giant Killer // Chop Down
 * {W}
 * Creature — Human Peasant
 * 1/2
 * {1}{W}, {T}: Tap target creature.
 *
 * Adventure: Chop Down — {2}{W}, Instant — Adventure
 * Destroy target creature with power 4 or greater.
 */
val GiantKiller = card("Giant Killer") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Peasant"
    power = 1
    toughness = 2
    oracleText = "{1}{W}, {T}: Tap target creature."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.Tap)
        val t = target(TargetFilter.Creature)
        effect = Effects.Tap(t)
    }

    adventure("Chop Down") {
        manaCost = "{2}{W}"
        typeLine = "Instant — Adventure"
        oracleText = "Destroy target creature with power 4 or greater. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val t = target(TargetFilter.Creature.powerAtLeast(4))
            effect = Effects.Destroy(t)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Jesper Ejsing"
        flavorText = "\"What size advantage? They never even see me coming.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75754468-2850-42e6-ab22-61ff7b9d1214.jpg?1783932673"
    }
}
