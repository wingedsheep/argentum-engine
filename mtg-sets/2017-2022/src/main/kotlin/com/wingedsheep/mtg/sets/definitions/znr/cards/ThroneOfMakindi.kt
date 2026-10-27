package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Throne of Makindi — Zendikar Rising #265
 * Land
 *
 * {T}: Add {C}.
 * {1}, {T}: Put a charge counter on this land.
 * {T}, Remove a charge counter from this land: Add two mana of any one color. Spend this mana
 * only to cast kicked spells.
 *
 * The kicked-spell mana is [ManaRestriction.KickedSpellsOnly] (as on Elfhame Druid): both mana are
 * one chosen color, added once.
 */
val ThroneOfMakindi = card("Throne of Makindi") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{1}, {T}: Put a charge counter on this land.\n" +
        "{T}, Remove a charge counter from this land: Add two mana of any one color. " +
        "Spend this mana only to cast kicked spells."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self)
        description = "{1}, {T}: Put a charge counter on this land."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.RemoveCounterFromSelf(CounterType.CHARGE, 1))
        effect = Effects.AddManaOfChoice(amount = 2, restriction = ManaRestriction.KickedSpellsOnly)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "265"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5a0563e-c83b-40df-abf6-51c83bf6792d.jpg"
    }
}
