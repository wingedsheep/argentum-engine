package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Accomplished Alchemist — Strixhaven: School of Mages #119 (canonical printing)
 * {3}{G} · Creature — Elf Druid · 2/5
 *
 * {T}: Add one mana of any color.
 * {T}: Add X mana of any one color, where X is the amount of life you gained this turn.
 *
 * Two mana abilities: a plain any-color tap, and the Kami of Whispered Hopes shape
 * ([Effects.AddAnyColorMana] over a dynamic amount) reading the LIFE_GAINED turn tracker.
 */
val AccomplishedAlchemist = card("Accomplished Alchemist") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    oracleText = "{T}: Add one mana of any color.\n" +
        "{T}: Add X mana of any one color, where X is the amount of life you gained this turn."
    power = 2
    toughness = 5

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddAnyColorMana(DynamicAmounts.lifeGainedThisTurn())
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "119"
        artist = "Randy Vargas"
        flavorText = "\"Tincture of murkroot and lizard feet? Simple. One that doesn't taste like murkroot and lizard feet? I'm working on it.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d7b7830-b65e-4c53-98e8-152026764e4b.jpg?1783927349"
        ruling("4/16/2021", "Accomplished Alchemist's second ability counts the total amount of life gained without taking into account any life you lost during that turn. For example, if you lost 3 life and gained 3 life earlier in the turn, you'll add three mana.")
    }
}
