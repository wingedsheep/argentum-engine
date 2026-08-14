package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Wolf Strike (Innistrad: Crimson Vow) — {2}{G} Instant
 *
 * "Target creature you control gets +2/+0 until end of turn if it's night. Then it deals damage equal to
 *  its power to target creature you don't control."
 *
 * A conditional fight-style spell built on the Clear Shot rail: pump the first target, then have it deal
 * damage equal to its (already-modified) power to the second target. The pump is gated on
 * [Conditions.IsNight] (CR 731) — true only while it is night, false during day *and* when neither
 * designation exists — wrapping the [Effects.ModifyStats] in a [ConditionalEffect]. Because the composite
 * resolves in order, the [DealDamageEffect]'s `DynamicAmounts.targetPower(0)` reads the buffed power when
 * it was night and the base power otherwise, matching "+2/+0 … if it's night. **Then** it deals damage
 * equal to its power".
 *
 * The damage's `damageSource = t1` attributes it to the pumped creature (it "deals damage"), so lifelink,
 * Howlpack Avenger-style riders, and "dealt damage by a creature" triggers see the right source.
 */
val WolfStrike: CardDefinition = card("Wolf Strike") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature you control gets +2/+0 until end of turn if it's night. Then it deals " +
        "damage equal to its power to target creature you don't control."

    spell {
        val t1 = target("your creature", TargetCreature(filter = TargetFilter.Creature.youControl()))
        val t2 = target("their creature", TargetCreature(filter = TargetFilter.Creature.opponentControls()))
        effect = Effects.Composite(
            ConditionalEffect(
                condition = Conditions.IsNight,
                effect = Effects.ModifyStats(2, 0, t1),
            ),
            DealDamageEffect(DynamicAmounts.targetPower(0), t2, damageSource = t1),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "228"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02e9cd00-7ffd-44e1-aa0f-c94489ff4a0f.jpg?1783924798"
    }
}
