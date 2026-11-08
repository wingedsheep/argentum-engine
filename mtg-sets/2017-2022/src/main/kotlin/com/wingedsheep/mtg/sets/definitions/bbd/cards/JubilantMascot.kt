package com.wingedsheep.mtg.sets.definitions.bbd.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jubilant Mascot — Battlebond #28 (canonical printing)
 * {2}{W} · Creature — Homunculus · 1/1
 *
 * At the beginning of combat on your turn, you may pay {3}{W}. If you do, support 2. (Put a +1/+1
 * counter on each of up to two other target creatures.)
 *
 * "If you do" (not "when you do"): the support targets are chosen as the trigger goes on the stack
 * and the {3}{W} is offered only as it resolves (2018-06-08 ruling) — the ordinary targeted-trigger
 * path with an [Effects.MayPay] gate. Support is Relief Captain's shape at two targets.
 */
val JubilantMascot = card("Jubilant Mascot") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Homunculus"
    power = 1
    toughness = 1
    oracleText = "At the beginning of combat on your turn, you may pay {3}{W}. If you do, support 2. " +
        "(Put a +1/+1 counter on each of up to two other target creatures.)"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        targets(TargetFilter.OtherCreature, count = 2, optional = true)
        effect = Effects.MayPay(
            cost = ManaCost.parse("{3}{W}"),
            then = Effects.ForEachTarget(
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
            ),
        )
        description = "At the beginning of combat on your turn, you may pay {3}{W}. If you do, support 2 — " +
            "put a +1/+1 counter on each of up to two other target creatures."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "28"
        artist = "Filip Burburan"
        flavorText = "The competition is but one aspect of the spectacle."
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc7c73fd-e5f8-40e6-97ed-bfff0d6cf8d8.jpg?1783934871"
        ruling("2018-06-08", "You choose the target creatures to support when Jubilant Mascot's triggered ability is put onto the stack, but you don't choose whether to pay {3}{W} until the ability resolves.")
        ruling("2018-06-08", "You can't put more than one +1/+1 counter on any one target using the support action.")
        ruling("2018-06-08", "Support can target a creature another player controls.")
        ruling("2018-06-08", "If a spell with support has other abilities that target creatures, those abilities and the support ability can target the same creature.")
        ruling("2018-06-08", "If some, but not all, targets for a spell become illegal, the remaining targets are affected as appropriate. If all of a spell's targets become illegal, that spell doesn't resolve.")
    }
}
