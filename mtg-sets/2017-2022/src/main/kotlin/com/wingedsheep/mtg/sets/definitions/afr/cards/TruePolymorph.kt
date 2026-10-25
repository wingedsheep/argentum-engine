package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * True Polymorph — Adventures in the Forgotten Realms #80
 * {4}{U}{U} · Instant
 *
 * Target artifact or creature becomes a copy of another target artifact or creature.
 *
 * Fleeting Reflection's single-permanent copy shape, made permanent: index 0 (`affected`) becomes a
 * copy of index 1 (`target`, wrapped in [TargetOther] for "another") with [Duration.Permanent], so
 * the copy is baked into base state. Copiable values only (CR 707.2) — counters, tapped state and
 * attachments stay put. If either target is illegal on resolution the copy step no-ops.
 */
val TruePolymorph = card("True Polymorph") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target artifact or creature becomes a copy of another target artifact or creature."

    spell {
        val affected = target(TargetFilter.CreatureOrArtifact)
        val copySource = target(TargetOther(
            baseRequirement = TargetObject(filter = TargetFilter.CreatureOrArtifact)
        ))
        effect = Effects.EachPermanentBecomesCopyOfTarget(
            target = copySource,
            duration = Duration.Permanent,
            affected = affected,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "80"
        artist = "Steve Prescott"
        flavorText = "Maybe a little *too* convincing."
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04a1f384-9266-425c-b739-09d74871fc7b.jpg?1783926505"
    }
}
