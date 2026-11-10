package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mirrorpool — Oath of the Gatewatch #174
 * Land
 * This land enters tapped.
 * {T}: Add {C}.
 * {2}{C}, {T}, Sacrifice this land: Copy target instant or sorcery spell you control. You may
 * choose new targets for the copy.
 * {4}{C}, {T}, Sacrifice this land: Create a token that's a copy of target creature you control.
 *
 * The spell copy is Lithoform Engine's second ability; the token copy is Littjara Mirrorlake's
 * without the counter exception. Neither ability is sorcery-speed. The {C} in each cost is
 * colorless mana specifically, not generic.
 */
val Mirrorpool = card("Mirrorpool") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {C}.\n" +
        "{2}{C}, {T}, Sacrifice this land: Copy target instant or sorcery spell you control. " +
        "You may choose new targets for the copy.\n" +
        "{4}{C}, {T}, Sacrifice this land: Create a token that's a copy of target creature you control."

    replacementEffect(EntersTapped())

    // {T}: Add {C}.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
    }

    // {2}{C}, {T}, Sacrifice this land: Copy target instant or sorcery spell you control.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{C}"),
            Costs.Tap,
            Costs.SacrificeSelf
        )
        val spell = target(TargetFilter.InstantOrSorcerySpellOnStack.youControl())
        effect = Effects.CopyTargetSpell(target = spell)
        description = "Copy target instant or sorcery spell you control. You may choose new targets for the copy."
    }

    // {4}{C}, {T}, Sacrifice this land: Create a token that's a copy of target creature you control.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{4}{C}"),
            Costs.Tap,
            Costs.SacrificeSelf
        )
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.CreateTokenCopyOfTarget(target = creature)
        description = "Create a token that's a copy of target creature you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "174"
        artist = "Cliff Childs"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/edbea2b5-eed3-464b-9d5c-9591f309f298.jpg?1783937891"
        ruling(
            "2016-01-22",
            "Mirrorpool's third ability can target (and copy) any instant or sorcery spell, not just one with targets."
        )
        ruling(
            "2016-01-22",
            "For Mirrorpool's last ability, the token copies exactly what's printed on the original creature and " +
                "nothing else (unless that creature is copying something else). It doesn't copy whether that " +
                "creature is tapped or untapped, whether it has any counters on it or Auras and Equipment attached " +
                "to it, or any non-copy effects that have changed its power, toughness, types, color, and so on."
        )
    }
}
