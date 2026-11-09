package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hurska Sweet-Tooth — Foundations Jumpstart #53 (canonical printing)
 * {2}{G} · Legendary Creature — Bear · 3/3
 *
 * Whenever Hurska attacks, create a Food token.
 * Whenever you gain life, you may pay {G/W}. When you do, target creature gets +X/+X until end of
 * turn, where X is the amount of life you gained.
 *
 * "When you do" is a reflexive trigger (CR 603.12, Go-Shintai of Boundless Vigor's shape): the
 * hybrid {G/W} is paid as the life-gain trigger resolves, and only then does the pump go on the
 * stack and choose its target. X is the amount of *this* life-gain event
 * ([DynamicAmounts.triggerLifeGained]) — the reflexive ability inherits the resolving trigger's
 * record — and is read once, as the reflexive ability resolves.
 */
val HurskaSweetTooth = card("Hurska Sweet-Tooth") {
    manaCost = "{2}{G}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Bear"
    power = 3
    toughness = 3
    oracleText = "Whenever Hurska attacks, create a Food token. (It's an artifact with \"{2}, {T}, " +
        "Sacrifice this token: You gain 3 life.\")\n" +
        "Whenever you gain life, you may pay {G/W}. When you do, target creature gets +X/+X until " +
        "end of turn, where X is the amount of life you gained."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.CreateFood()
    }

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayMana("{G/W}"),
            descriptionOverride = "You may pay {G/W}. When you do, target creature gets +X/+X until " +
                "end of turn, where X is the amount of life you gained."
        ) {
            val creature = target(TargetFilter.Creature)
            val x = DynamicAmounts.triggerLifeGained()
            effect = Effects.ModifyStats(x, x, creature)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "53"
        artist = "GOSSAN"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/00841818-d46a-4865-a41c-daf627d10d1b.jpg?1783908854"
        ruling(
            "2024-11-08",
            "You don't choose a target for Hurska's last ability at the time it triggers. Rather, a second " +
                "\"reflexive\" ability triggers when you pay {G/W} this way. You choose a target for that " +
                "ability as it goes on the stack. Each player may respond to this triggered ability as normal.",
        )
        ruling("2024-11-08", "The value of X is calculated only once, as the reflexive triggered ability resolves.")
    }
}
