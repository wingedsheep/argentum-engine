package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Amulet of Kroog
 * {2}
 * Artifact
 * {2}, {T}: Prevent the next 1 damage that would be dealt to any target this turn.
 */
val AmuletOfKroog = card("Amulet of Kroog") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{2}, {T}: Prevent the next 1 damage that would be dealt to any target this turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val t = target("any target", Targets.Any)
        effect = Effects.PreventNextDamage(1, EffectTarget.ContextTarget(0))
        description = "{2}, {T}: Prevent the next 1 damage that would be dealt to any target this turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "36"
        artist = "Margaret Organ-Kean"
        flavorText = "Among the first allies Urza gained were the people of Kroog. As a sign of friendship, Urza gave the healers of the city potent amulets; afterwards, thousands journeyed to Kroog in hope of healing, greatly adding to the city's glory."
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b094f8dd-0184-41a2-9767-e848a6e4eac1.jpg?1562932268"
    }
}
