package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kolaghan Aspirant
 * {1}{R}
 * Creature — Human Warrior
 * 2/1
 * Whenever this creature becomes blocked by a creature, this creature deals 1 damage to that creature.
 *
 * `Triggers.self.becomesBlocked(by = Creature)` fires once per blocker with that blocker as the
 * triggering entity (Acolyte of the Inferno's shape), so each creature in a gang block is dealt 1
 * damage by its own trigger, before combat damage. The Aspirant is the damage source.
 */
val KolaghanAspirant = card("Kolaghan Aspirant") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 1
    oracleText = "Whenever this creature becomes blocked by a creature, this creature deals 1 damage to that creature."

    triggeredAbility {
        trigger = Triggers.self.becomesBlocked(by = GameObjectFilter.Creature)
        effect = Effects.DealDamage(1, EffectTarget.TriggeringEntity)
        description = "Whenever this creature becomes blocked by a creature, this creature deals 1 damage to that creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "143"
        artist = "Aaron Miller"
        flavorText = "She answers the call of the Crave, the desire for battle sated only by bloodshed."
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa17972e-c16b-4dec-a108-1c3fb0138a4e.jpg?1783938589"
        ruling(
            "2015-02-25",
            "If Kolaghan Aspirant becomes blocked by multiple creatures, its ability will trigger once for each of them."
        )
        ruling(
            "2015-02-25",
            "The damage Kolaghan Aspirant deals because of its triggered ability isn't combat damage. If that damage causes each creature blocking it to leave combat, Kolaghan Aspirant will remain blocked. It won't be dealt combat damage in this case."
        )
    }
}
