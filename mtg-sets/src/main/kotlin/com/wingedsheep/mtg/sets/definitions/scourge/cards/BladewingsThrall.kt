package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bladewing's Thrall
 * {2}{B}{B}
 * Creature — Zombie
 * 3/3
 * Bladewing's Thrall has flying as long as you control a Dragon.
 * When a Dragon enters, you may return Bladewing's Thrall from your
 * graveyard to the battlefield.
 *
 * Note: Per ruling (2017-11-17), the second ability triggers even if a Dragon
 * enters the battlefield under an opponent's control.
 */
val BladewingsThrall = card("Bladewing's Thrall") {
    manaCost = "{2}{B}{B}"
    typeLine = "Creature — Zombie"
    oracleText = "This creature has flying as long as you control a Dragon.\nWhen a Dragon enters, you may return this card from your graveyard to the battlefield."
    power = 3
    toughness = 3

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FLYING, StaticTarget.SourceCreature),
            condition = Conditions.ControlCreatureOfType(Subtype("Dragon"))
        )
    }

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.withSubtype("Dragon"),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.ANY
        )
        triggerZone = Zone.GRAVEYARD
        effect = MayEffect(Effects.PutOntoBattlefield(EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Kev Walker"
        ruling("2017-11-17", "The second ability triggers even if a Dragon enters the battlefield under an opponent's control.")
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f07e0d28-6383-4846-89d3-72910a7bbdcd.jpg?1562536719"
    }
}
