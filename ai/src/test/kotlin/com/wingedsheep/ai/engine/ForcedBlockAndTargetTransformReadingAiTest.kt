package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.definitions.emn.cards.ImpetuousDevils
import com.wingedsheep.mtg.sets.definitions.emn.cards.KessigProwler
import com.wingedsheep.mtg.sets.definitions.emn.cards.PropheticRavings
import com.wingedsheep.mtg.sets.definitions.emn.cards.WaxingMoon
import com.wingedsheep.mtg.sets.definitions.ktk.cards.AvalancheTusker
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "Target creature defending player controls blocks it this combat if able" (`ForceBlockEffect`)
 * is a lure aimed at the opponent's creature — harmful to it, a combat enhancement for us — and
 * "transform target Werewolf you control" (`TransformEffect` on a slot) is a gift to our own
 * double-faced permanent. Unread, the AI couldn't tell which side either slot belonged on, and
 * Impetuous Devils / Avalanche Tusker's attack trigger had no intent at all.
 *
 * Alongside: a team trample grant until end of turn is the untargeted team trick
 * (`expiringGroupPump`) a team pump is; "can't be blocked by more than one creature" is evasion;
 * an Aura's granted "{T}, Discard a card: Draw a card" is the host's own loot, read for intent and
 * for the graveyard it feeds.
 */
class ForcedBlockAndTargetTransformReadingAiTest : FunSpec({

    test("a forced block aims at the opponent's creature and reads as a combat enhancement") {
        for (card in listOf(ImpetuousDevils, AvalancheTusker)) {
            val trigger = card.script.triggeredAbilities.first()
            withClue(card.name) {
                TargetPolarityAnalyzer.forRequirements(
                    trigger.effect,
                    listOfNotNull(trigger.targetRequirement) + trigger.additionalTargetRequirements,
                ) shouldBe listOf(TargetPolarity.HARMFUL)
                CardIntentAnalyzer.analyze(card).tags shouldContain IntentTag.COMBAT_ENHANCEMENT
                AiReadingAudit.read(card).readFully shouldBe true
            }
        }
    }

    test("transforming a target permanent of ours is a gift, and the team trample is a held team trick") {
        TargetPolarityAnalyzer.forSpell(WaxingMoon) shouldBe listOf(TargetPolarity.BENEFICIAL)
        CardIntentAnalyzer.analyze(WaxingMoon).expiringGroupPump shouldBe true
        val reading = AiReadingAudit.read(WaxingMoon)
        withClue("$reading") { reading.readFully shouldBe true }
    }

    test("a back face that can't be blocked by more than one creature reads as evasion") {
        CardIntentAnalyzer.analyze(checkNotNull(KessigProwler.backFace)).tags shouldContain IntentTag.EVASION_GRANT
    }

    test("an Aura's granted loot reads as draw and feeds the graveyard") {
        CardIntentAnalyzer.analyze(PropheticRavings).tags shouldContain IntentTag.DRAW
        SynergyAxes.of(PropheticRavings).feeds shouldContain SynergyAxis.GRAVEYARD
    }
})
