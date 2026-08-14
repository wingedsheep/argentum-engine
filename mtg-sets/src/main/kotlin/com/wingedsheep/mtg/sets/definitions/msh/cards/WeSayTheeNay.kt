package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.teamwork
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * We Say Thee Nay! — Marvel Super Heroes #82
 * {1}{U} · Instant — Arcane
 *
 * Teamwork 2 (As an additional cost to cast this spell, you may tap any number of creatures you
 * control with total power 2 or more.)
 * Counter target spell unless its controller pays {2}. Counter that spell unless its controller
 * pays {4} instead if this spell was cast using teamwork.
 *
 * The plain spell-rider "instead" shape of teamwork (CR 702.194b), gated on
 * [Conditions.TeamworkWasPaid] — the declaration is read off this spell while it is still on the
 * stack, at the moment the counter resolves. One [Effects.CounterUnlessDynamicPays] with a
 * [DynamicAmount.Conditional] tax rather than two counter effects: the printed card is a *single*
 * "counter unless they pay" event whose price changes, and both prices are purely generic, so a
 * dynamic generic amount is exact.
 */
val WeSayTheeNay = card("We Say Thee Nay!") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant — Arcane"
    oracleText = "Teamwork 2 (As an additional cost to cast this spell, you may tap any number of " +
        "creatures you control with total power 2 or more.)\n" +
        "Counter target spell unless its controller pays {2}. Counter that spell unless its " +
        "controller pays {4} instead if this spell was cast using teamwork."

    teamwork(2)

    spell {
        target = Targets.Spell
        effect = Effects.CounterUnlessDynamicPays(
            DynamicAmount.Conditional(
                condition = Conditions.TeamworkWasPaid,
                ifTrue = DynamicAmount.Fixed(4),
                ifFalse = DynamicAmount.Fixed(2),
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Mateus Manhanini"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13b70321-75bd-4d44-b9f6-5f062a5dda0f.jpg?1783902948"
    }
}
