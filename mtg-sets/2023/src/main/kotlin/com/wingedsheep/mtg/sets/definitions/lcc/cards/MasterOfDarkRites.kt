package com.wingedsheep.mtg.sets.definitions.lcc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Master of Dark Rites
 * {B}
 * Creature — Vampire Cleric
 * 1/1
 * {T}, Sacrifice another creature: Add {B}{B}{B}. Spend this mana only to cast Vampire, Cleric,
 * and/or Demon spells.
 *
 * "Vampire, Cleric, and/or Demon spells" is [ManaRestriction.SubtypeSpellsOnly] over the three
 * subtypes: a spell with any one of them may spend the mana.
 */
val MasterOfDarkRites = card("Master of Dark Rites") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Cleric"
    oracleText = "{T}, Sacrifice another creature: Add {B}{B}{B}. Spend this mana only to cast " +
        "Vampire, Cleric, and/or Demon spells."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeAnother(GameObjectFilter.Creature))
        effect = Effects.AddMana(
            color = Color.BLACK,
            amount = 3,
            restriction = ManaRestriction.SubtypeSpellsOnly(setOf("Vampire", "Cleric", "Demon"))
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}, Sacrifice another creature: Add {B}{B}{B}. Spend this mana only to cast " +
            "Vampire, Cleric, and/or Demon spells."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "83"
        artist = "Igor Kieryluk"
        flavorText = "\"With each sacrifice, we step closer to the Age of Everflowing Blood.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/3/6394a522-37bb-4cf3-b434-5121c8000ac6.jpg?1783913910"
    }
}
