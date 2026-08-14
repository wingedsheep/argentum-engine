package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gnashing of Teeth (HOB #69) — {1}{B}{B} Sorcery.
 *
 * "Choose one —
 *  • Target creature gets -5/-5 until end of turn. If that creature would die this turn, exile it instead.
 *  • Creatures target player controls get -1/-1 until end of turn."
 *
 * Mode 0's replacement rider is the interesting half: the creature must end up in *exile*, not the
 * graveyard. Mode 1 is a one-sided sweeper, so it must miss the caster's own creatures.
 */
class GnashingOfTeethScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature(
                "Test Cave Troll", ManaCost.parse("{5}{G}"), emptySet(), power = 6, toughness = 7
            )
        )

        context("Gnashing of Teeth") {

            test("mode 0 — -5/-5 kills the creature and exiles it instead of burying it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gnashing of Teeth")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                game.castSpellWithMode(1, "Gnashing of Teeth", modeIndex = 0, targetId = courser)
                    .error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("a 3/3 with -5/-5 dies") {
                    game.findPermanent("Centaur Courser") shouldBe null
                }
                withClue("the replacement sent it to exile, not the graveyard") {
                    game.isInExile(2, "Centaur Courser") shouldBe true
                    game.isInGraveyard(2, "Centaur Courser") shouldBe false
                }
            }

            test("mode 0 — a creature that survives the -5/-5 stays on the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gnashing of Teeth")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    // Test Cave Troll is a vanilla 6/7 — it survives at 1/2.
                    .withCardOnBattlefield(2, "Test Cave Troll")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val troll = game.findPermanent("Test Cave Troll")!!
                game.castSpellWithMode(1, "Gnashing of Teeth", modeIndex = 0, targetId = troll)
                    .error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("6/7 minus 5/5 is a 1/2, which lives") {
                    game.isOnBattlefield("Test Cave Troll") shouldBe true
                    game.state.projectedState.getPower(troll) shouldBe 1
                    game.state.projectedState.getToughness(troll) shouldBe 2
                }
            }

            test("mode 1 — only the targeted player's creatures shrink") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gnashing of Teeth")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ownCourser = game.findPermanent("Centaur Courser")!!
                val theirBears = game.findPermanent("Grizzly Bears")!!
                val spell = game.state.getHand(game.player1Id).single { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Gnashing of Teeth"
                }
                val target = listOf(ChosenTarget.Player(game.player2Id))

                game.execute(
                    CastSpell(
                        game.player1Id, spell, target,
                        chosenModes = listOf(1),
                        modeTargetsOrdered = listOf(target)
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the targeted player's 2/2 became a 1/1") {
                    game.state.projectedState.getPower(theirBears) shouldBe 1
                    game.state.projectedState.getToughness(theirBears) shouldBe 1
                }
                withClue("their 1/1 became a 0/0 and died") {
                    game.findPermanent("Savannah Lions") shouldBe null
                    game.isInGraveyard(2, "Savannah Lions") shouldBe true
                }
                withClue("the caster's own creature is untouched — this is one-sided") {
                    game.state.projectedState.getPower(ownCourser) shouldBe 3
                    game.state.projectedState.getToughness(ownCourser) shouldBe 3
                }
            }
        }
    }
}
