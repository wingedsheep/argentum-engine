package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter

/**
 * Hallar, the Firefletcher
 * {1}{R}{G}
 * Legendary Creature — Elf Archer
 * 3/3
 * Trample
 * Whenever you cast a spell, if that spell was kicked, put a +1/+1 counter on Hallar,
 * then Hallar deals damage equal to the number of +1/+1 counters on it to each opponent.
 */
val HallarTheFirefletcher = card("Hallar, the Firefletcher") {
    manaCost = "{1}{R}{G}"
    typeLine = "Legendary Creature — Elf Archer"
    power = 3
    toughness = 3
    oracleText = "Trample\nWhenever you cast a spell, if that spell was kicked, put a +1/+1 counter on Hallar, the Firefletcher, then Hallar deals damage equal to the number of +1/+1 counters on it to each opponent."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.YouCastKickedSpell
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
            .then(Effects.DealDamage(
                DynamicAmounts.countersOnSelf(CounterTypeFilter.PlusOnePlusOne),
                EffectTarget.PlayerRef(Player.EachOpponent)
            ))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "196"
        artist = "Bram Sels"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c896643e-eeef-49a6-a1ca-2577b55af2b0.jpg?1562742764"
        ruling("2018-04-27", "Hallar's last ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
    }
}
