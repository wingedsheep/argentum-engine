package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Extinguish the Light
 * {2}{B}{B}
 * Instant
 * Destroy target creature or planeswalker. If its mana value was 3 or less, you gain 3 life.
 *
 * The mana-value check runs *before* the destroy (the Raze to the Ground / Perilous Voyage
 * idiom): a target read is live-only, so checking afterwards would read the card in the
 * graveyard rather than the permanent as it last existed on the battlefield (a Clone copying a
 * three-drop would otherwise report its own printed mana value). The life gain doesn't depend on
 * the destroy succeeding — an indestructible target with mana value 3 or less still gains you 3.
 * If the target is illegal on resolution the whole spell fizzles and no life is gained.
 */
val ExtinguishTheLight = card("Extinguish the Light") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature or planeswalker. If its mana value was 3 or less, you gain 3 life."

    spell {
        val victim = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.If(
            condition = Conditions.TargetSpellManaValueAtMost(DynamicAmounts.fixed(3), victim),
            then = Effects.Destroy(victim) then Effects.GainLife(3),
            otherwise = Effects.Destroy(victim)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Ekaterina Burmak"
        flavorText = "Jaya did not want to die, but of all the things to die for, Dominaria felt right."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01a51d08-f90e-4b72-8dce-fb2a6c72f181.jpg?1783921332"
    }
}
