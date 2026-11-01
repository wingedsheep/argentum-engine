package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tenth District Legionnaire — War of the Spark #222 (canonical printing)
 * {R}{W}
 * Creature — Human Soldier
 * 2/2
 * Haste
 * Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature,
 * then scry 1.
 *
 * The heroic-style cast trigger (Hero of the Nyxborn's `TargetsSource` predicate) with a
 * counter-then-scry body. The scry still happens if the Legionnaire has left the battlefield
 * by resolution — the counter simply has nowhere to go.
 */
val TenthDistrictLegionnaire = card("Tenth District Legionnaire") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Haste\nWhenever you cast a spell that targets this creature, put a +1/+1 counter on this creature, then scry 1."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.Scry(1)
        description = "Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature, then scry 1."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "222"
        artist = "Victor Adame Minguez"
        flavorText = "\"The Tenth is under heavy attack, but it's being handled. I sent Mileva.\"\n—Tajic"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44f3090b-917b-4122-b522-27c30dca8e69.jpg?1783933383"
        ruling("2019-05-03", "Tenth District Legionnaire's ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2019-05-03", "You scry 1 even if you can't put a +1/+1 counter on Tenth District Legionnaire, most likely because it has left the battlefield.")
    }
}
