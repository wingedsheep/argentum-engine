package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.splice
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Through the Breach
 * {4}{R}
 * Instant — Arcane
 * You may put a creature card from your hand onto the battlefield. That creature gains haste.
 * Sacrifice that creature at the beginning of the next end step.
 * Splice onto Arcane {2}{R}{R}
 *
 * The haste grant is `Duration.Permanent`: the oracle text says only "gains haste", with no "until
 * end of turn", so the creature keeps haste for as long as it is around — which is until the delayed
 * sacrifice below, unless something saves it.
 *
 * The splice keyword needs no wiring beyond the one line: the spell effect declared here *is* the
 * rules text that gets spliced onto another Arcane spell (CR 702.47a).
 */
val ThroughTheBreach = card("Through the Breach") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Instant — Arcane"
    oracleText = "You may put a creature card from your hand onto the battlefield. That creature " +
        "gains haste. Sacrifice that creature at the beginning of the next end step.\n" +
        "Splice onto Arcane {2}{R}{R} (As you cast an Arcane spell, you may reveal this card from " +
        "your hand and pay its splice cost. If you do, add this card's effects to that spell.)"

    splice("{2}{R}{R}")

    spell {
        effect = Patterns.Hand.putFromHand(
            filter = GameObjectFilter.Creature
        ).then(
            ConditionalOnCollectionEffect(
                collection = "putting",
                ifNotEmpty = Effects.Composite(
                    Effects.GrantKeyword(
                        keyword = Keyword.HASTE,
                        target = EffectTarget.PipelineTarget("putting", 0),
                        duration = Duration.Permanent
                    ),
                    CreateDelayedTriggerEffect(
                        step = Step.END,
                        effect = Effects.SacrificeTarget(EffectTarget.PipelineTarget("putting", 0))
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "193"
        artist = "Hugh Jamieson"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6da09e6a-2965-4855-bd41-41b41ba188fb.jpg"
    }
}
