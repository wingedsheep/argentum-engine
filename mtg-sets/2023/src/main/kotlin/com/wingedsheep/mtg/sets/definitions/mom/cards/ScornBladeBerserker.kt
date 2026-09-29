package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scorn-Blade Berserker
 * {B}
 * Creature — Human Berserker
 * 0/1
 * Backup 1
 * {1}, Sacrifice this creature: Draw a card.
 */
val ScornBladeBerserker = card("Scorn-Blade Berserker") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Berserker"
    oracleText = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If that's " +
        "another creature, it gains the following ability until end of turn.)\n" +
        "{1}, Sacrifice this creature: Draw a card."
    power = 0
    toughness = 1

    triggeredAbility {
        isBackup = true
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.If(
                condition = Conditions.Not(Conditions.TargetIsSource(0)),
                then = Effects.GrantActivatedAbility(
                    ability = grantedActivatedAbility {
                        cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeSelf)
                        effect = Effects.DrawCards(1)
                    },
                    target = creature,
                ),
            )
        description = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If that's " +
            "another creature, it gains the following ability until end of turn.)"
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeSelf)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "124"
        artist = "Tuan Duong Chu"
        flavorText = "\"Before I fall, I will taste the blood of Sarulf himself!\""
        imageUri = "https://cards.scryfall.io/normal/front/7/9/792e7386-4c2d-4fa9-b499-fa3681f2a50e.jpg?1783917003"
    }
}
