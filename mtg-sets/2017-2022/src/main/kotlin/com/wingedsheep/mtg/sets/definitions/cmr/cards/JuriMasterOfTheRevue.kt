package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Juri, Master of the Revue
 * {B}{R}
 * Legendary Creature — Human Shaman
 * 1/1
 *
 * Whenever you sacrifice a permanent, put a +1/+1 counter on Juri.
 * When Juri dies, it deals damage equal to its power to any target.
 *
 * The sacrifice trigger is per-permanent with ANY binding (CR 603.2c), as Assay reads it. The dies
 * trigger reads Juri's last-known power, per the ruling.
 */
val JuriMasterOfTheRevue = card("Juri, Master of the Revue") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Human Shaman"
    power = 1
    toughness = 1
    oracleText = "Whenever you sacrifice a permanent, put a +1/+1 counter on Juri.\n" +
        "When Juri dies, it deals damage equal to its power to any target."

    triggeredAbility {
        trigger = Triggers.you.sacrifices(GameObjectFilter.Permanent)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val t = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.sourcePower(), t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "282"
        artist = "Dmitry Burmak"
        flavorText = "\"What a show! We're going to make a killing tonight.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/e/aec6b2b7-625a-451d-bc0b-4456f5bf2719.jpg?1783928771"

        ruling(
            "2020-11-10",
            "For Juri's second ability, use its power from when it was last on the battlefield to " +
                "determine how much damage is dealt. If that power was 0 or less, Juri deals no damage."
        )
    }
}
