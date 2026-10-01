package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Armored Scrapgorger
 * {1}{G}
 * Creature — Phyrexian Beast
 * 0/3
 *
 * This creature gets +3/+0 as long as it has three or more oil counters on it.
 * {T}: Add one mana of any color.
 * Whenever this creature becomes tapped, exile target card from a graveyard and put an oil counter
 * on this creature.
 *
 * Tapping for its own mana ability is a "becomes tapped" event, so every mana activation feeds the
 * trigger. With no card in any graveyard the trigger has no legal target and is removed from the
 * stack, so no oil counter is added (one target, one ability: illegal target → nothing resolves).
 */
val ArmoredScrapgorger = card("Armored Scrapgorger") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Phyrexian Beast"
    power = 0
    toughness = 3
    oracleText = "This creature gets +3/+0 as long as it has three or more oil counters on it.\n" +
        "{T}: Add one mana of any color.\n" +
        "Whenever this creature becomes tapped, exile target card from a graveyard and put an oil " +
        "counter on this creature."

    staticAbility {
        condition = Conditions.SourceCounterCountAtLeast(CounterType.OIL, 3)
        ability = ModifyStats(3, 0, GroupFilter.source())
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        val t = target(TargetFilter.CardInGraveyard)
        effect = Effects.Move(t, Zone.EXILE) then
            Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self)
        description = "Whenever this creature becomes tapped, exile target card from a graveyard and " +
            "put an oil counter on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "158"
        artist = "Martin de Diego Sádaba"
        flavorText = "\"Eat. Sort. Eat. Sort. Eat.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61e220d0-38c9-4584-940b-8a9e983ecfe7.jpg?1783918019"
    }
}
