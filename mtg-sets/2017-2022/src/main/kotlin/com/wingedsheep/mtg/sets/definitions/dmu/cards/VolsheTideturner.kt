package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Volshe Tideturner
 * {1}{U}
 * Creature — Merfolk Wizard
 * 1/3
 * {T}: Add {U}. Spend this mana only to cast an instant or sorcery spell or a kicked spell.
 *
 * The "or" is `AnyOf(InstantOrSorceryOnly, KickedSpellsOnly)`; `KickedSpellsOnly` reads the kicker
 * slot declared on the spell actually being paid for, so an unkicked creature spell can't use it.
 */
val VolsheTideturner = card("Volshe Tideturner") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 1
    toughness = 3
    oracleText = "{T}: Add {U}. Spend this mana only to cast an instant or sorcery spell or a kicked spell."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(
            Color.BLUE,
            1,
            ManaRestriction.AnyOf(
                listOf(ManaRestriction.InstantOrSorceryOnly, ManaRestriction.KickedSpellsOnly)
            )
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Tia Masic"
        flavorText = "Ships suspected of carrying Sheoldred's agents have a tendency to drift far off course as they sail through the Voda Sea."
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea61871b-9295-4ea0-86ef-e7a11e23bda8.jpg?1783921340"
    }
}
