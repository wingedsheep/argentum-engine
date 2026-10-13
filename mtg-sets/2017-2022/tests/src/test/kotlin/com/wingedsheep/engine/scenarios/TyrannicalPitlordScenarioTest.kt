package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dmu.cards.TyrannicalPitlord
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Tyrannical Pitlord (DMU) — "As this creature enters, choose another creature you control. The
 * chosen creature gets +3/+3 and has flying. When this creature leaves the battlefield, sacrifice
 * the chosen creature."
 *
 * Covers the source-chosen static scope (the bonus lands on the chosen creature only, follows it to
 * a new controller, and is gone for good once that object leaves) and the leaves trigger reading
 * the choice as it last existed — the Pitlord's own cast-choices bag is stripped as it leaves.
 */
class TyrannicalPitlordScenarioTest : ScenarioTestBase() {

    private fun TestGame.drainMana() {
        var guard = 0
        while (guard++ < 15 && getPendingDecision() is SelectManaSourcesDecision) {
            submitManaSourcesAutoPay()
        }
    }

    private fun pitlordGame(vararg extraHand: String) = scenario()
        .withPlayers("Alice", "Bob")
        .withCardInHand(1, "Tyrannical Pitlord")
        .also { b -> extraHand.forEach { b.withCardInHand(1, it) } }
        .withLandsOnBattlefield(1, "Swamp", 9)
        .withLandsOnBattlefield(1, "Plains", 1)
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Cast the Pitlord and choose [chosen] as it enters. */
    private fun TestGame.castPitlordChoosing(chosen: EntityId): EntityId {
        castSpell(1, "Tyrannical Pitlord").error shouldBe null
        drainMana()
        resolveStack()
        val decision = getPendingDecision()
        withClue("the choice is made as it enters, among the other creatures you control") {
            (decision as? SelectCardsDecision)?.options?.toSet() shouldBe
                setOf(findPermanent("Grizzly Bears")!!, findPermanent("Hill Giant")!!)
        }
        selectCards(listOf(chosen)).error shouldBe null
        return findPermanent("Tyrannical Pitlord")!!
    }

    private fun TestGame.cast(name: String, target: EntityId) {
        castSpell(1, name, target).error shouldBe null
        drainMana()
        resolveStack()
    }

    private fun TestGame.zoneOf(entityId: EntityId): Zone? =
        state.zones.entries.firstOrNull { (_, ids) -> entityId in ids }?.key?.zoneType

    init {
        cardRegistry.register(TyrannicalPitlord)

        context("Tyrannical Pitlord") {

            test("the chosen creature gets +3/+3 and flying; the other one doesn't") {
                val game = pitlordGame()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castPitlordChoosing(bears)

                val projected = game.state.projectedState
                projected.getPower(bears) shouldBe 5
                projected.getToughness(bears) shouldBe 5
                projected.hasKeyword(bears, "FLYING") shouldBe true
                withClue("the creature that wasn't chosen is untouched") {
                    projected.getPower(giant) shouldBe 3
                    projected.hasKeyword(giant, "FLYING") shouldBe false
                }
            }

            test("when it leaves the battlefield, the chosen creature is sacrificed") {
                val game = pitlordGame("Murder")
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val pitlord = game.castPitlordChoosing(bears)

                game.cast("Murder", pitlord)

                withClue("the leaves trigger read the choice the departed Pitlord made") {
                    game.zoneOf(bears) shouldBe Zone.GRAVEYARD
                }
                withClue("only the chosen creature") { game.zoneOf(giant) shouldBe Zone.BATTLEFIELD }
            }

            test("a chosen creature that blinks is a new object: no bonus, and it isn't sacrificed") {
                val game = pitlordGame("Cloudshift", "Murder")
                val bears = game.findPermanent("Grizzly Bears")!!
                val pitlord = game.castPitlordChoosing(bears)

                game.cast("Cloudshift", bears)
                val returned = game.findPermanent("Grizzly Bears")!!
                withClue("the card keeps its entity id, so only the object generation tells them apart") {
                    returned shouldBe bears
                }

                withClue("you can't choose a new creature, and the returned card isn't the chosen one") {
                    game.state.projectedState.getPower(returned) shouldBe 2
                    game.state.projectedState.hasKeyword(returned, "FLYING") shouldBe false
                }

                game.cast("Murder", pitlord)
                withClue("the leaves trigger has no chosen creature left to sacrifice") {
                    game.zoneOf(returned) shouldBe Zone.BATTLEFIELD
                }
            }

            test("a stolen chosen creature keeps the bonus but isn't sacrificed") {
                val game = pitlordGame("Murder")
                val bears = game.findPermanent("Grizzly Bears")!!
                val pitlord = game.castPitlordChoosing(bears)

                game.state = game.state.updateEntity(bears) { it.with(ControllerComponent(game.player2Id)) }

                withClue("ruling: it continues to have flying and get +3/+3 under another controller") {
                    game.state.projectedState.getController(bears) shouldBe game.player2Id
                    game.state.projectedState.getPower(bears) shouldBe 5
                    game.state.projectedState.hasKeyword(bears, "FLYING") shouldBe true
                }

                game.cast("Murder", pitlord)
                withClue("ruling: a chosen creature you don't control isn't sacrificed") {
                    game.zoneOf(bears) shouldBe Zone.BATTLEFIELD
                }
                withClue("and once the Pitlord is gone the bonus is too") {
                    game.state.projectedState.getPower(bears) shouldBe 2
                }
            }
        }
    }
}
