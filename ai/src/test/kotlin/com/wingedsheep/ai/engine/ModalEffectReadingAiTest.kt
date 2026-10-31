package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The intent reader on "Choose one —" spells ([com.wingedsheep.sdk.scripting.effects.ModalEffect]).
 *
 * Reality Fracture is built on charms, and the reader saw none of them: a modal spell was one opaque
 * leaf, so Fatehold Charm, Theorix Charm and Stingerquill Charm read as doing nothing at all. Two
 * readings now, for two questions:
 *
 *  - **In hand** the card is worth what any mode does — every mode's tags (a charm with a burn mode
 *    is removal), except that "counterspell", which also says "dead with nothing on the stack",
 *    needs every mode to counter.
 *  - **As a cast** it does what the chosen modes do — Theorix Charm's counter mode is a
 *    counterspell with nothing to counter on an empty stack, and Fatehold Charm's team pump is a
 *    combat trick with no fight to spend it in at the opponent's end step.
 */
class ModalEffectReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun TestGame.choice(): GameAction = AIPlayer.create(cardRegistry, player1Id, AiProfile.LIVE).chooseAction(state)

    private fun TestGame.inHand(name: String) = state.getZone(player1Id, Zone.HAND).first {
        state.getEntity(it)?.get<CardComponent>()?.name == name
    }

    private fun live() = HoldPolicy(intents, holdUnusablePumps = true)

    init {
        test("a charm in hand carries every mode's tags, and counterspell only when every mode counters") {
            val stingerquill = intents.forName("Stingerquill Charm")!!.tags
            withClue("Stingerquill Charm: 3 damage to any target / first strike + deathtouch / a 2/2 token") {
                stingerquill shouldContain IntentTag.REMOVAL
                stingerquill shouldContain IntentTag.TOKEN_MAKER
            }
            val theorix = intents.forName("Theorix Charm")!!.tags
            withClue("Theorix Charm: counter / -2/-2 / mill then draw — not dead on an empty stack") {
                theorix shouldContain IntentTag.REMOVAL
                theorix shouldContain IntentTag.DRAW
                theorix shouldNotContain IntentTag.COUNTERSPELL
            }
        }

        test("Theorix Charm's counter mode has no window on an empty stack; its -2/-2 mode does") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(6)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardInHand(1, "Theorix Charm")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val charm = game.inHand("Theorix Charm")

            val counter = live().verdictFor(
                game.state, game.player1Id, "Theorix Charm", cast = CastSpell(game.player1Id, charm, chosenModes = listOf(0)),
            )
            withClue("a cast of the counter mode reads as a counterspell") { counter shouldBe TimingVerdict.NoWindow }

            val shrink = live().verdictFor(
                game.state, game.player1Id, "Theorix Charm",
                cast = CastSpell(
                    game.player1Id, charm,
                    targets = listOf(ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!)),
                    chosenModes = listOf(1),
                ),
            )
            withClue("the -2/-2 mode is removal, not a counterspell") { shrink shouldNotBe TimingVerdict.NoWindow }
        }

        // instants-06's shape with a charm: the +1/+2 wears off in cleanup, so spending the card on
        // it at the opponent's end step buys nothing — the draw mode is the card's end-step play.
        test("does not spend Fatehold Charm's team pump at the opponent's end step") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(6)
                .withActivePlayer(2)
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInHand(1, "Fatehold Charm")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Savannah Lions")
                .build()
                .advanceToPriority(1, Step.END)
            val charm = game.inHand("Fatehold Charm")

            val pump = live().verdictFor(
                game.state, game.player1Id, "Fatehold Charm", cast = CastSpell(game.player1Id, charm, chosenModes = listOf(2)),
            )
            withClue("the chosen team pump is an expiring group pump with no fight left") {
                pump shouldBe TimingVerdict.NoWindow
            }

            val action = game.choice()
            withClue("chose $action") {
                ((action as? CastSpell)?.takeIf { it.cardId == charm }?.chosenModes ?: emptyList()) shouldNotContain 2
            }
        }
    }
}
