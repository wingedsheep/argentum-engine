package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import io.kotest.matchers.shouldBe

/**
 * Haliya, Ascendant Cadet:
 *  - Whenever Haliya enters or attacks, put a +1/+1 counter on target creature you control.
 *  - Whenever one or more creatures you control with +1/+1 counters on them deal combat damage
 *    to a player, draw a card.
 *
 * The second ability relies on the combat-damage batch trigger honoring a state predicate
 * (the +1/+1 counter filter), so the negative test below guards that the engine does not fire
 * the draw for a counter-less creature.
 */
class HaliyaAscendantCadetScenarioTest : ScenarioTestBase() {

    init {
        test("Haliya's enter trigger puts a +1/+1 counter on a target creature you control") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Haliya, Ascendant Cadet")
                .withCardOnBattlefield(1, "Forest")
                .withCardOnBattlefield(1, "Plains")
                .withCardOnBattlefield(1, "Plains")
                .withCardOnBattlefield(1, "Plains")
                .withCardOnBattlefield(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Haliya, Ascendant Cadet")
            // Resolve the creature spell; the enter trigger then asks for a target.
            game.resolveStack()

            val haliyaId = game.findPermanent("Haliya, Ascendant Cadet")!!
            // Only Haliya is on the battlefield, so it targets itself.
            game.selectTargets(listOf(haliyaId))
            game.resolveStack()

            val counters = game.state.getEntity(haliyaId)?.get<CountersComponent>()
            counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1

            val projected = game.state.projectedState
            projected.getPower(haliyaId) shouldBe 4
            projected.getToughness(haliyaId) shouldBe 4
        }

        test("attack trigger counters a creature, then that creature's combat damage draws a card") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Haliya, Ascendant Cadet")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val handSizeBefore = game.handSize(1)

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf(
                    "Haliya, Ascendant Cadet" to 2,
                    "Hill Giant" to 2
                )
            )

            // Haliya's attack trigger puts a +1/+1 counter on the Hill Giant.
            val hillGiantId = game.findPermanent("Hill Giant")!!
            game.selectTargets(listOf(hillGiantId))
            game.resolveStack()

            val giantCounters = game.state.getEntity(hillGiantId)?.get<CountersComponent>()
            giantCounters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            // The Hill Giant had a +1/+1 counter when it connected, so the draw trigger fires once.
            game.handSize(1) shouldBe handSizeBefore + 1
        }

        test("a creature without a +1/+1 counter dealing combat damage does not draw a card") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Haliya, Ascendant Cadet")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val handSizeBefore = game.handSize(1)

            // Attack only with the counter-less Hill Giant; Haliya stays back so no counter is placed.
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            // No creature with a +1/+1 counter dealt combat damage, so Haliya's draw does not fire.
            game.handSize(1) shouldBe handSizeBefore
        }
    }
}
