package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.DeckSynergy
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.definitions.bfz.cards.BlightHerder
import com.wingedsheep.mtg.sets.definitions.bfz.cards.MindRaker
import com.wingedsheep.mtg.sets.definitions.bfz.cards.MurkStrider
import com.wingedsheep.mtg.sets.definitions.bfz.cards.RuinProcessor
import com.wingedsheep.mtg.sets.definitions.bfz.cards.UlamogsReclaimer
import com.wingedsheep.mtg.sets.definitions.bfz.cards.VileAggregate
import com.wingedsheep.mtg.sets.definitions.bfz.cards.WastelandStrangler
import com.wingedsheep.mtg.sets.definitions.ddp.cards.DominatorDrone
import com.wingedsheep.mtg.sets.definitions.lea.cards.GrizzlyBears
import com.wingedsheep.mtg.sets.definitions.mid.cards.AdelineResplendentCathar
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Three synergy readings Battle for Zendikar's Eldrazi needed:
 *
 *  - a processor *trigger* or spell effect ("you may put a card an opponent owns from exile into that
 *    player's graveyard. If you do, …") rewards opponents' exiled cards, as the processor cost does —
 *    and the card it moves stocks the opponent's graveyard, not ours;
 *  - a power defined by a count of your own permanents (Vile Aggregate's colorless creatures) asks
 *    for that kind of permanent;
 *  - a trigger's intervening "if you control another colorless creature" (Dominator Drone) asks
 *    for colorless creatures.
 */
class ProcessorTriggerSynergyAiTest : FunSpec({

    val processors = listOf(WastelandStrangler, MindRaker, MurkStrider, UlamogsReclaimer, RuinProcessor, BlightHerder)

    test("a processor trigger rewards opponents' exiled cards and doesn't feed our graveyard") {
        processors.forEach { card ->
            withClue(card.name) {
                SynergyAxes.of(card).rewards shouldContain SynergyAxis.OPPONENT_EXILE
                SynergyAxes.of(card).feeds shouldNotContain SynergyAxis.GRAVEYARD
            }
        }
        // An ingest creature stocks what the processor spends.
        SynergyAxes.of(WastelandStrangler).sharedWith(SynergyAxes.of(DominatorDrone)) shouldContain
            SynergyAxis.OPPONENT_EXILE
    }

    test("an each-opponent loop still feeds what it makes for the source's controller by name") {
        // Mind Raker's "each opponent discards" bins their cards; Adeline's "for each opponent, create
        // a 1/1 Human" under ControllerOfSource makes ours.
        SynergyAxes.of(AdelineResplendentCathar).feeds shouldContain SynergyAxis.TOKENS
    }

    test("Vile Aggregate's power asks for colorless creatures") {
        val aggregate = SynergyAxes.of(VileAggregate)
        aggregate.asks.any { SynergyAxes.of(DominatorDrone).answers(it) } shouldBe true
        aggregate.asks.any { SynergyAxes.of(GrizzlyBears).answers(it) } shouldBe false
    }

    test("Dominator Drone's intervening if asks for colorless creatures") {
        val drone = SynergyAxes.of(DominatorDrone)
        drone.asks.any { SynergyAxes.of(VileAggregate).answers(it) } shouldBe true
        drone.asks.any { SynergyAxes.of(GrizzlyBears).answers(it) } shouldBe false
    }

    test("a deck of colorless creatures keeps both payoffs fed; colored bears don't") {
        val payoffs = listOf(VileAggregate, DominatorDrone).map(SynergyAxes::of)
        fun fed(vararg others: com.wingedsheep.sdk.model.CardDefinition) =
            DeckSynergy.fedPayoffs(payoffs + others.map(SynergyAxes::of))
        fed(WastelandStrangler, MindRaker, MurkStrider) shouldBeGreaterThan fed(GrizzlyBears, GrizzlyBears, GrizzlyBears)
    }
})
