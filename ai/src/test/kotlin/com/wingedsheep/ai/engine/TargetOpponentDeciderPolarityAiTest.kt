package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.definitions.chk.cards.GiftsUngiven
import com.wingedsheep.mtg.sets.definitions.emn.cards.FortunesFavor
import com.wingedsheep.mtg.sets.definitions.ltr.cards.SauronsRansom
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Target opponent looks at the top four cards of your library and separates them into a face-down
 * pile and a face-up pile" (Fortune's Favor, Sauron's Ransom), "target opponent chooses two of
 * those cards" (Gifts Ungiven): the opponent slot only names who decides a split of the caster's
 * cards. The decider works against the caster, so the slot is aimed at the opponent's side.
 *
 * Unread, each slot was UNKNOWN and fell back to the removal ranking.
 */
class TargetOpponentDeciderPolarityAiTest : FunSpec({

    test("a target opponent who splits our cards is aimed at the opponent's side") {
        for (card in listOf(FortunesFavor, SauronsRansom, GiftsUngiven)) {
            withClue(card.name) { TargetPolarityAnalyzer.forSpell(card) shouldBe listOf(TargetPolarity.HARMFUL) }
        }
    }

    test("Fortune's Favor reads fully") {
        val reading = AiReadingAudit.read(FortunesFavor)
        withClue("$reading") { reading.readFully shouldBe true }
    }
})
