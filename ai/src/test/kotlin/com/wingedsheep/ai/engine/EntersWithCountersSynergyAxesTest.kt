package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.definitions.bfz.cards.TajuruStalwart
import com.wingedsheep.mtg.sets.definitions.bfz.cards.WoodlandWanderer
import com.wingedsheep.mtg.sets.definitions.conflux.cards.GluttonousSlime
import com.wingedsheep.mtg.sets.definitions.fem.cards.Homarid
import com.wingedsheep.mtg.sets.definitions.znr.cards.SpringmantleCleric
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * A creature that enters with +1/+1 counters through an as-enters replacement — Converge's "enters
 * with a +1/+1 counter on it for each color of mana spent to cast it" (Tajuru Stalwart, Woodland
 * Wanderer, Springmantle Cleric), Devour (Gluttonous Slime) — puts +1/+1 counters down as surely as
 * an "put a +1/+1 counter" effect does, so it feeds the +1/+1-counter axis and the deckbuilder sees
 * it beside its counter payoffs. Entering with some other counter (Homarid's tide counter) does not.
 */
class EntersWithCountersSynergyAxesTest : FunSpec({

    test("entering with +1/+1 counters feeds the +1/+1-counter axis") {
        for (card in listOf(TajuruStalwart, WoodlandWanderer, SpringmantleCleric, GluttonousSlime)) {
            withClue(card.name) { SynergyAxes.of(card).feeds shouldContain SynergyAxis.PLUS_COUNTERS }
        }
    }

    test("entering with another kind of counter feeds no +1/+1 counters") {
        SynergyAxes.of(Homarid).feeds shouldNotContain SynergyAxis.PLUS_COUNTERS
    }
})
