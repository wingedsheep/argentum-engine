package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wildfire Cerberus
 * {4}{R}
 * Creature — Dog
 * 4/3
 * {5}{R}{R}: Monstrosity 1. (If this creature isn't monstrous, put a +1/+1 counter on it and it becomes monstrous.)
 * When this creature becomes monstrous, it deals 2 damage to each opponent and each creature they control.
 */
val WildfireCerberus = card("Wildfire Cerberus") {
    manaCost = "{4}{R}"
    typeLine = "Creature — Dog"
    power = 4
    toughness = 3
    oracleText = "{5}{R}{R}: Monstrosity 1. (If this creature isn't monstrous, put a +1/+1 counter on it and it becomes monstrous.)\n" +
        "When this creature becomes monstrous, it deals 2 damage to each opponent and each creature they control."

    activatedAbility {
        cost = Costs.Mana("{5}{R}{R}")
        effect = Effects.Monstrosity(1)
    }

    triggeredAbility {
        trigger = Triggers.self.becomesMonstrous()
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Patterns.Group.dealDamageToAll(2, GroupFilter.AllCreaturesOpponentsControl)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7a26c23-abb9-41c3-aea1-bb241093119d.jpg?1783939416"
        ruling("2014-04-26", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2014-04-26", "Monstrous isn't an ability that a creature has. It's just something true about that creature. If the creature stops being a creature or loses its abilities, it will continue to be monstrous.")
        ruling("2014-04-26", "An ability that triggers when a creature becomes monstrous won't trigger if that creature isn't on the battlefield when its monstrosity ability resolves.")
    }
}
