package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tainted Adversary — Innistrad: Midnight Hunt #124
 * {1}{B} · Creature — Zombie · 2/3
 *
 * Deathtouch
 * When this creature enters, you may pay {2}{B} any number of times. When you pay this cost one
 * or more times, put that many +1/+1 counters on this creature, then create twice that many 2/2
 * black Zombie creature tokens with decayed.
 *
 * The repeated payment is [Effects.PayRepeatedly] under an optional reflexive trigger (CR 603.12),
 * as on Intrepid and Spectral Adversary. The reflexive half reads the repetition count back with
 * [DynamicAmounts.timesPaid]: that many +1/+1 counters, then twice that many decayed Zombies.
 */
val TaintedAdversary = card("Tainted Adversary") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 3
    oracleText = "Deathtouch\n" +
        "When this creature enters, you may pay {2}{B} any number of times. When you pay this cost " +
        "one or more times, put that many +1/+1 counters on this creature, then create twice that " +
        "many 2/2 black Zombie creature tokens with decayed. (A creature with decayed can't block. " +
        "When it attacks, sacrifice it at end of combat.)"

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayRepeatedly("{2}{B}"),
            optional = true,
            reflexiveEffect = Effects.AddDynamicCounters(
                CounterType.PLUS_ONE_PLUS_ONE,
                DynamicAmounts.timesPaid(),
                EffectTarget.Self
            ) then Effects.CreateToken(
                count = DynamicAmounts.timesPaid() * 2,
                power = 2,
                toughness = 2,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Zombie"),
                keywords = setOf(Keyword.DECAYED),
                imageUri = "https://cards.scryfall.io/normal/front/6/a/6adb8607-1066-451d-a719-74ad32358278.jpg?1783925226",
            ),
            descriptionOverride = "You may pay {2}{B} any number of times. When you pay this cost " +
                "one or more times, put that many +1/+1 counters on this creature, then create " +
                "twice that many 2/2 black Zombie creature tokens with decayed."
        )
        description = "When this creature enters, you may pay {2}{B} any number of times. When you " +
            "pay this cost one or more times, put that many +1/+1 counters on this creature, then " +
            "create twice that many 2/2 black Zombie creature tokens with decayed."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "124"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6b9b8fc-c30d-49e7-bb6c-219380d73a82.jpg?1783925608"
        ruling(
            "2021-09-24",
            "After you pay the {2}{B} cost one or more times, a second ability triggers. Players " +
                "may respond to the new ability at that point. As that ability resolves, Tainted " +
                "Adversary gets its counters and the tokens are created."
        )
    }
}
