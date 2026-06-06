package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Hydroid Krasis — Ravnica Allegiance #183
 * {X}{G}{U} · Creature — Jellyfish Hydra Beast · 0/0
 *
 * When you cast this spell, you gain half X life and draw half X cards. Round down each time.
 * Flying, trample
 * This creature enters with X +1/+1 counters on it.
 *
 * The cleanest "inherited X" card: the same `{X}` chosen as you cast feeds both the
 * "when you cast this spell" trigger (gain/draw half X) and the enters-with-counters
 * replacement (X +1/+1 counters). Both read [DynamicAmount.CastX], which resolves the
 * cast-time X off the spell's stable entity — from `SpellOnStackComponent` while the cast
 * trigger resolves on the stack, then from the durable `CastChoicesComponent` that rides the
 * same entity onto the battlefield. No counter laundering: the half-X draw cannot be fed by
 * +1/+1 counters, so it relies on `CastX` directly.
 *
 * "Half X, round down each time" is `Divide(CastX, Fixed(2), roundUp = false)`, evaluated
 * independently for the life gain and the draw (so X = 5 → gain 2, draw 2).
 */
val HydroidKrasis = card("Hydroid Krasis") {
    manaCost = "{X}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Jellyfish Hydra Beast"
    power = 0
    toughness = 0
    oracleText = "When you cast this spell, you gain half X life and draw half X cards. " +
        "Round down each time.\nFlying, trample\nThis creature enters with X +1/+1 counters on it."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    // "When you cast this spell, you gain half X life and draw half X cards. Round down each time."
    triggeredAbility {
        trigger = Triggers.WhenYouCastThisSpell()
        effect = Effects.Composite(
            Effects.GainLife(DynamicAmount.Divide(DynamicAmount.CastX, DynamicAmount.Fixed(2), roundUp = false)),
            Effects.DrawCards(DynamicAmount.Divide(DynamicAmount.CastX, DynamicAmount.Fixed(2), roundUp = false)),
        )
        description = "When you cast this spell, you gain half X life and draw half X cards. Round down each time."
    }

    // "This creature enters with X +1/+1 counters on it." — reads the SAME cast-time X.
    replacementEffect(EntersWithDynamicCounters(count = DynamicAmount.CastX))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "183"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/801dd9c6-b159-4e1c-af2c-214c1f573633.jpg?1584833616"
    }
}
