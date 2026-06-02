package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.CantBeRegeneratedEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.MarkExileOnDeathEffect
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Scorching Lava
 * {1}{R}
 * Instant
 * Kicker {R}
 * Scorching Lava deals 2 damage to any target. If this spell was kicked,
 * that creature can't be regenerated this turn and if it would die this
 * turn, exile it instead.
 */
val ScorchingLava = card("Scorching Lava") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Kicker {R} (You may pay an additional {R} as you cast this spell.)\n" +
        "Scorching Lava deals 2 damage to any target. If this spell was kicked, " +
        "that creature can't be regenerated this turn and if it would die this turn, exile it instead."

    keywordAbility(KeywordAbility.kicker("{R}"))

    spell {
        val t = target("target", AnyTarget())
        effect = DealDamageEffect(2, t) then ConditionalEffect(
            condition = WasKicked,
            effect = Effects.Composite(
                listOf(
                    CantBeRegeneratedEffect(t),
                    MarkExileOnDeathEffect(t),
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a85437f-052e-494c-a9ee-265c4624a409.jpg?1562903659"
    }
}
