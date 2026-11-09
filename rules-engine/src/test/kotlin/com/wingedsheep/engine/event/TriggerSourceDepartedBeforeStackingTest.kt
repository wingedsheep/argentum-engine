package com.wingedsheep.engine.event

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * A triggered ability whose source leaves the battlefield after the ability triggers but before
 * it is put on the stack still resolves against the source's last-known information (CR 608.2h).
 *
 * Combat damage triggers its source's ability, then state-based actions destroy the source
 * before any player would receive priority, which is when the ability is put on the stack
 * (CR 603.3). The combat damage step applies those state-based actions before triggers are
 * detected, so `DamageTriggerDetector` reads a departed damage source off its departure in the
 * same batch — the only record left of a token — and stamps the snapshot on the [PendingTrigger];
 * a source leaving while its trigger already waits in the queue is stamped by
 * `ZoneTransitionService`. `TriggerProcessor` carries the snapshot onto the stack object — the
 * same one an ability already on the stack gets when its source leaves.
 *
 * Driven through Permeating Mass ("whenever this creature deals combat damage to a creature, that
 * creature becomes a copy of this creature"; ruling 2016-07-13: if Permeating Mass is dealt
 * lethal damage during combat, the creature it dealt damage to still becomes a copy of it).
 */
class TriggerSourceDepartedBeforeStackingTest : ScenarioTestBase() {

    private fun combatWith(blocker: String, massIsToken: Boolean = false): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Permeating Mass", isToken = massIsToken)
            .withCardOnBattlefield(2, blocker)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Permeating Mass" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareBlockers(mapOf(blocker to listOf("Permeating Mass"))).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        return game
    }

    private fun TestGame.stackedTrigger(): TriggeredAbilityOnStackComponent =
        state.stack.mapNotNull { state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>() }.single()

    init {
        test("a source destroyed by the combat damage that triggered it carries its departure snapshot onto the stack") {
            val game = combatWith("Hill Giant")
            val giant = game.findPermanent("Hill Giant")!!

            withClue("the 3/3 blocker killed the 1/3 Mass before its trigger reached the stack") {
                game.findPermanent("Permeating Mass") shouldBe null
            }
            val snapshot = game.stackedTrigger().lastKnownSourceSnapshot.shouldNotBeNull()
            snapshot.copiableCard?.name shouldBe "Permeating Mass"

            game.resolveStack()
            val copied = game.state.getEntity(giant)!!.get<CardComponent>()!!
            withClue("the damaged Hill Giant became a copy of the departed Mass") {
                copied.name shouldBe "Permeating Mass"
                game.state.projectedState.getPower(giant) shouldBe 1
                game.state.projectedState.getToughness(giant) shouldBe 3
                (giant in game.state.getBattlefield()) shouldBe true
            }
        }

        test("a token source that ceased to exist is still copied from its last-known values") {
            val game = combatWith("Hill Giant", massIsToken = true)
            val giant = game.findPermanent("Hill Giant")!!
            game.stackedTrigger().lastKnownSourceSnapshot.shouldNotBeNull()

            game.resolveStack()
            game.state.getEntity(giant)!!.get<CardComponent>()!!.name shouldBe "Permeating Mass"
        }

        test("a source still on the battlefield carries no snapshot and is copied live") {
            val game = combatWith("Grizzly Bears")
            val bears = game.findPermanent("Grizzly Bears")!!
            game.findPermanent("Permeating Mass").shouldNotBeNull()
            game.stackedTrigger().lastKnownSourceSnapshot.shouldBeNull()

            game.resolveStack()
            game.state.getEntity(bears)!!.get<CardComponent>()!!.name shouldBe "Permeating Mass"
            game.findPermanents("Permeating Mass").size shouldBe 2
        }

        test("a source leaving while its trigger waits to be stacked stamps only the triggers of that visit") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Permeating Mass")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val mass = game.findPermanent("Permeating Mass")!!
            val ability = cardRegistry.getCard("Permeating Mass")!!.triggeredAbilities.single()
            fun waiting(visit: Long) = PendingTrigger(
                ability = ability, sourceId = mass, sourceName = "Permeating Mass",
                sourceBattlefieldTimestamp = visit, controllerId = game.player1Id,
                triggerContext = TriggerContext(),
            )
            val state = game.state
                .updateEntity(mass) { it.with(BattlefieldEntryTimestampComponent(7L)) }
                .copy(pendingTriggers = listOf(waiting(7L), waiting(3L)))

            val zones = ZoneTransitionService(cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = cardRegistry))
            val after = zones.moveToZone(state, mass, Zone.GRAVEYARD).state

            val (sameVisit, earlierVisit) = after.pendingTriggers
            withClue("the trigger of the visit that ended carries its departure") {
                sameVisit.lastKnownSourceSnapshot.shouldNotBeNull().battlefieldEntryTimestamp shouldBe 7L
            }
            withClue("a trigger recorded against another visit of the same card is left alone") {
                earlierVisit.lastKnownSourceSnapshot.shouldBeNull()
            }
        }
    }
}
