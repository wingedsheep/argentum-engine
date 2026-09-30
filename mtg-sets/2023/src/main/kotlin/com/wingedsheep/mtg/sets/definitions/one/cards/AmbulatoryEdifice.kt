package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ambulatory Edifice
 * {2}{B}
 * Artifact Creature — Phyrexian Construct
 * 3/2
 *
 * When this creature enters, you may pay 2 life. When you do, target creature gets -1/-1
 * until end of turn.
 *
 * The "you may pay" is an [Effects.MayPay] gate (only offered when the 2 life is payable); the
 * payoff is a real reflexive triggered ability (CR 603.12) whose target is chosen as it goes on
 * the stack, per the 2023-02-04 ruling. Same shape as Zoraline, Cosmos Caller.
 */
val AmbulatoryEdifice = card("Ambulatory Edifice") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Phyrexian Construct"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, you may pay 2 life. When you do, target creature gets -1/-1 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.MayPay(
            cost = Effects.PayLife(2),
            then = Effects.ReflexiveTrigger(
                action = Effects.Nothing,
                optional = false,
                descriptionOverride = "target creature gets -1/-1 until end of turn"
            ) {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(-1, -1, creature)
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Ariel Perez"
        flavorText = "The Mirran forces thought they'd vanquished their foes . . . until a nearby monument got up and started stalking toward them."
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43c9005c-0574-4b75-8dbf-0a6641c3ae33.jpg?1783918052"

        ruling("2023-02-04", "You don't choose a target for Ambulatory Edifice's last ability at the time it triggers. Rather, a second \"reflexive\" ability triggers when you pay 2 life this way. You choose a target for that ability as it goes on the stack. Each player may respond to this triggered ability as normal.")
    }
}
