package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hero of Leina Tower — Born of the Gods #123 (canonical printing)
 * {G}
 * Creature — Human Warrior
 * 1/1
 * Heroic — Whenever you cast a spell that targets this creature, you may pay {X}. If you do, put X
 * +1/+1 counters on this creature.
 *
 * X is chosen and paid as the trigger resolves ([Effects.MayPayX]), and the counters go on in the
 * same resolution ("If you do", not a reflexive "when you do") — matching the 2018-12-07 ruling.
 */
val HeroOfLeinaTower = card("Hero of Leina Tower") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    power = 1
    toughness = 1
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, you may pay {X}. " +
        "If you do, put X +1/+1 counters on this creature."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.MayPayX(
            then = Effects.AddDynamicCounters(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                amount = DynamicAmounts.xValue(),
                target = EffectTarget.Self,
            ),
            descriptionOverride = "You may pay {X}. If you do, put X +1/+1 counters on this creature.",
        )
        description = "Heroic — Whenever you cast a spell that targets this creature, you may pay {X}. " +
            "If you do, put X +1/+1 counters on this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "123"
        artist = "Aaron Miller"
        flavorText = "\"You cannot achieve greatness if its seed has not already been planted in your heart.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f022c0c3-6331-47be-b07f-9768930afd3a.jpg?1783939534"
        ruling("2018-12-07", "The amount to pay for {X} is chosen as the triggered ability resolves. No player may take actions between the time you pay {X} and the time Hero of Leina Tower has X +1/+1 counters on it.")
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
