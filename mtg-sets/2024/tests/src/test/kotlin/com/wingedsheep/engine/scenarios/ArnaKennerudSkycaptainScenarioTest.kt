package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Arna Kennerüd, Skycaptain (MH3 #178) — {2}{W}{U}{B} 4/4.
 * "Whenever a modified creature you control attacks, double the number of each kind of counter on
 * it. Then for each nontoken permanent attached to it, create a token that's a copy of that
 * permanent attached to that creature."
 */
class ArnaKennerudSkycaptainScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.tokensAttachedTo(host: EntityId): List<EntityId> =
            state.getBattlefield().filter { id ->
                val c = state.getEntity(id) ?: return@filter false
                c.get<TokenComponent>() != null && c.get<AttachedToComponent>()?.targetId == host
            }

        fun TestGame.attackWithBears() {
            passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            resolveStack()
        }

        context("Arna Kennerüd, Skycaptain") {
            test("doubles the attacker's counters and copies each nontoken attachment onto it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Arna Kennerüd, Skycaptain")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                    .withCardAttachedTo(1, "Bonesplitter", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = game.state.updateEntity(bears) {
                    it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
                }

                game.attackWithBears()

                game.state.getEntity(bears)!!.get<CountersComponent>()!!
                    .getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 4
                val copies = game.tokensAttachedTo(bears)
                withClue("one Holy Strength and one Bonesplitter token, never a copy of a copy") {
                    copies shouldHaveSize 2
                    game.findPermanents("Holy Strength") shouldHaveSize 2
                    game.findPermanents("Bonesplitter") shouldHaveSize 2
                }
                // 2/2 + four +1/+1 counters + two Holy Strength (+1/+2) + two Bonesplitter (+2/+0).
                game.state.projectedState.getPower(bears) shouldBe 12
                game.state.projectedState.getToughness(bears) shouldBe 10
            }

            test("a token already attached to the attacker isn't copied") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Arna Kennerüd, Skycaptain")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                    .withCardAttachedTo(1, "Bonesplitter", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val holyStrength = game.findPermanent("Holy Strength")!!
                game.state = game.state.updateEntity(holyStrength) { it.with(TokenComponent) }

                game.attackWithBears()

                game.findPermanents("Holy Strength") shouldHaveSize 1
                game.findPermanents("Bonesplitter") shouldHaveSize 2
            }

            test("a creature modified only by counters just doubles them") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Arna Kennerüd, Skycaptain")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = game.state.updateEntity(bears) {
                    it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
                }

                game.attackWithBears()

                game.state.getEntity(bears)!!.get<CountersComponent>()!!
                    .getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                game.tokensAttachedTo(bears).shouldBeEmpty()
            }

            test("an unmodified attacker doesn't trigger it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Arna Kennerüd, Skycaptain")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.state.stack.shouldBeEmpty()
            }
        }
    }
}
