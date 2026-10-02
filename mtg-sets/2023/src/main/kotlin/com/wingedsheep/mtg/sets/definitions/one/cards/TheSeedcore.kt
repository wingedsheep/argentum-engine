package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * The Seedcore
 * Land — Sphere
 *
 * {T}: Add {C}.
 * {T}: Add one mana of any color. Spend this mana only to cast Phyrexian creature spells.
 * Corrupted — {T}: Target 1/1 creature gets +2/+1 until end of turn. Activate only if an opponent
 * has three or more poison counters.
 *
 * The restricted mana is [ManaRestriction.SubtypeSpellsOrAbilitiesOnly] with `creatureOnly = true`:
 * only a creature spell with the Phyrexian subtype may spend it. "1/1 creature" reads the
 * creature's current power and toughness (projected), so a pumped token no longer qualifies.
 */
val TheSeedcore = card("The Seedcore") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land — Sphere"
    oracleText = "{T}: Add {C}.\n" +
        "{T}: Add one mana of any color. Spend this mana only to cast Phyrexian creature spells.\n" +
        "Corrupted — {T}: Target 1/1 creature gets +2/+1 until end of turn. Activate only if an opponent has three or more poison counters."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(
            1,
            restriction = ManaRestriction.SubtypeSpellsOrAbilitiesOnly("Phyrexian", creatureOnly = true)
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        val creature = target(
            TargetFilter(GameObjectFilter.Creature.power(1).toughnessAtLeast(1).toughnessAtMost(1))
        )
        cost = Costs.Tap
        effect = Effects.ModifyStats(2, 1, creature)
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.Corrupted))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "259"
        artist = "Kasia 'Kafis' Zielińska"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29c91aad-bf33-448e-b122-65940fb2e33b.jpg?1783917980"
    }
}
