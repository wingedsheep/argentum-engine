package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stormwild Capridor — Ikoria: Lair of Behemoths #34
 * {2}{W} · Creature — Bird Goat · 1/3
 *
 * A prevention effect (Purity's noncombat filter on Vigor's shape): [PreventDamage] scoped to
 * noncombat damage dealt to this creature, whose [PreventDamage.onPrevented] rider puts one +1/+1
 * counter on it per point this application actually prevented ([DynamicAmounts.preventedDamage]).
 * Damage that can't be prevented is dealt normally and places no counters.
 */
val StormwildCapridor = card("Stormwild Capridor") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Goat"
    power = 1
    toughness = 3
    oracleText = "Flying\nIf noncombat damage would be dealt to this creature, prevent that damage. Put a +1/+1 counter on this creature for each 1 damage prevented this way."

    keywords(Keyword.FLYING)

    replacementEffect(
        PreventDamage(
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Self,
                damageType = DamageType.NonCombat
            ),
            onPrevented = Effects.AddDynamicCounters(
                CounterType.PLUS_ONE_PLUS_ONE, DynamicAmounts.preventedDamage(), EffectTarget.Self
            )
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "34"
        artist = "Dmitry Burmak"
        flavorText = "After a disastrous day of battle, Lavabrink's lightning mages were recalled."
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2afded81-2fc1-4285-bf48-d25f72e72138.jpg?1783931082"
        ruling("2020-04-17", "If damage that can’t be prevented is dealt to Stormwild Capridor, it doesn’t get +1/+1 counters for that damage.")
        ruling("2020-04-17", "If damage that can be prevented is dealt to Stormwild Capridor at the same time as damage that can’t be prevented, it gets +1/+1 counters for the prevented damage before checking to see if it dies from the damage that couldn’t be prevented.")
        ruling("2020-04-17", "If multiple replacement or prevention effects try to modify damage that would be dealt to a creature, the controller of the creature chooses the order in which they apply.")
    }
}
