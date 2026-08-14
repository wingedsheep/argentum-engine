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
 * Elvenking's Halls — The Hobbit #182
 * Land · Common
 *
 * This land enters tapped.
 * {T}: Add {G} or {U}.
 * {2}{G}{U}, {T}, Sacrifice this land: Put two +1/+1 counters on target Elf you control.
 * Activate only as a sorcery.
 *
 * The Elf member of the HOB tapland cycle; see [IronHills] for the shape.
 */
val ElvenkingsHalls = card("Elvenking's Halls") {
    manaCost = ""
    colorIdentity = "GU"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {G} or {U}.\n" +
        "{2}{G}{U}, {T}, Sacrifice this land: Put two +1/+1 counters on target Elf you control. " +
        "Activate only as a sorcery."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}{U}"), Costs.Tap, Costs.SacrificeSelf)
        val elf = target(
            "target Elf you control",
            TargetCreature(filter = TargetFilter.CreatureYouControl.withSubtype(Subtype.ELF))
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, elf)
        timing = TimingRule.SorcerySpeed
        description = "Put two +1/+1 counters on target Elf you control."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "182"
        artist = "Leon Tukker"
        flavorText = "Elf-guards sang as they marched along the twisting, crossing, and echoing paths."
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd477096-41b1-4907-9cb3-852cb22c9ba2.jpg?1785323590"
    }
}
