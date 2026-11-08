package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brigone, Soldier of Meletis — Foundations Jumpstart #30 (canonical printing)
 * {1}{W} · Legendary Creature — Human Soldier · 2/2
 *
 * Vigilance
 * Heroic — Whenever you cast a spell that targets Brigone, put a +1/+1 counter on Brigone.
 * {T}, Remove a +1/+1 counter from Brigone: Draw a card.
 *
 * Heroic is the `TargetsSource` cast trigger (Akroan Skyguard); the draw is [Costs.Composite] of
 * [Costs.Tap] and [Costs.RemoveCounterFromSelf] (Serum Tank's shape, minus the mana).
 */
val BrigoneSoldierOfMeletis = card("Brigone, Soldier of Meletis") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Vigilance\n" +
        "Heroic — Whenever you cast a spell that targets Brigone, put a +1/+1 counter on Brigone.\n" +
        "{T}, Remove a +1/+1 counter from Brigone: Draw a card."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Heroic — Whenever you cast a spell that targets Brigone, put a +1/+1 counter on Brigone."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.RemoveCounterFromSelf(CounterType.PLUS_ONE_PLUS_ONE, 1)
        )
        effect = Effects.DrawCards(1)
        description = "{T}, Remove a +1/+1 counter from Brigone: Draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "30"
        artist = "Daisuke Tatsuma"
        flavorText = "\"Gods, guide my hand!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20ecadef-8aa3-4bca-8744-adbc809083c3.jpg?1783908862"
        ruling(
            "2024-11-08",
            "Brigone's second ability resolves before the spell that caused it to trigger. It resolves even if " +
                "that spell is countered or otherwise leaves the stack without resolving."
        )
    }
}
