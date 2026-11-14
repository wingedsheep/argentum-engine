package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Talent of the Telepath (ORI #78) — {2}{U}{U} Sorcery.
 *
 *   Target opponent reveals the top seven cards of their library. You may cast an instant or
 *   sorcery spell from among them without paying its mana cost. Then that player puts the rest
 *   into their graveyard.
 *   Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, you
 *   may cast up to two instant and/or sorcery spells from among the revealed cards instead of one.
 *
 * Covers casting straight out of the opponent's library (untargeted and targeted), the rest being
 * milled into the opponent's graveyard (the eighth card staying put), declining the cast, a
 * creature not being castable, and spell mastery allowing exactly two casts — the one cast last
 * resolving first — with the cast cards ending in their owner's graveyard.
 */
class TalentOfTheTelepathScenarioTest : ScenarioTestBase() {

    private fun TestGame.namesIn(ids: List<EntityId>): List<String> =
        ids.map { state.getEntity(it)!!.get<CardComponent>()!!.name }

    /**
     * The opponent's top seven are [revealed] (top first), with a Forest under them; the caster has
     * [graveyard] in their graveyard and three Plains to draw from.
     */
    private fun castTalent(revealed: List<String>, graveyard: List<String> = emptyList()): TestGame {
        require(revealed.size == 7)
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Talent of the Telepath")
            .withLandsOnBattlefield(1, "Island", 4)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        for (card in revealed) builder = builder.withCardInLibrary(2, card)
        builder = builder.withCardInLibrary(2, "Forest")
        for (card in graveyard) builder = builder.withCardInGraveyard(1, card)
        repeat(3) { builder = builder.withCardInLibrary(1, "Plains") }
        val game = builder.build()

        game.castSpellTargetingPlayer(1, "Talent of the Telepath", 2).error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        context("Talent of the Telepath") {

            test("cast one instant or sorcery from the opponent's library; the rest go to their graveyard") {
                val game = castTalent(
                    listOf("Grizzly Bears", "Divination", "Think Twice", "Island", "Island", "Island", "Island")
                )
                val pick = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val bears = game.findCardsInLibrary(2, "Grizzly Bears").single()
                val divination = game.findCardsInLibrary(2, "Divination").single()
                withClue("all seven revealed cards are shown; only the two instants/sorceries are selectable") {
                    pick.options.size shouldBe 2
                    (pick.options + pick.nonSelectableOptions).toSet().size shouldBe 7
                    pick.nonSelectableOptions shouldContain bears
                }
                withClue("a creature can't be cast this way") {
                    game.selectCards(listOf(bears)).error shouldNotBe null
                }
                game.selectCards(listOf(divination)).error shouldBe null

                withClue("without spell mastery no second cast is offered") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("the rest were milled while Divination waited on the stack") {
                    game.namesIn(game.state.getLibrary(game.player2Id)) shouldBe listOf("Forest")
                    game.namesIn(game.state.getGraveyard(game.player2Id)) shouldContainExactlyInAnyOrder
                        listOf("Grizzly Bears", "Think Twice", "Island", "Island", "Island", "Island")
                }
                game.resolveStack()

                withClue("Divination resolved for the caster, then went to its owner's graveyard") {
                    game.handSize(1) shouldBe 2
                    game.isInGraveyard(2, "Divination") shouldBe true
                    game.isInGraveyard(1, "Divination") shouldBe false
                }
                game.isInGraveyard(1, "Talent of the Telepath") shouldBe true
            }

            test("a targeted spell is cast with a target of the caster's choosing") {
                val game = castTalent(
                    listOf("Lightning Bolt", "Island", "Island", "Island", "Island", "Island", "Island")
                )
                val bolt = game.findCardsInLibrary(2, "Lightning Bolt").single()
                game.selectCards(listOf(bolt)).error shouldBe null
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
                game.isInGraveyard(2, "Lightning Bolt") shouldBe true
                game.namesIn(game.state.getLibrary(game.player2Id)) shouldBe listOf("Forest")
                game.graveyardSize(2) shouldBe 7
            }

            test("declining the cast puts all seven revealed cards into the opponent's graveyard") {
                val game = castTalent(
                    listOf("Divination", "Think Twice", "Island", "Island", "Island", "Island", "Island")
                )
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.skipSelection().error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.handSize(1) shouldBe 0
                game.namesIn(game.state.getLibrary(game.player2Id)) shouldBe listOf("Forest")
                game.graveyardSize(2) shouldBe 7
                game.isInGraveyard(2, "Divination") shouldBe true
            }

            test("spell mastery: up to two spells are cast, the last cast resolving first") {
                val game = castTalent(
                    revealed = listOf("Divination", "Think Twice", "Shock", "Island", "Island", "Island", "Island"),
                    graveyard = listOf("Lightning Bolt", "Divination"),
                )
                val divination = game.findCardsInLibrary(2, "Divination").single()
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(listOf(divination)).error shouldBe null

                val second = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val thinkTwice = game.findCardsInLibrary(2, "Think Twice").single()
                withClue("the already-cast Divination is no longer among the choices") {
                    (divination in second.options) shouldBe false
                }
                game.selectCards(listOf(thinkTwice)).error shouldBe null

                withClue("two casts is the cap; the uncast Shock goes to the graveyard with the rest") {
                    game.hasPendingDecision() shouldBe false
                    game.isInGraveyard(2, "Shock") shouldBe true
                    game.namesIn(game.state.getLibrary(game.player2Id)) shouldBe listOf("Forest")
                }
                withClue("Think Twice, cast last, is on top of the stack") {
                    game.state.stack.size shouldBe 2
                    game.state.stack.last() shouldBe thinkTwice
                }
                game.resolveStack()

                withClue("both resolved for the caster: one card from Think Twice, two from Divination") {
                    game.handSize(1) shouldBe 3
                }
                game.isInGraveyard(2, "Think Twice") shouldBe true
                game.isInGraveyard(2, "Divination") shouldBe true
                game.graveyardSize(2) shouldBe 7
            }
        }
    }
}
