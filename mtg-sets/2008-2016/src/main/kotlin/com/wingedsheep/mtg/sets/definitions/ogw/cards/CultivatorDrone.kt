package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Cultivator Drone
 * {2}{U}
 * Creature — Eldrazi Drone
 * 2/3
 * Devoid (This card has no color.)
 * {T}: Add {C}. Spend this mana only to cast a colorless spell, activate an ability of a colorless
 * permanent, or pay a cost that contains {C}. ({C} represents colorless mana.)
 *
 * The three clauses are one [ManaRestriction.AnyOf]: colorless spells and abilities of colorless
 * permanents ([ManaRestriction.ColorlessSpellsOrAbilitiesOnly], battlefield sources only), and any
 * cost containing {C} ([ManaRestriction.CostsContainingSymbolOnly]) — including a cost paid as an
 * effect resolves, such as Bearer of Silence's "you may pay {1}{C}" (2016-01-22 ruling).
 */
val CultivatorDrone = card("Cultivator Drone") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Drone"
    power = 2
    toughness = 3
    oracleText = "Devoid (This card has no color.)\n" +
        "{T}: Add {C}. Spend this mana only to cast a colorless spell, activate an ability of a colorless " +
        "permanent, or pay a cost that contains {C}. ({C} represents colorless mana.)"

    keywords(Keyword.DEVOID)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(
            1,
            restriction = ManaRestriction.AnyOf(
                listOf(
                    ManaRestriction.ColorlessSpellsOrAbilitiesOnly(allowAbilities = true, permanentSourcesOnly = true),
                    ManaRestriction.CostsContainingSymbolOnly(ManaSymbol.Colorless),
                )
            )
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {C}. Spend this mana only to cast a colorless spell, activate an ability of a " +
            "colorless permanent, or pay a cost that contains {C}."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "42"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbe4bb6f-bec6-49d2-99d7-e3750a0ea03f.jpg?1783937921"
    }
}
