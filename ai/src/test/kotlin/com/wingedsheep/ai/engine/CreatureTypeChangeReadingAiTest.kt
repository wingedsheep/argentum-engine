package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

/**
 * Creature-type changes: "target creature loses all creature types" is a curse on its holder (its
 * lords and typal payoffs stop seeing it), and "target creature gains all creature types" (Oko,
 * Lorwyn Liege's +2, granted as Changeling) is a gift whose payoff is the typal board the
 * simulation prices — so it needs no intent tag of its own.
 *
 * The decision it changes: Amoeboid Changeling's "loses all creature types" activation is aimed at
 * an opponent's creature, where before its target slot had no side.
 */
class CreatureTypeChangeReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.requireCard(name)

    init {
        test("losing all creature types is aimed at an opponent's creature") {
            val amoeboid = card("Amoeboid Changeling")
            val loses = amoeboid.script.activatedAbilities[1]
            withClue("the creature that loses its types loses its lords and typal payoffs") {
                TargetPolarityAnalyzer.forAbility(amoeboid, loses) shouldBe listOf(TargetPolarity.HARMFUL)
            }
        }

        test("Nameless Inversion's type loss is read; its +3/-3 stays the board's call") {
            val gaps = AiReadingAudit.read(card("Nameless Inversion")).abilities
                .flatMap { it.findings }.filterNot { it.read }.toString()
            gaps shouldNotContain "LoseAllCreatureTypesEffect"
        }

        test("Oko, Lorwyn Liege's changeling grant needs no intent and the card reads fully") {
            val reading = AiReadingAudit.read(card("Oko, Lorwyn Liege"))
            withClue(reading.toString()) { reading.readFully shouldBe true }
        }
    }
}
