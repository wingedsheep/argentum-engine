package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AssignCombatDamageAsUnblocked
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Garruk, Savage Herald — Core Set 2021 #336 (canonical printing)
 * {4}{G}{G} · Legendary Planeswalker — Garruk · Starting loyalty 5
 *
 * +1: Reveal the top card of your library. If it's a creature card, put it into your hand.
 *     Otherwise, put it on the bottom of your library.
 * −2: Target creature you control deals damage equal to its power to another target creature.
 * −7: Until end of turn, creatures you control gain "You may have this creature assign its combat
 *     damage as though it weren't blocked."
 *
 * Modeling notes:
 *
 *  - **+1** is the reveal-top-one sort: a creature goes to hand, anything else to the bottom.
 *  - **−2** is the Markov Retribution / Hunter's Edge bite: the second slot is a [TargetOther]
 *    creature, the amount is the first target's power, and the first target is the damage source,
 *    so if either target is illegal on resolution no damage is dealt (ruling).
 *  - **−7** grants the quoted ability, so it is a per-creature [AssignCombatDamageAsUnblocked]
 *    (Thorn Elemental's printed static) granted to each creature you control at resolution via
 *    `ForEachInGroup` — creatures you begin to control later in the turn don't gain it (ruling).
 */
val GarrukSavageHerald = card("Garruk, Savage Herald") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Planeswalker — Garruk"
    startingLoyalty = 5
    oracleText = "+1: Reveal the top card of your library. If it's a creature card, put it into your hand. " +
        "Otherwise, put it on the bottom of your library.\n" +
        "−2: Target creature you control deals damage equal to its power to another target creature.\n" +
        "−7: Until end of turn, creatures you control gain \"You may have this creature assign its combat " +
        "damage as though it weren't blocked.\""

    loyaltyAbility(+1) {
        effect = Patterns.Library.revealTopPutAllMatchingToHand(1, GameObjectFilter.Creature)
    }

    loyaltyAbility(-2) {
        val yours = target(TargetFilter.Creature.youControl())
        val victim = target(TargetOther(TargetObject(filter = TargetFilter.Creature)))
        effect = Effects.DealDamage(DynamicAmounts.powerOf(yours), victim, damageSource = yours)
    }

    loyaltyAbility(-7) {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.GrantStaticAbility(AssignCombatDamageAsUnblocked(), EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "336"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48133629-4a5b-4c91-b496-2ee94f6cbc4c.jpg?1783930617"
        ruling("2020-06-23", "If either creature is an illegal target as Garruk's second ability tries to resolve, the creature you control won't deal damage to any creature.")
        ruling("2020-06-23", "When assigning combat damage, you choose for each creature whether you want to assign its damage to the creature blocking it, or if you want to assign it to the player or planeswalker the creature is attacking. You can't split the damage assignment between them.")
        ruling("2020-06-23", "Garruk's last ability affects only creatures you control at the time it resolves. Creatures you begin to control later in the turn won't gain the ability.")
    }
}
