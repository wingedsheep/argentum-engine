package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gruesome Slaughter
 * {6}
 * Sorcery
 * Until end of turn, colorless creatures you control gain "{T}: This creature deals damage equal to
 * its power to target creature."
 *
 * The group is the colorless creatures you control as the spell resolves (CR 611.2c — a
 * characteristic-changing effect from a resolving spell locks in its affected set), so creatures
 * that arrive or turn colorless later in the turn gain nothing. Inside the granted ability "this
 * creature" is [EffectTarget.Self] — the creature the ability is on (Firja's Retribution's shape) —
 * and its power is read as the ability resolves.
 */
val GruesomeSlaughter = card("Gruesome Slaughter") {
    manaCost = "{6}"
    typeLine = "Sorcery"
    oracleText = "Until end of turn, colorless creatures you control gain \"{T}: This creature deals " +
        "damage equal to its power to target creature.\""

    spell {
        effect = Effects.GrantActivatedAbilityToGroup(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                val creature = target(TargetFilter.Creature)
                effect = Effects.DealDamage(
                    DynamicAmounts.sourcePower(),
                    creature,
                    damageSource = EffectTarget.Self,
                )
                description = "{T}: This creature deals damage equal to its power to target creature."
            },
            filter = GroupFilter(
                GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColorless).youControl()
            ),
            duration = Duration.EndOfTurn,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Aleksi Briclot"
        flavorText = "Though Gideon arrived at Sea Gate too late to prevent the massacre, he still had " +
            "enough time to lead its survivors to relative safety."
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d3e464c-3032-4579-8840-4fe75474e3fb.jpg?1783938224"
    }
}
