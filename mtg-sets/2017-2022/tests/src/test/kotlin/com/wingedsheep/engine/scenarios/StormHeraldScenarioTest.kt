package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Storm Herald (THB #156) — {2}{R} Creature — Human Shaman, 3/2, haste.
 *
 * "When this creature enters, return any number of Aura cards from your graveyard to the
 *  battlefield attached to creatures you control. Exile those Auras at the beginning of your next
 *  end step. If those Auras would leave the battlefield, exile them instead of putting them
 *  anywhere else."
 *
 * Pins the host restriction (only creatures you control, Storm Herald included; an opponent's
 * creature is never offered), that an Aura which can't enchant a creature you control stays in the
 * graveyard (ruling), that only the returned Auras are exiled at your end step, and the
 * exile-instead replacement when a returned Aura's host dies.
 */
class StormHeraldScenarioTest : ScenarioTestBase() {

    private fun heraldGame() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Storm Herald")
        .withCardInHand(1, "Murder")
        .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInGraveyard(1, "Holy Strength")
        .withCardInGraveyard(1, "Pacifism")
        .withCardInGraveyard(1, "Wild Growth")
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.hostOf(auraId: EntityId): EntityId? =
        state.getEntity(auraId)?.get<AttachedToComponent>()?.targetId

    /** Cast Storm Herald and choose [returning] from the graveyard when its trigger resolves. */
    private fun TestGame.castHeraldReturning(vararg returning: String) {
        castSpell(1, "Storm Herald").error shouldBe null
        resolveStack()
        val chosen = returning.map { name -> findCardsInGraveyard(1, name).single() }
        selectCards(chosen).error shouldBe null
    }

    /** Answer the host prompt for [auraName], asserting the hosts offered. */
    private fun TestGame.attach(auraName: String, host: EntityId, expectedHosts: Set<EntityId>) {
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
        decision.context.sourceName shouldBe auraName
        withClue("only creatures you control are offered as hosts for $auraName") {
            decision.legalTargets.values.flatten().toSet() shouldBe expectedHosts
        }
        selectTargets(listOf(host)).error shouldBe null
    }

    init {
        context("Storm Herald") {

            test("returns the chosen Auras onto creatures you control and exiles them at your end step") {
                val game = heraldGame()
                val holyStrength = game.findCardsInGraveyard(1, "Holy Strength").single()
                val pacifism = game.findCardsInGraveyard(1, "Pacifism").single()
                game.castHeraldReturning("Holy Strength", "Pacifism", "Wild Growth")

                val herald = game.findPermanent("Storm Herald")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.attach("Holy Strength", herald, setOf(herald, giant))
                game.attach("Pacifism", giant, setOf(herald, giant))
                game.resolveStack()

                withClue("each returned Aura is attached to the creature chosen for it") {
                    game.hostOf(holyStrength) shouldBe herald
                    game.hostOf(pacifism) shouldBe giant
                }
                withClue("Wild Growth can't enchant a creature, so it stays in the graveyard (ruling)") {
                    game.isInGraveyard(1, "Wild Growth") shouldBe true
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("the returned Auras are exiled at the beginning of your next end step") {
                    game.isInExile(1, "Holy Strength") shouldBe true
                    game.isInExile(1, "Pacifism") shouldBe true
                }
                withClue("the Aura that never returned is untouched") {
                    game.isInGraveyard(1, "Wild Growth") shouldBe true
                }
            }

            test("Auras you leave in the graveyard stay there") {
                val game = heraldGame()
                game.castHeraldReturning("Holy Strength")
                val herald = game.findPermanent("Storm Herald")!!
                game.attach("Holy Strength", herald, setOf(herald, game.findPermanent("Hill Giant")!!))
                game.resolveStack()

                game.isInGraveyard(1, "Pacifism") shouldBe true
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.isInExile(1, "Holy Strength") shouldBe true
                game.isInGraveyard(1, "Pacifism") shouldBe true
            }

            test("a returned Aura whose host dies is exiled instead of going to the graveyard") {
                val game = heraldGame()
                game.castHeraldReturning("Holy Strength")
                val giant = game.findPermanent("Hill Giant")!!
                game.attach("Holy Strength", giant, setOf(game.findPermanent("Storm Herald")!!, giant))
                game.resolveStack()

                game.castSpell(1, "Murder", giant).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant died") { game.isInGraveyard(1, "Hill Giant") shouldBe true }
                withClue("the unattached Aura is exiled instead of put into the graveyard") {
                    game.isInExile(1, "Holy Strength") shouldBe true
                    game.isInGraveyard(1, "Holy Strength") shouldBe false
                }
            }
        }
    }
}
