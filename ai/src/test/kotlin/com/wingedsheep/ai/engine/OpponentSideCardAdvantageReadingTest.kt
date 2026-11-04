package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.definitions.rix.cards.DeadMansChest
import com.wingedsheep.mtg.sets.definitions.rix.cards.ExpelFromOrazca
import com.wingedsheep.mtg.sets.definitions.rix.cards.VraskasScorn
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Readings of cards that win cards or tempo off the opponent's side of the table:
 *
 * - an Aura only an opponent's creature can wear, whose whole card is a payoff for its controller,
 *   is aimed at the opponent's creatures (the removal ranking), not unread;
 * - a lasting "you may cast spells from among those cards" off an **opponent's** library top is card
 *   draw, as an impulse off our own is;
 * - a search that reaches the library through several zones ("your library and/or graveyard") is a tutor;
 * - a permanent put from the battlefield onto its owner's library is removal, a worse bounce.
 */
class OpponentSideCardAdvantageReadingTest : FunSpec({
    fun unread(card: com.wingedsheep.sdk.model.CardDefinition) =
        AiReadingAudit.read(card).abilities.flatMap { it.findings }.filter { !it.read }

    test("an opponent-only Aura with nothing on its host is aimed at the opponent's creatures") {
        TargetPolarityAnalyzer.forSpell(DeadMansChest) shouldBe listOf(TargetPolarity.HARMFUL)
    }

    test("a lasting impulse grant off an opponent's library top reads as card draw") {
        val trigger = DeadMansChest.script.triggeredAbilities.single()
        CardIntentAnalyzer.analyzeEffect(trigger.effect).tags shouldContain IntentTag.DRAW
        unread(DeadMansChest) shouldBe emptyList()
        // The host dies once and the Aura goes with it: a one-shot, not an engine.
        CardIntentAnalyzer.analyze(DeadMansChest).repeatable shouldBe false
    }

    test("a search of library and graveyard is a tutor") {
        CardIntentAnalyzer.analyze(VraskasScorn).tags shouldContain IntentTag.TUTOR
        unread(VraskasScorn) shouldBe emptyList()
        // Its graveyard is only where the named card may be found, not a graveyard payoff.
        val profile = SynergyAxes.of(VraskasScorn)
        profile.feeds shouldBe setOf(SynergyAxis.SPELLS)
        profile.rewards shouldBe emptySet()
    }

    test("a bounce that may instead tuck onto the library is read as either-way, not unread") {
        // The tuck alone is harmful; beside the to-hand bounce (rescuing our own creature is a real
        // play) the slot reads two-sided, so both sides are compared by simulation.
        TargetPolarityAnalyzer.forSpell(ExpelFromOrazca) shouldBe listOf(TargetPolarity.FLEXIBLE)
        unread(ExpelFromOrazca) shouldBe emptyList()
    }
})
