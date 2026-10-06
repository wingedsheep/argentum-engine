package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chandra, Flame's Fury
 * {4}{R}{R}
 * Legendary Planeswalker — Chandra
 * +1: Chandra deals 2 damage to any target.
 * −2: Chandra deals 4 damage to target creature and 2 damage to that creature's controller.
 * −8: Chandra deals 10 damage to target player and each creature that player controls.
 */
val ChandraFlamesFury = card("Chandra, Flame's Fury") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Chandra"
    startingLoyalty = 4
    oracleText = "+1: Chandra deals 2 damage to any target.\n−2: Chandra deals 4 damage to target creature and 2 damage to that creature's controller.\n−8: Chandra deals 10 damage to target player and each creature that player controls."

    loyaltyAbility(+1) {
        val any = target(Targets.Any)
        effect = Effects.DealDamage(2, any)
    }

    loyaltyAbility(-2) {
        val creature = target(TargetFilter.Creature)
        // "That creature's controller" is not targeted; TargetController falls back to
        // last-known information if the creature is gone by then.
        effect = Effects.DealDamage(4, creature) then
            Effects.DealDamage(2, EffectTarget.TargetController)
    }

    loyaltyAbility(-8) {
        // Only the player is targeted — hexproof creatures they control are still dealt damage.
        val player = target(Targets.Player)
        effect = Effects.DealDamage(10, player) then
            Patterns.Group.dealDamageToAll(10, GroupFilter(GameObjectFilter.Creature.targetPlayerControls(player)))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "294"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d1c6c4b-62ab-4d44-a2d7-64b44d163606.jpg?1783932919"
        ruling("2019-07-12", "If the target creature is an illegal target by the time Chandra's second ability resolves, the ability doesn't resolve. No player is dealt damage. Similarly, if the target player is an illegal target by the time Chandra's last ability resolves, no creatures are dealt damage.")
        ruling("2019-07-12", "Chandra's last ability targets the player, not their creatures. A creature with hexproof may be dealt damage this way.")
    }
}
