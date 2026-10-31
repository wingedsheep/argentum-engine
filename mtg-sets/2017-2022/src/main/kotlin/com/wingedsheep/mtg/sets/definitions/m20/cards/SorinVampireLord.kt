package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sorin, Vampire Lord
 * {4}{B}{B}
 * Legendary Planeswalker — Sorin
 * Loyalty: 4
 *
 * +1: Up to one target creature gets +2/+0 until end of turn.
 * −2: Sorin deals 4 damage to any target. You gain 4 life.
 * −8: Until end of turn, each Vampire you control gains "{T}: Gain control of target creature."
 *
 * The −8 grants the tap ability only to the Vampires you control as it resolves (locked-in group,
 * Firja's Retribution shape); the control change it grants is permanent.
 */
val SorinVampireLord = card("Sorin, Vampire Lord") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Planeswalker — Sorin"
    startingLoyalty = 4
    oracleText = "+1: Up to one target creature gets +2/+0 until end of turn.\n" +
        "−2: Sorin deals 4 damage to any target. You gain 4 life.\n" +
        "−8: Until end of turn, each Vampire you control gains \"{T}: Gain control of target creature.\""

    loyaltyAbility(+1) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.ModifyStats(2, 0, creature)
        description = "Up to one target creature gets +2/+0 until end of turn."
    }

    loyaltyAbility(-2) {
        val any = target(Targets.Any)
        effect = Effects.DealDamage(4, any) then Effects.GainLife(4)
        description = "Sorin deals 4 damage to any target. You gain 4 life."
    }

    loyaltyAbility(-8) {
        effect = Effects.GrantActivatedAbilityToGroup(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                val creature = target(TargetFilter.Creature)
                effect = Effects.GainControl(creature)
            },
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Vampire").youControl()),
            duration = Duration.EndOfTurn,
        )
        description = "Until end of turn, each Vampire you control gains \"{T}: Gain control of target creature.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "290"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf20ea7b-e480-4b0e-b080-888f24d3c08d.jpg?1783932920"
    }
}
