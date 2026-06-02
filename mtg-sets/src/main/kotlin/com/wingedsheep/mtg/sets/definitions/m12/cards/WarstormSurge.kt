package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Warstorm Surge
 * {5}{R}
 * Enchantment
 *
 * Whenever a creature you control enters, it deals damage equal to its power to any target.
 *
 * The triggering creature is the source of the damage (per CR 700.2 / 2011 ruling),
 * so protection from creatures, lifelink, deathtouch, and infect all check the
 * entering creature rather than Warstorm Surge itself.
 */
val WarstormSurge = card("Warstorm Surge") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control enters, it deals damage equal to its power to any target."

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl(),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.ANY
        )
        val any = target("any target", Targets.Any)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.triggeringPower(),
            target = any,
            damageSource = EffectTarget.TriggeringEntity
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "160"
        artist = "Raymond Swanland"
        flavorText = "\"Listen to the roar! Feel the thunder! The Immersturm shouts its approval with every bolt of lightning!\""
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b16443df-52c6-4c9d-a7ff-89a37e593a0a.jpg?1562655722"

        ruling("2011-09-22", "The creature that entered deals damage equal to its current power to the targeted permanent or player. If it's no longer on the battlefield, its last known existence on the battlefield is checked to determine its power.")
        ruling("2011-09-22", "Warstorm Surge is the source of the ability, but the creature is the source of the damage. The ability couldn't target a creature with protection from red, for example. It could target a creature with protection from creatures, but all the damage would be prevented. Since damage is dealt by the creature, abilities like lifelink, deathtouch and infect are taken into account, even if the creature has left the battlefield by the time it deals damage.")
    }
}
