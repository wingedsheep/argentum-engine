package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.dsl.Targets

/**
 * Probe
 * {2}{U}
 * Sorcery
 * Kicker {1}{B}
 * Draw three cards, then discard two cards. If this spell was kicked, target player
 * discards two cards.
 *
 * CR 702.33g: the target in the kicked part of the spell is chosen only if the spell was
 * kicked; otherwise the spell is cast as though it had no target. That is the kicker branch —
 * `kickerTarget` / `kickerEffect`, which replaces the unkicked effect wholesale — so the kicked
 * effect restates the draw-and-discard before the target's discard. An optional target under
 * `Effects.If(WasKicked)` would let a kicked Probe be cast with no target at all.
 */
val Probe = card("Probe") {
    manaCost = "{2}{U}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Kicker {1}{B} (You may pay an additional {1}{B} as you cast this spell.)\n" +
        "Draw three cards, then discard two cards. If this spell was kicked, target player discards two cards."

    keywordAbility(KeywordAbility.kicker("{1}{B}"))

    spell {
        effect = Effects.DrawCards(3) then Effects.Discard(2)

        val targetPlayer = kickerTarget(Targets.Player)
        kickerEffect = Effects.DrawCards(3) then
            Effects.Discard(2) then
            Effects.Discard(2, targetPlayer)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Eric Peterson"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2a58d18-3d52-4178-86b2-7590d4164e76.jpg?1562927868"
    }
}
