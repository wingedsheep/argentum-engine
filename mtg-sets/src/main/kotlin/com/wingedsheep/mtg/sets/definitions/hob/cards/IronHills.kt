package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Iron Hills — The Hobbit #185
 * Land · Common
 *
 * This land enters tapped.
 * {T}: Add {R} or {W}.
 * {2}{R}{W}, {T}, Sacrifice this land: Put two +1/+1 counters on target Dwarf you control.
 * Activate only as a sorcery.
 *
 * The tribal half of the HOB tapland cycle. "Add {R} or {W}" is two separate mana abilities (one per
 * color), matching how every other dual tapland in the engine is modelled — the player picks which
 * ability to activate rather than making a choice on resolution.
 *
 * The sacrifice ability is a normal activated ability with [TimingRule.SorcerySpeed] for "Activate
 * only as a sorcery". It targets, so it is simply removed from the stack if no Dwarf you control is a
 * legal target when it would be put there (CR 603.3d) — but the cost, land and all, is already paid.
 */
val IronHills = card("Iron Hills") {
    manaCost = ""
    colorIdentity = "RW"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {R} or {W}.\n" +
        "{2}{R}{W}, {T}, Sacrifice this land: Put two +1/+1 counters on target Dwarf you control. " +
        "Activate only as a sorcery."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}{W}"), Costs.Tap, Costs.SacrificeSelf)
        val dwarf = target(
            "target Dwarf you control",
            TargetCreature(filter = TargetFilter.CreatureYouControl.withSubtype(Subtype.DWARF))
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, dwarf)
        timing = TimingRule.SorcerySpeed
        description = "Put two +1/+1 counters on target Dwarf you control."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "185"
        artist = "Marina Ortega Lorente"
        flavorText = "The hills provided quality iron, allowing Dáin's folk to clad themselves in " +
            "hauberks of steel mail and hoses of fine metal mesh."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78045c43-5cbe-48ff-837d-e7c6baac2937.jpg?1785323594"
    }
}
