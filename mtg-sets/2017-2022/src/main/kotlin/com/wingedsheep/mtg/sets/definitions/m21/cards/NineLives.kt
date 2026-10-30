package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nine Lives — Core Set 2021 #28
 *
 * The counter is the rest of the prevention effect ([PreventDamage.onPrevented]), once per source's
 * damage event. It is put on even when the damage can't be prevented (CR 615.12: the prevention
 * effect still applies, prevents nothing, and its additional effect still happens).
 */
val NineLives = card("Nine Lives") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Hexproof\nIf a source would deal damage to you, prevent that damage and put an incarnation counter on this enchantment.\nWhen there are nine or more incarnation counters on this enchantment, exile it.\nWhen this enchantment leaves the battlefield, you lose the game."

    keywords(Keyword.HEXPROOF)

    replacementEffect(
        PreventDamage(
            appliesTo = EventPattern.DamageEvent(recipient = Recipient.You),
            onPrevented = Effects.AddCounters(CounterType.INCARNATION, 1, EffectTarget.Self)
        )
    )

    stateTriggeredAbility {
        condition = Conditions.SourceCounterCountAtLeast(CounterType.INCARNATION, 9)
        effect = Effects.Exile(EffectTarget.Self)
        description = "When there are nine or more incarnation counters on this enchantment, exile it."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.LoseGame()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Paul Scott Canavan"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e70b7a73-484e-48f1-944c-3d38866cdc20.jpg?1783930737"
        ruling("2020-06-23", "If you counter the first triggered ability or otherwise remove it from the stack, it will just trigger again immediately if Nine Lives still has nine or more incarnation counters on it.")
        ruling("2020-06-23", "If more than one source deals damage to you at once, prevent the damage from each of them and put that many incarnation counters on Nine Lives, even if this causes more than nine incarnation counters to be on Nine Lives. Leave it to cats to abuse loopholes.")
        ruling("2020-06-23", "The last ability of Nine Lives triggers no matter how it leaves the battlefield.")
        ruling("2020-06-23", "Once the first triggered ability is on the stack, it won't trigger again, even if more damage is prevented and more incarnation counters are put onto it.")
        ruling("2020-06-23", "If damage that a source would deal to you can't be prevented, you still put an incarnation counter on Nine Lives.")
    }
}
