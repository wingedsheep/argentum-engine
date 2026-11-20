package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Servant of Tymaret
 * {2}{B}
 * Creature — Zombie
 * 1/3
 *
 * Inspired — Whenever this creature becomes untapped, each opponent loses 1 life. You gain life
 * equal to the life lost this way.
 * {2}{B}: Regenerate this creature.
 *
 * [Effects.DrainLife] is the "each opponent loses X, you gain the life lost this way" shape.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val ServantOfTymaret = card("Servant of Tymaret") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 1
    toughness = 3
    oracleText = "Inspired — Whenever this creature becomes untapped, each opponent loses 1 life. " +
        "You gain life equal to the life lost this way.\n" +
        "{2}{B}: Regenerate this creature."

    triggeredAbility {
        trigger = Triggers.self.becomesUntapped()
        effect = Effects.DrainLife(1)
    }

    activatedAbility {
        cost = Costs.Mana("{2}{B}")
        effect = Effects.Regenerate(EffectTarget.Self)
        description = "{2}{B}: Regenerate this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Karl Kopinski"
        flavorText = "Life is most precious to those who have already lost it."
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfb16d4f-ed42-4b19-9e45-330db88eea9b.jpg?1783939552"
    }
}
