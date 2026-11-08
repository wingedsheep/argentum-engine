package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiCardRating
import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.definitions.kld.cards.AnimationModule
import com.wingedsheep.mtg.sets.definitions.kld.cards.DeadlockTrap
import com.wingedsheep.mtg.sets.definitions.kld.cards.FleetwheelCruiser
import com.wingedsheep.mtg.sets.definitions.kld.cards.SequesteredStash
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Three effect shapes the Kaladesh artifacts brought the readers:
 *
 *  - `AddCountersOfChosenKindEffect` over a target ("choose a counter on target permanent or player;
 *    give it another of that kind", Animation Module) — proliferate's one-holder form: read either
 *    way, since its controller picks the kind among the ones already there, and needing no tag;
 *  - `AddCardTypeEffect` adding creature to the source until end of turn (Fleetwheel Cruiser's
 *    enters trigger) — the crew-shaped self-animation, needing no tag like `BecomeCreature` on Self;
 *  - a pipeline that picks a card from our graveyard and puts it on our library's top (Sequestered
 *    Stash) — a regrowth paid for with the next draw.
 */
class KaladeshReadingGapsAiTest : FunSpec({

    test("a counter of a chosen kind on a target is read either way") {
        val ability = AnimationModule.script.activatedAbilities.single()
        TargetPolarityAnalyzer.forAbility(AnimationModule, ability) shouldBe listOf(TargetPolarity.FLEXIBLE)
    }

    test("a graveyard card put on our library's top reads as recursion") {
        CardIntentAnalyzer.analyze(SequesteredStash).tags shouldContain IntentTag.RECURSION
    }

    test("each card reads fully and is recognised") {
        listOf(AnimationModule, DeadlockTrap, FleetwheelCruiser, SequesteredStash).forEach { card ->
            val reading = AiReadingAudit.read(card)
            withClue("$reading") { reading.readFully shouldBe true }
            withClue("${card.name} is not flagged 'abilities not recognised'") {
                AiCardRating.of(card, listOf("KLD")).intent.unrecognised shouldBe false
            }
        }
    }
})
