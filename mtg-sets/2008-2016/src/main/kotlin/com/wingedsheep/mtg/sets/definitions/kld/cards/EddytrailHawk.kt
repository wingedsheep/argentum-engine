package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Eddytrail Hawk — Kaladesh #12
 * {1}{W} · Creature — Bird · 1/2
 *
 * Flying
 * When this creature enters, you get {E}{E} (two energy counters).
 * Whenever this creature attacks, you may pay {E}. If you do, another target attacking creature
 * gains flying until end of turn.
 *
 * The target is chosen when the attack trigger goes on the stack; the energy is offered on
 * resolution ([Effects.MayPay], Hexgold Slith shape). "Another" excludes the Hawk via
 * [TargetFilter.other] (Phase Dolphin shape).
 */
val EddytrailHawk = card("Eddytrail Hawk") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird"
    power = 1
    toughness = 2
    oracleText = "Flying\n" +
        "When this creature enters, you get {E}{E} (two energy counters).\n" +
        "Whenever this creature attacks, you may pay {E}. If you do, another target attacking creature " +
        "gains flying until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val t = target(TargetFilter.AttackingCreature.other())
        effect = Effects.MayPay(
            cost = Effects.PayExactCounters(CounterType.ENERGY, 1),
            then = Effects.GrantKeyword(Keyword.FLYING, t),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "12"
        artist = "James Paick"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81dcaec6-4cb9-46d8-aed1-46f33c67dff2.jpg?1783937235"
        ruling(
            "2024-06-07",
            "Some triggered abilities that state that you \"may pay\" a certain amount of {E} describe an effect " +
                "that happens \"If you do.\" In that case, no player may take actions to try to stop the ability's " +
                "effect after you make your choice."
        )
        ruling(
            "2024-06-07",
            "If a spell or ability with one or more targets states that you \"may pay\" some amount of {E}, and " +
                "each permanent that it targets has become an illegal target, the spell or ability won't resolve. " +
                "You can't pay any {E} even if you want to."
        )
    }
}
