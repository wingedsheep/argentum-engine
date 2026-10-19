package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.leg.cards.RingOfImmortals
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ring of Immortals — {5} Artifact.
 * {3}, {T}: Counter target instant or Aura spell that targets a permanent you control.
 *
 * Pinned: an instant or an Aura spell aimed at one of your permanents can be countered; an instant
 * aimed only at an opponent's permanent, and a sorcery aimed at yours, are not legal targets.
 */
class RingOfImmortalsScenarioTest : ScenarioTestBase() {

    private val abilityId = RingOfImmortals.activatedAbilities.first().id

    /** Player 1 controls Ring of Immortals, three Islands and Grizzly Bears; player 2 is active. */
    private fun board(spell: String, lands: String, landCount: Int) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Ring of Immortals")
        .withLandsOnBattlefield(1, "Island", 3)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withLandsOnBattlefield(2, lands, landCount)
        .withCardInHand(2, spell)
        .withActivePlayer(2)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.spellOnStack(name: String): EntityId =
        state.stack.first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    /** Player 2 passes with [spell] on the stack; player 1 activates Ring of Immortals targeting it. */
    private fun TestGame.activateRing(spell: String) = run {
        passPriority()
        execute(
            ActivateAbility(
                player1Id,
                findPermanent("Ring of Immortals")!!,
                abilityId,
                listOf(ChosenTarget.Spell(spellOnStack(spell)))
            )
        )
    }

    init {
        context("Ring of Immortals") {
            test("counters an instant that targets a creature you control") {
                val game = board("Lightning Bolt", "Mountain", 1)
                game.castSpell(2, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                val result = game.activateRing("Lightning Bolt")
                withClue("Ring can target Lightning Bolt: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Lightning Bolt") shouldBe true
            }

            test("counters an Aura spell that would enchant a creature you control") {
                val game = board("Pacifism", "Plains", 2)
                game.castSpell(2, "Pacifism", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                val result = game.activateRing("Pacifism")
                withClue("Ring can target Pacifism: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.isOnBattlefield("Pacifism") shouldBe false
                game.isInGraveyard(2, "Pacifism") shouldBe true
            }

            test("can't target an instant that targets only an opponent's permanent") {
                val game = board("Lightning Bolt", "Mountain", 1)
                game.castSpell(2, "Lightning Bolt", game.findPermanent("Hill Giant")!!).error shouldBe null
                game.activateRing("Lightning Bolt").error shouldNotBe null
            }

            test("can't target a sorcery, even one aimed at your permanent") {
                val game = board("Stone Rain", "Mountain", 3)
                game.castSpell(2, "Stone Rain", game.findPermanents("Island").first()).error shouldBe null
                game.activateRing("Stone Rain").error shouldNotBe null
            }
        }
    }
}
