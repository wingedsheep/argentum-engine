package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Troll Negotiations (HOB) — {2}{G}{G} Sorcery.
 * "Put two +1/+1 counters on target creature you control. Then it fights target creature an
 *  opponent controls."
 *
 * Ordering is the whole card: the counters must be on *before* the fight, so the damage dealt is
 * the boosted power. Unlike Quarrel this is a genuine fight, so damage goes both ways.
 */
class TrollNegotiationsScenarioTest : ScenarioTestBase() {

    private fun damageOn(game: TestGame, id: com.wingedsheep.sdk.model.EntityId): Int =
        game.state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    init {
        cardRegistry.register(
            CardDefinition.creature(
                "Test Cave Troll", ManaCost.parse("{5}{G}"), emptySet(), power = 6, toughness = 7
            )
        )

        context("Troll Negotiations") {

            test("counters land first, then the boosted creature fights") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Troll Negotiations")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    // A 3/3 becomes a 5/5 before fighting the 6/7 Troll.
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(2, "Test Cave Troll")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mine = game.findPermanent("Centaur Courser")!!
                val theirs = game.findPermanent("Test Cave Troll")!!
                val spell = game.state.getHand(game.player1Id).single { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Troll Negotiations"
                }

                game.execute(
                    CastSpell(
                        game.player1Id, spell,
                        listOf(ChosenTarget.Permanent(mine), ChosenTarget.Permanent(theirs))
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the fight used the boosted power (5), not the printed 3 — the counters went on first") {
                    damageOn(game, theirs) shouldBe 5
                }
                withClue("a fight deals damage both ways — the Troll's 6 killed the 5/5 back") {
                    game.isOnBattlefield("Centaur Courser") shouldBe false
                    game.isInGraveyard(1, "Centaur Courser") shouldBe true
                    game.isOnBattlefield("Test Cave Troll") shouldBe true
                }
            }

            test("the boosted creature can survive a fight it would otherwise lose") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Troll Negotiations")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    // A 3/3 that becomes a 5/5 beats a 4/4 cleanly.
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(2, "Ordinary Bear")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mine = game.findPermanent("Centaur Courser")!!
                val theirs = game.findPermanent("Ordinary Bear")!!
                val spell = game.state.getHand(game.player1Id).single { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Troll Negotiations"
                }

                game.execute(
                    CastSpell(
                        game.player1Id, spell,
                        listOf(ChosenTarget.Permanent(mine), ChosenTarget.Permanent(theirs))
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("5 damage kills the 4/5 Ordinary Bear") {
                    game.isOnBattlefield("Ordinary Bear") shouldBe false
                    game.isInGraveyard(2, "Ordinary Bear") shouldBe true
                }
                withClue("the 5/5 took only 4 and lives") {
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                    damageOn(game, mine) shouldBe 4
                }
                withClue("the two +1/+1 counters are real and still on the survivor") {
                    game.state.getEntity(mine)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                    game.state.projectedState.getPower(mine) shouldBe 5
                    game.state.projectedState.getToughness(mine) shouldBe 5
                }
            }
        }
    }
}
