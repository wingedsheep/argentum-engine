package com.wingedsheep.ai.audit

import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "When this creature enters, an opponent gains control of it" spelled as choose-then-give (Akroan
 * Horse): `ChooseOpponent` only records which opponent the `GiveControl` after it reads back through
 * `Player.ChosenOpponent`. A setup step with no payoff of its own, it needs no intent — what the
 * ability is for is the step that reads the choice.
 */
class ChooseOpponentSetupReadingTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name } }
    fun named(name: String) = byName[name] ?: error("'$name' is not in the corpus")

    test("choosing the opponent that the next step hands the permanent to needs no intent") {
        val reading = AiReadingAudit.read(named("Akroan Horse"))
        withClue(reading.toString()) { reading.readFully shouldBe true }
    }
})
