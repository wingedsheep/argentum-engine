package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry

/**
 * Felonious Rage — Murders at Karlov Manor #125
 * {R} · Instant
 *
 * Target creature you control gets +2/+0 and gains haste until end of turn. When that creature
 * dies this turn, create a 2/2 white and blue Detective creature token.
 *
 * The death clause is an entity-scoped delayed triggered ability (`Triggers.Dies` with
 * `watchedTarget` bound to the chosen creature), not a rider on the pump — so it still fires if
 * the +2/+0 has been overwritten, if the creature dies after combat, or if it dies to something
 * other than damage. `fireOnce` matches the printed "When", not "Whenever": one token even if the
 * same permanent somehow dies twice in the turn. `DelayedTriggerExpiry.EndOfTurn` is the "this
 * turn" scope — a creature that survives the turn and dies later produces nothing.
 *
 * The token's art comes from the MKM `tokenArt` layer, so no `imageUri` is baked in here.
 */
val FeloniousRage = card("Felonious Rage") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature you control gets +2/+0 and gains haste until end of turn. " +
        "When that creature dies this turn, create a 2/2 white and blue Detective creature token."

    spell {
        val t = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.Composite(
            Effects.ModifyStats(2, 0, t),
            Effects.GrantKeyword(Keyword.HASTE, t),
            CreateDelayedTriggerEffect(
                effect = Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.WHITE, Color.BLUE),
                    creatureTypes = setOf("Detective")
                ),
                trigger = Triggers.Dies,
                watchedTarget = t,
                fireOnce = true,
                expiry = DelayedTriggerExpiry.EndOfTurn
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "125"
        artist = "Justine Cruz"
        flavorText = "It's hard to keep the peace if you can't even control your temper."
        imageUri = "https://cards.scryfall.io/normal/front/4/5/4538d6a8-a24a-40e3-b894-45a30882c92a.jpg?1783912880"
    }
}
