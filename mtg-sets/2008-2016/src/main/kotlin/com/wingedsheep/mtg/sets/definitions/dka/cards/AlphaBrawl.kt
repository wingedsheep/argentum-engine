package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Alpha Brawl — Dark Ascension #82 (canonical printing)
 * {6}{R}{R}
 * Sorcery
 * Target creature an opponent controls deals damage equal to its power to each other creature that
 * player controls, then each of those creatures deals damage equal to its power to that creature.
 *
 * Two sequential `ForEachInGroup` passes over the same group — the target's controller's other
 * creatures (`targetPlayerControls(TargetController)` + `otherThanTarget()`, Fear, Fire, Foes!'s
 * shape). First the target is the damage source for each of them (Widespread Brutality's shape);
 * then each of them is the source of damage equal to its own power to the target (Polukranos's
 * reverse half). Powers are read as each damage is dealt, so -1/-1 counters from an infect/wither
 * target shrink the return damage, and no state-based actions run between the halves, so creatures
 * dealt lethal damage still hit back. None of it is combat damage.
 */
val AlphaBrawl = card("Alpha Brawl") {
    manaCost = "{6}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Target creature an opponent controls deals damage equal to its power to each other " +
        "creature that player controls, then each of those creatures deals damage equal to its power " +
        "to that creature."

    spell {
        val creature = target(TargetFilter.CreatureOpponentControls)
        val others = GroupFilter(
            GameObjectFilter.Creature.targetPlayerControls(EffectTarget.TargetController)
        ).otherThanTarget()
        effect = Effects.ForEachInGroup(
            filter = others,
            effect = Effects.DealDamage(
                amount = DynamicAmounts.powerOf(creature),
                target = EffectTarget.IterationEntity,
                damageSource = creature,
            ),
        ) then Effects.ForEachInGroup(
            filter = others,
            effect = Effects.DealDamage(
                amount = DynamicAmounts.powerOf(EffectTarget.IterationEntity),
                target = creature,
                damageSource = EffectTarget.IterationEntity,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "82"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2ec168a-3e4f-4527-901a-bc28cc28d125.jpg?1783940822"
        ruling("2011-01-22", "The two parts of Alpha Brawl's effect happen sequentially: first the targeted creature deals damage, then the other creatures deal damage. State-based actions aren't checked in the middle, so creatures that are dealt lethal damage by the targeted creature will still deal damage.")
        ruling("2011-01-22", "None of the damage dealt due to Alpha Brawl is combat damage.")
        ruling("2011-01-22", "Abilities like lifelink and deathtouch that care about any kind of damage are taken into account.")
        ruling("2011-01-22", "If the targeted creature has infect or wither, the damage it deals will result in -1/-1 counters being put on each creature it deals damage to. The power of those creatures will be reduced when they deal damage back to the targeted creature.")
        ruling("2011-01-22", "The damage dealt by the targeted creature is not divided in any way. For example, if a 3/3 creature is targeted, it will deal 3 damage to each other creature controlled by its controller.")
    }
}
