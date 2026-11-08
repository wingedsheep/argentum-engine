package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Animation Module — Kaladesh #194
 * {1} · Artifact
 *
 * Whenever one or more +1/+1 counters are put on a permanent you control, you may pay {1}. If you
 * do, create a 1/1 colorless Servo artifact creature token.
 * {3}, {T}: Choose a counter on target permanent or player. Give that permanent or player another
 * counter of that kind.
 *
 * - "One or more" is per permanent: the non-batch `getsCounters` watcher merges one recipient's
 *   simultaneous placements into a single trigger, so counters landing on two permanents at once
 *   trigger it twice (ruling), and a permanent entering with +1/+1 counters counts too.
 * - The {1} is offered on resolution ([Effects.MayPay], Era of Innovation shape) — once per
 *   resolution, so it can't be paid twice for two Servos.
 * - The activated ability is [Effects.AddCountersOfChosenKind] over a permanent-or-player target:
 *   the kind is chosen on resolution among the kinds the recipient has (no prompt with one kind,
 *   no-op with none). It isn't proliferate, so "whenever you proliferate" doesn't fire.
 */
val AnimationModule = card("Animation Module") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever one or more +1/+1 counters are put on a permanent you control, you may pay {1}. " +
        "If you do, create a 1/1 colorless Servo artifact creature token.\n" +
        "{3}, {T}: Choose a counter on target permanent or player. Give that permanent or player another " +
        "counter of that kind."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.youControl()).getsCounters(CounterType.PLUS_ONE_PLUS_ONE)
        effect = Effects.MayPay(
            cost = Effects.PayMana("{1}"),
            then = Effects.CreateToken(
                power = 1,
                toughness = 1,
                creatureTypes = setOf("Servo"),
                artifactToken = true,
            ),
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        val recipient = target(Targets.PermanentOrPlayer)
        effect = Effects.AddCountersOfChosenKind(recipient)
        description = "{3}, {T}: Choose a counter on target permanent or player. Give that permanent or " +
            "player another counter of that kind."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "194"
        artist = "Aaron Miller"
        flavorText = "Design leads to progress."
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34bdc973-db45-46a6-ac48-ce88fb59920a.jpg?1783937163"
        ruling(
            "2016-09-20",
            "If +1/+1 counters are put on more than one permanent you control at the same time, Animation " +
                "Module's first ability triggers once for each of those permanents."
        )
        ruling(
            "2016-09-20",
            "Abilities that trigger when counters are put on a permanent trigger when a permanent enters the " +
                "battlefield with counters and when a player puts counters on a permanent."
        )
        ruling(
            "2016-09-20",
            "You can pay {1} only once each time Animation Module's first ability resolves. You can't pay more " +
                "to create extra Servo tokens."
        )
    }
}
