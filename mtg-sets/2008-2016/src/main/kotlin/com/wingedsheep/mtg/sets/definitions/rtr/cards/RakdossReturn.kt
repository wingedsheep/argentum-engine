package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget


/**
 * Rakdos's Return — Return to Ravnica #188 (canonical printing)
 * {X}{B}{R} · Sorcery
 *
 * Rakdos's Return deals X damage to target opponent or planeswalker. That player or that
 * planeswalker's controller discards X cards.
 *
 * The discard follows the target's kind: a targeted opponent discards directly
 * ([EffectTarget.ContextTarget]); a targeted planeswalker's controller discards through
 * [EffectTarget.TargetController] (a player entity has no controller, so that read alone would
 * skip the player case). Either way the discarding player picks the cards from their own hand.
 */
val RakdossReturn = card("Rakdos's Return") {
    manaCost = "{X}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "Rakdos's Return deals X damage to target opponent or planeswalker. " +
        "That player or that planeswalker's controller discards X cards."

    spell {
        val t = target(Targets.OpponentOrPlaneswalker)
        effect = Effects.DealDamage(DynamicAmounts.xValue(), t) then
            Effects.If(
                condition = Conditions.TargetIsPlayer(0),
                then = Effects.Discard(DynamicAmounts.xValue(), t),
                otherwise = Effects.Discard(DynamicAmounts.xValue(), EffectTarget.TargetController)
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "188"
        artist = "Daarken"
        flavorText = "When Lord Rakdos stirred from his slumber, everyone else's nightmare began."
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d72981c0-1632-4d64-9341-2a76047d9b36.jpg?1783940334"
    }
}
