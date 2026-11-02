package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sarkhan's Unsealing
 * {3}{R}
 * Enchantment
 * Whenever you cast a creature spell with power 4, 5, or 6, this enchantment deals 4 damage to
 * any target.
 * Whenever you cast a creature spell with power 7 or greater, this enchantment deals 4 damage to
 * each opponent and each creature and planeswalker they control.
 *
 * Both thresholds read the creature spell's power on the stack as it's cast (the cast event), so
 * +1/+1 counters it will enter with and later pumps don't count.
 */
val SarkhansUnsealing = card("Sarkhan's Unsealing") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast a creature spell with power 4, 5, or 6, this enchantment deals " +
        "4 damage to any target.\n" +
        "Whenever you cast a creature spell with power 7 or greater, this enchantment deals 4 damage " +
        "to each opponent and each creature and planeswalker they control."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature.powerAtLeast(4).powerAtMost(6))
        val t = target(Targets.Any)
        effect = Effects.DealDamage(4, t, damageSource = EffectTarget.Self)
        description = "Whenever you cast a creature spell with power 4, 5, or 6, this enchantment " +
            "deals 4 damage to any target."
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature.powerAtLeast(7))
        effect = Effects.DealDamage(4, EffectTarget.PlayerRef(Player.EachOpponent), damageSource = EffectTarget.Self) then
            Patterns.Group.dealDamageToAll(4, GroupFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))
        description = "Whenever you cast a creature spell with power 7 or greater, this enchantment " +
            "deals 4 damage to each opponent and each creature and planeswalker they control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "155"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/edfa8c8c-8013-46d5-8e80-3d9285fe81c8.jpg?1783934548"
        ruling("2018-07-13", "If you cast a creature spell that will enter the battlefield with a number of +1/+1 counters, such as Hungering Hydra, those counters aren't considered when determining whether either ability of Sarkhan's Unsealing triggers. Similarly, effects that will raise the creature's power once it has entered the battlefield won't apply.")
        ruling("2018-07-13", "Each of the triggered abilities of Sarkhan's Unsealing resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2018-07-13", "If a creature card's power is written as * and an ability defines its power, that ability applies while the creature spell is on the stack. Sarkhan's unsealing checks its power only as you finish paying costs; it doesn't matter what the spell's power is as you begin to cast it or when the triggered ability resolves.")
    }
}
