package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Meeting of the Five (SNC #202, {3}{W}{U}{B}{R}{G} sorcery).
 *
 *   Exile the top ten cards of your library. You may cast spells with exactly three colors from
 *   among them this turn. Add {W}{W}{U}{U}{B}{B}{R}{R}{G}{G}. Spend this mana only to cast spells
 *   with exactly three colors.
 *
 * Covers both halves of the colour count: the cast permission (`castColorCount`) and the mana
 * restriction (`ManaRestriction.SpellsWithExactColorCount`).
 */
class MeetingOfTheFiveScenarioTest : ScenarioTestBase() {

    init {
        context("the exiled cards") {

            test("only the three-colour spells among the ten may be cast, and the mana pays for them") {
                val game = resolvedMeeting()

                withClue("all ten cards were exiled") {
                    game.librarySize(1) shouldBe 0
                    listOf("Woolly Thoctar", "Lightning Helix", "Lightning Bolt", "Mountain")
                        .all { game.isInExile(1, it) } shouldBe true
                }
                withClue("a three-colour spell is offered; one- and two-colour spells and lands are not") {
                    castableFromExile(game, "Woolly Thoctar") shouldBe true
                    castableFromExile(game, "Lightning Helix") shouldBe false
                    castableFromExile(game, "Lightning Bolt") shouldBe false
                    playableFromExile(game, "Mountain") shouldBe false
                }

                withClue("the Thoctar is cast from exile with the restricted mana alone") {
                    game.castSpellFromExile(1, "Woolly Thoctar").error shouldBe null
                    game.resolveStack()
                    game.findPermanent("Woolly Thoctar") shouldNotBe null
                }
            }

            test("a hand-built cast of a two-colour exiled spell is rejected") {
                val game = resolvedMeeting()

                val helix = game.state.getExile(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Lightning Helix"
                }
                val result = game.execute(
                    CastSpell(game.player1Id, helix, listOf(ChosenTarget.Player(game.player2Id)))
                )
                withClue("Lightning Helix is two colours, so no permission covers it") {
                    result.error shouldBe "You may only cast spells with exactly 3 colors from there"
                }
            }
        }

        context("the restricted mana") {

            test("pays for a three-colour spell from hand but not for a one-colour one") {
                val game = resolvedMeeting(hand = listOf("Lightning Bolt", "Sprouting Thrinax"))

                withClue("every land was tapped for Meeting, so only the restricted mana is left") {
                    game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldNotBe null
                }
                withClue("Sprouting Thrinax ({B}{R}{G}) has exactly three colours") {
                    game.castSpell(1, "Sprouting Thrinax").error shouldBe null
                    game.resolveStack()
                    game.findPermanent("Sprouting Thrinax") shouldNotBe null
                }
            }
        }
    }

    /** Cast and resolve Meeting of the Five off exactly eight lands, over a known ten-card library. */
    private fun resolvedMeeting(hand: List<String> = emptyList()): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Meeting of the Five")
            .withLandsOnBattlefield(1, "Plains", 2)
            .withLandsOnBattlefield(1, "Island", 1)
            .withLandsOnBattlefield(1, "Swamp", 1)
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withLandsOnBattlefield(1, "Forest", 2)
            .withCardInLibrary(1, "Woolly Thoctar")
            .withCardInLibrary(1, "Lightning Helix")
            .withCardInLibrary(1, "Lightning Bolt")
            .withCardInLibrary(1, "Mountain")
        repeat(6) { builder.withCardInLibrary(1, "Grizzly Bears") }
        hand.forEach { builder.withCardInHand(1, it) }
        val game = builder
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        game.castSpell(1, "Meeting of the Five").error shouldBe null
        game.resolveStack()
        return game
    }

    private fun castableFromExile(game: TestGame, cardName: String): Boolean =
        game.getLegalActions(1).any {
            it.actionType == "CastSpell" && it.description.contains(cardName) && it.sourceZone == "EXILE"
        }

    private fun playableFromExile(game: TestGame, cardName: String): Boolean =
        game.getLegalActions(1).any {
            it.description.contains(cardName) && it.sourceZone == "EXILE"
        }
}
