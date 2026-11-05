package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll

/**
 * Readings the Hour of Devastation eternalize / embalm identity gap surfaced:
 *
 *  - **Eternalize** is a graveyard keyword like embalm, so a card that has it is a graveyard
 *    payoff (Timeless Witness, Fanatic of Rhonas).
 *  - **Searching a card into your own graveyard** (Entomb, Vizier of the Anointed's enters trigger)
 *    stocks the graveyard the way a self-mill does, so it feeds the graveyard axis.
 *  - Vizier's "whenever you activate an eternalize or embalm ability, draw a card" is read as
 *    repeatable card draw.
 */
class EternalizeEmbalmReadingAiTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.distinctBy { it.name }.associateBy { it.name } }

    fun card(name: String) = byName[name] ?: error("'$name' is not in the corpus")

    test("an eternalize card rewards the graveyard axis") {
        SynergyAxes.of(card("Timeless Witness")).rewards shouldContain SynergyAxis.GRAVEYARD
        SynergyAxes.of(card("Fanatic of Rhonas")).rewards shouldContain SynergyAxis.GRAVEYARD
    }

    test("searching a card into your own graveyard feeds the graveyard axis") {
        withClue("Entomb bins the card it finds") {
            SynergyAxes.of(card("Entomb")).feeds shouldContain SynergyAxis.GRAVEYARD
        }
        withClue("Vizier of the Anointed bins an eternalize/embalm card, and rewards activating it") {
            val axes = SynergyAxes.of(card("Vizier of the Anointed"))
            axes.feeds shouldContain SynergyAxis.GRAVEYARD
            axes.rewards shouldContain SynergyAxis.GRAVEYARD
        }
    }

    test("Vizier of the Anointed is read fully: a tutor and card draw") {
        AiReadingAudit.audit(card("Vizier of the Anointed")).map { it.key }.shouldBeEmpty()
        CardIntentAnalyzer.analyze(card("Vizier of the Anointed")).tags shouldContainAll
            listOf(IntentTag.TUTOR, IntentTag.DRAW)
    }
})
