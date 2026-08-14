package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Thranduil's Decree — "Counter target spell. If a permanent spell is countered this way, exile it
 * instead of putting it into its owner's graveyard. You may cast that card without paying its mana
 * cost for as long as it remains exiled."
 *
 * The card composes an existing `ConditionalEffect` over `Conditions.TargetMatchesFilter`, but it is
 * the first one to evaluate that condition against a **spell on the stack** rather than a
 * battlefield permanent — `ConditionEvaluator.evaluateTargetFilterMatch` has to route
 * `ChosenTarget.Spell` to the spell entity and read its printed card types. Both branches are
 * covered here because getting the condition backwards would silently swap the two destinations.
 */
class ThranduilsDecreeScenarioTest : ScenarioTestBase() {

    init {
        test("a countered permanent spell is exiled and its caster's opponent may recast it free") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Thranduil's Decree")
                .withLandsOnBattlefield(1, "Island", 6)
                .withCardInHand(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Grizzly Bears").error shouldBe null
            game.passPriority() // P2 passes so P1 can respond

            val bearsId = game.state.stack.first()
            val decreeId = game.state.getHand(game.player1Id).first()
            val cast = game.execute(
                CastSpell(game.player1Id, decreeId, targets = listOf(ChosenTarget.Spell(bearsId)))
            )
            withClue("Countering a creature spell should be legal: ${cast.error}") {
                cast.error shouldBe null
            }
            game.resolveStack()

            withClue("A permanent spell goes to exile, not to its owner's graveyard") {
                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Grizzly Bears") shouldBe false
            }
            withClue("It never reached the battlefield — it was countered, not blinked") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }

            val exiled = game.state.getZone(game.player2Id, Zone.EXILE).first { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
            }
            val freeCast = game.state.getEntity(exiled)?.get<PlayWithoutPayingCostComponent>()
            withClue("The Decree's controller — not the Bears' owner — may cast it for free") {
                freeCast shouldNotBe null
                freeCast!!.controllerId shouldBe game.player1Id
                freeCast.permanent shouldBe true
            }
        }

        test("a countered instant goes to its owner's graveyard, with no exile and no free cast") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Thranduil's Decree")
                .withLandsOnBattlefield(1, "Island", 6)
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            game.passPriority()

            val boltId = game.state.stack.first()
            val decreeId = game.state.getHand(game.player1Id).first()
            val cast = game.execute(
                CastSpell(game.player1Id, decreeId, targets = listOf(ChosenTarget.Spell(boltId)))
            )
            withClue("Countering an instant should be legal: ${cast.error}") {
                cast.error shouldBe null
            }
            game.resolveStack()

            withClue("Only *permanent* spells are exiled by the Decree") {
                game.isInGraveyard(2, "Lightning Bolt") shouldBe true
                game.isInExile(2, "Lightning Bolt") shouldBe false
            }
            withClue("The bolt was countered, so its damage never happened") {
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
