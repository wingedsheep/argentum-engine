package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Fleeting Reflection {1}{U}
 * Instant
 *
 * Target creature you control gains hexproof until end of turn. Untap that creature.
 * Until end of turn, it becomes a copy of up to one other target creature.
 *
 * Two target requirements: a mandatory "target creature you control" (index 0) and an optional
 * "up to one other target creature" (index 1), wrapped in [TargetOther] so it must differ from the
 * first. The composite applies, in order, over index 0: [Effects.GrantHexproof] (until end of turn)
 * and [Effects.Untap]; then [Effects.EachPermanentBecomesCopyOfTarget] makes index 0 (`affected`)
 * become a copy of index 1 (`target`) until end of turn (reverted by cleanup).
 *
 * Edge cases (per the OTJ rulings, 2024-04-12):
 *  - The first target may already be untapped — it still gains hexproof and copies.
 *  - The second target is optional; when omitted, the copy effect's [target] resolves to nothing
 *    and is a no-op, so the creature only gains hexproof and untaps.
 *  - If the second target becomes illegal before resolution, the first still gains hexproof/untaps;
 *    the copy step then no-ops (its copy source is gone).
 *  - Becoming a copy copies copiable values only (Rule 707); counters, tapped state, attachments
 *    and non-copy effects are unaffected, and no enters/leaves abilities trigger.
 */
val FleetingReflection = card("Fleeting Reflection") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target creature you control gains hexproof until end of turn. Untap that " +
        "creature. Until end of turn, it becomes a copy of up to one other target creature."

    spell {
        target(
            "creature you control",
            TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.youControl()))
        )
        target(
            "other creature",
            TargetOther(
                baseRequirement = TargetObject(
                    optional = true,
                    filter = TargetFilter(GameObjectFilter.Creature)
                )
            )
        )
        effect = Effects.Composite(
            listOf(
                Effects.GrantHexproof(EffectTarget.ContextTarget(0), Duration.EndOfTurn),
                Effects.Untap(EffectTarget.ContextTarget(0)),
                Effects.EachPermanentBecomesCopyOfTarget(
                    target = EffectTarget.ContextTarget(1),
                    duration = Duration.EndOfTurn,
                    affected = EffectTarget.ContextTarget(0),
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Camille Alquier"
        flavorText = "\"You're not mad I pulled a little prank. You're just mad you fell for it.\"\n—Oko, to Annie Flash"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f8c931e-219f-4032-b55b-b5975fbea1e7.jpg?1712355426"
    }
}
