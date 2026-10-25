package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mimic
 * {2}
 * Artifact — Treasure
 * {T}, Sacrifice this artifact: Add one mana of any color.
 * {2}: This artifact becomes a Shapeshifter artifact creature with base power and toughness 3/3
 * until end of turn.
 *
 * The animation adds the creature type and the Shapeshifter creature subtype (Layer 4) and sets
 * base P/T 3/3 (Layer 7b). Setting creature subtypes only replaces creature types, so Mimic keeps
 * its Treasure artifact subtype and its mana ability while animated; it stays colorless.
 */
val Mimic = card("Mimic") {
    manaCost = "{2}"
    typeLine = "Artifact — Treasure"
    oracleText = "{T}, Sacrifice this artifact: Add one mana of any color.\n" +
        "{2}: This artifact becomes a Shapeshifter artifact creature with base power and toughness 3/3 until end of turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
    }

    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 3,
            toughness = 3,
            creatureTypes = setOf("Shapeshifter"),
            duration = Duration.EndOfTurn,
        )
        description = "{2}: This artifact becomes a Shapeshifter artifact creature with base power and toughness 3/3 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "249"
        artist = "Scott Murphy"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9da96aab-cf84-4150-b9d0-a4b84fcf585f.jpg?1783926437"
        ruling("2021-07-23", "If an effect refers to a Treasure, it means any Treasure artifact, not just a Treasure artifact token.")
        ruling("2021-07-23", "Mimic is still a Treasure after its last ability resolves, but Treasure is an artifact type, never a creature type.")
    }
}
