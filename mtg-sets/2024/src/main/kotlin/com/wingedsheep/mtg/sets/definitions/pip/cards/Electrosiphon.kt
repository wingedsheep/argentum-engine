package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Electrosiphon
 * {U}{U}{R}
 * Instant
 *
 * Counter target spell. You get an amount of {E} (energy counters) equal to its mana value.
 *
 * "Its mana value" is the spell's as it was on the stack, where an {X} counts as the X chosen for
 * it (CR 202.3e). Once countered the card sits in its owner's graveyard with X back at 0, so the
 * number is stored before the counter and paid out after it — the order the oracle text reads.
 * A spell that can't be countered stays on the stack and still pays out its mana value.
 */
val Electrosiphon = card("Electrosiphon") {
    manaCost = "{U}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "Counter target spell. You get an amount of {E} (energy counters) equal to its mana value."

    spell {
        val spell = target(TargetFilter.SpellOnStack)
        effect = Effects.StoreNumber(SPELL_MANA_VALUE, DynamicAmounts.manaValueOf(spell)) then
            Effects.CounterSpell() then
            Effects.AddDynamicCounters(
                CounterType.ENERGY,
                DynamicAmounts.storedNumber(SPELL_MANA_VALUE),
                EffectTarget.Controller,
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "104"
        artist = "Antonio José Manzanedo"
        flavorText = "\"I calculate your chance of success to be . . . well, I don't want to be morbid . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/8/3/8377fce8-edc8-4926-bf75-57162ff6df9f.jpg?1783912388"
    }
}

private const val SPELL_MANA_VALUE = "electrosiphonManaValue"
