package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aragorn, Hornburg Hero
 * {1}{R}{G}{W}
 * Legendary Creature — Human Soldier
 * 4/4
 *
 * Attacking creatures you control have first strike and renown 1.
 * Whenever a renowned creature you control deals combat damage to a player, double the number of
 * +1/+1 counters on it.
 *
 * - The renown grant is the projected `RENOWN_1` keyword ([KeywordAbility.grantedRenown]); the
 *   engine derives one renown trigger per grant, beside any printed renown the creature has
 *   (CR 702.112c), and the attacking-state group filter drops it the moment combat ends.
 * - "Renowned" is read as the combat damage event is matched, so a creature that becomes renowned
 *   from that same hit's renown trigger does not double (ruling) — it wasn't renowned yet.
 */
val AragornHornburgHero = card("Aragorn, Hornburg Hero") {
    manaCost = "{1}{R}{G}{W}"
    colorIdentity = "RGW"
    typeLine = "Legendary Creature — Human Soldier"
    power = 4
    toughness = 4
    oracleText = "Attacking creatures you control have first strike and renown 1. (When a creature " +
        "with renown 1 deals combat damage to a player, if it isn't renowned, put a +1/+1 counter " +
        "on it and it becomes renowned.)\n" +
        "Whenever a renowned creature you control deals combat damage to a player, double the " +
        "number of +1/+1 counters on it."

    val attackers = GroupFilter(GameObjectFilter.Creature.attacking().youControl())

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE, attackers)
    }

    staticAbility {
        ability = GrantKeyword(KeywordAbility.grantedRenown(1), attackers)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().renowned())
            .dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.DoubleCounters(CounterType.PLUS_ONE_PLUS_ONE, EffectTarget.TriggeringEntity)
        description = "Whenever a renowned creature you control deals combat damage to a player, " +
            "double the number of +1/+1 counters on it."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "492"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14c62aa6-2660-4a18-8f69-4088e18abb40.jpg?1783914203"
        ruling("2023-11-03", "If a creature has multiple instances of renown, each triggers separately. The first one that resolves causes the creature to become renowned, which means any additional renown abilities that creature has will have no effect. The creature's controller chooses what order to put those renown abilities on the stack, so they can choose to have the ability with the higher renown number resolve first.")
        ruling("2023-11-03", "When a creature you control that isn't renowned deals combat damage to a player, you'll put a +1/+1 counter on it and it will become renowned. Since it wasn't renowned when it dealt combat damage, Aragorn, Hornburg Hero's last ability won't trigger, and you won't double the number of +1/+1 counters on it.")
        ruling("2023-11-03", "Renown won't trigger when a creature deals damage to a planeswalker, a battle, or another creature. It also won't trigger when a creature deals noncombat damage to a player.")
        ruling("2023-11-03", "If Aragorn, Hornburg Hero leaves the battlefield after first strike damage is dealt but before regular combat damage, creatures you control that dealt first strike damage but no longer have first strike won't also deal regular combat damage (unless they have double strike for some reason).")
    }
}
