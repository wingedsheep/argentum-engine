package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.DealsDamageEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kazarov, Sengir Pureblood
 * {5}{B}{B}
 * Legendary Creature — Vampire
 * Flying
 * Whenever a creature an opponent controls is dealt damage, put a +1/+1 counter on Kazarov, Sengir Pureblood.
 * {3}{R}: Kazarov, Sengir Pureblood deals 2 damage to target creature.
 * 4/4
 */
val KazarovSengirPureblood = card("Kazarov, Sengir Pureblood") {
    manaCost = "{5}{B}{B}"
    typeLine = "Legendary Creature — Vampire"
    oracleText = "Flying\nWhenever a creature an opponent controls is dealt damage, put a +1/+1 counter on Kazarov, Sengir Pureblood.\n{3}{R}: Kazarov, Sengir Pureblood deals 2 damage to target creature."
    power = 4
    toughness = 4

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = TriggerSpec(DealsDamageEvent(recipient = RecipientFilter.CreatureOpponentControls), TriggerBinding.ANY)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Mana("{3}{R}")
        val creature = target("creature", Targets.Creature)
        effect = Effects.DealDamage(2, creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "96"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2aff7077-496b-46ca-b9d7-a8c1772f9a91.jpg?1562733186"
        ruling("2018-04-27", "If Kazarov is dealt damage at the same time that a creature an opponent controls is dealt damage, Kazarov must survive the damage to get a +1/+1 counter.")
        ruling("2018-04-27", "Kazarov's triggered ability triggers once for each creature dealt damage at one time.")
        ruling("2018-04-27", "If a creature is dealt an amount of damage \"for each\" of something, that damage is dealt as one event and Kazarov's triggered ability triggers only once.")
    }
}
