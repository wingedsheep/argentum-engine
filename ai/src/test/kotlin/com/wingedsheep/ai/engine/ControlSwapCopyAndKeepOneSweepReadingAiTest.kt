package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * Five effect shapes the readers used to leave unread, each read the way its single-target sibling is:
 *
 * - **Exchange of control** ([com.wingedsheep.sdk.scripting.effects.ExchangeControlEffect], Shrewd
 *   Negotiation): the slot only an opponent's permanent fills is a steal ([TargetPolarity.HARMFUL]),
 *   the permanent we hand over its price ([TargetPolarity.FLEXIBLE], so no veto on a slot only ours
 *   fill), and the card is [IntentTag.REMOVAL] as a plain steal is.
 * - **Keep one of each, sacrifice the rest** (Cataclysmic Gearhulk, Mythos of Snapdax, Single
 *   Combat): each player's permanents are gathered inside a collecting loop over players and the
 *   unchosen rest sacrificed after it — a symmetric [IntentTag.SWEEPER] the deckbuilder sees
 *   hitting its own creatures.
 * - **All-targets redirect** (Insidious Will's second mode): the unrestricted redirect, two-sided.
 * - **Token copy of any side's permanent** (Saheeli's Artistry): the copy is ours, the best original
 *   may be theirs — two-sided.
 * - **Pipeline bounce of the chosen targets** (Paradoxical Outcome): the single-target bounce, two-sided.
 *
 * The decision it changes: Shrewd Negotiation is cast as the removal it is — the opponent's best
 * creature taken for our least valuable artifact — and the sweeper Gearhulks count against a
 * creature deck's build.
 */
class ControlSwapCopyAndKeepOneSweepReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun tags(card: String) = CardIntentAnalyzer.analyze(cardRegistry.requireCard(card)).tags

    init {
        test("the five Kaladesh cards read fully") {
            for (card in listOf(
                "Cataclysmic Gearhulk", "Insidious Will", "Paradoxical Outcome", "Saheeli's Artistry",
                "Shrewd Negotiation", "Mythos of Snapdax",
            )) {
                val reading = AiReadingAudit.read(cardRegistry.requireCard(card))
                withClue("$card: $reading") { reading.readFully shouldBe true }
            }
        }

        test("an exchange of control steals their permanent and pays with ours") {
            intents.spellTargetPolarities("Shrewd Negotiation") shouldBe
                listOf(TargetPolarity.FLEXIBLE, TargetPolarity.HARMFUL)
            tags("Shrewd Negotiation") shouldContain IntentTag.REMOVAL
        }

        test("keep one of each, sacrifice the rest, is a sweeper that hits our own side too") {
            for (card in listOf("Cataclysmic Gearhulk", "Mythos of Snapdax", "Single Combat")) {
                withClue(card) {
                    tags(card) shouldContainAll listOf(IntentTag.REMOVAL, IntentTag.SWEEPER)
                    CardIntentAnalyzer.sweepsOwnSide(cardRegistry.requireCard(card)) shouldBe true
                }
            }
        }

        test("a redirect of every target, a copy of any permanent and a pipeline bounce stay two-sided") {
            intents.spellModeTargetPolarities("Insidious Will", listOf(1)) shouldBe listOf(TargetPolarity.FLEXIBLE)
            tags("Insidious Will") shouldContain IntentTag.SPELL_REDIRECT
            intents.spellModeTargetPolarities("Saheeli's Artistry", listOf(0)) shouldBe listOf(TargetPolarity.FLEXIBLE)
            intents.spellModeTargetPolarities("Saheeli's Artistry", listOf(1)) shouldBe listOf(TargetPolarity.FLEXIBLE)
            intents.spellTargetPolarities("Paradoxical Outcome") shouldBe listOf(TargetPolarity.FLEXIBLE)
        }

        test("Shrewd Negotiation takes their Craw Wurm for our Ornithopter") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(6)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardInHand(1, "Shrewd Negotiation")
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardOnBattlefield(1, "Juggernaut")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardOnBattlefield(2, "Llanowar Elves")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val cast = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id)
                .first { action ->
                    val spell = action.action as? CastSpell ?: return@first false
                    game.state.getEntity(spell.cardId)?.get<CardComponent>()?.name == "Shrewd Negotiation"
                }

            withClue("the artifact we hand over is the price, not a wrong-side target that vetoes the cast") {
                TargetSelection.aimsOnlyAtTheWrongSide(game.state, cast, game.player1Id, intents) shouldBe false
            }
            val filled = TargetSelection.fillHeuristically(
                game.state, cast, game.player1Id, fillPartialRequirements = true,
                intents = intents, polarityFromEffect = true,
            ) as CastSpell
            val picked = filled.targets.map { (it as ChosenTarget.Permanent).entityId }
            withClue("give away the 0/2 Ornithopter, take the 6/4 Craw Wurm") {
                picked shouldBe listOf(game.findPermanent("Ornithopter")!!, game.findPermanent("Craw Wurm")!!)
            }
        }
    }
}
