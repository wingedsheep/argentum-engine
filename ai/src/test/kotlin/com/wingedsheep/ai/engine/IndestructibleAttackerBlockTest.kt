package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.seatId
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * An indestructible attacker can't be killed by combat damage, so a block that loses creatures
 * "to kill it" loses them for nothing. The AI takes the hit, blocks with a creature that survives,
 * or chumps one body when the life is needed — it never gang-blocks or trades into it.
 *
 * The 2026-11 report: the AI blocked indestructible attackers with creatures that died, as if the
 * block would kill them.
 */
class IndestructibleAttackerBlockTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            listOf(
                CardDefinition.creature(
                    name = WURM, manaCost = ManaCost.parse("{4}{G}"),
                    subtypes = setOf(Subtype("Wurm")), power = 5, toughness = 4,
                    keywords = setOf(Keyword.INDESTRUCTIBLE),
                ),
                CardDefinition.creature(
                    name = COLOSSUS, manaCost = ManaCost.parse("{7}"),
                    subtypes = setOf(Subtype("Golem")), power = 7, toughness = 7,
                    keywords = setOf(Keyword.INDESTRUCTIBLE),
                ),
                CardDefinition.creature(
                    name = BEAR, manaCost = ManaCost.parse("{3}"),
                    subtypes = setOf(Subtype("Bear")), power = 2, toughness = 2,
                    keywords = setOf(Keyword.INDESTRUCTIBLE),
                ),
            ),
        )
    }

    /**
     * Seat 1 attacks with [attacker]; seat 2 (the AI) holds [ours]. Against a plain Spined Wurm
     * this board gang-blocks — two 2-power creatures kill the 5/4 and lose one of themselves.
     */
    private fun board(attacker: String, ours: List<String>, ourLife: Int = 20): TestGame {
        var b = scenario().withRngSeed(20261109L).withPlayers()
            .withLifeTotal(2, ourLife)
            .withCardOnBattlefield(1, attacker)
        ours.forEach { b = b.withCardOnBattlefield(2, it) }
        repeat(10) { b = b.withCardInLibrary(1, "Craw Wurm").withCardInLibrary(2, "Craw Wurm") }
        val game = b.build().advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
        val a = game.findPermanent(attacker)!!
        game.execute(DeclareAttackers(game.seatId(1), mapOf(a to game.seatId(2))))
        return game.advanceToDeclaration(2, Step.DECLARE_BLOCKERS)
    }

    private fun TestGame.nameOf(id: EntityId): String? = state.getEntity(id)?.get<CardComponent>()?.name

    private fun TestGame.blockersOn(attacker: String, profile: AiProfile): List<String?> {
        val a = findPermanent(attacker)!!
        val blocks = AIPlayer.create(cardRegistry, seatId(2), profile).chooseAction(state) as DeclareBlockers
        return blocks.blockers.filterValues { a in it }.keys.map { nameOf(it) }
    }

    private companion object {
        const val WURM = "Test Darksteel Wurm"
        const val BEAR = "Test Darksteel Bear"
        const val COLOSSUS = "Test Darksteel Golem"
        val GANG = listOf("Savannah Lions", "Goblin Piker", "Giant Spider", "Pensive Minotaur")
        // LIVE is what players face; without the trade fixes it reads single blocks the legacy way.
        val PROFILES = listOf(AiProfile.LIVE, AiProfile.LIVE_WITHOUT_TRADE_FIXES)
    }

    init {
        test("control: a plain Spined Wurm is worth ganging up on") {
            val blockers = board("Spined Wurm", GANG).blockersOn("Spined Wurm", AiProfile.LIVE)
            withClue("blocked with $blockers") { (blockers.size >= 2) shouldBe true }
        }

        for (profile in PROFILES) {
            test("${profile.id}: an indestructible 5/4 is not gang-blocked — take the five") {
                val blockers = board(WURM, GANG).blockersOn(WURM, profile)
                withClue("blocked with $blockers") { blockers.shouldBeEmpty() }
            }

            test("${profile.id}: an indestructible 7/7 is not triple-blocked by three 3/3s") {
                val blockers = board(COLOSSUS, List(3) { "Hill Giant" }).blockersOn(COLOSSUS, profile)
                withClue("blocked with $blockers") { blockers.shouldBeEmpty() }
            }

            test("${profile.id}: an indestructible 2/2 is not traded with") {
                val blockers = board(BEAR, listOf("Grizzly Bears")).blockersOn(BEAR, profile)
                withClue("blocked with $blockers") { blockers.shouldBeEmpty() }
            }

            test("${profile.id}: facing lethal from an indestructible 5/4, one creature chumps it") {
                val blockers = board(WURM, GANG, ourLife = 5).blockersOn(WURM, profile)
                withClue("blocked with $blockers") { blockers.size shouldBe 1 }
            }
        }
    }
}
