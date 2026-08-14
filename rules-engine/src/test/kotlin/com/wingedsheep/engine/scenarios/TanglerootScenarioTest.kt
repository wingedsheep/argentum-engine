package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Tangleroot
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tangleroot (MRD #259) — "Whenever a player casts a creature spell, that player adds {G}."
 *
 * The cross-player mana shape: the trigger is symmetric (it watches *every* player's creature
 * spells) but the {G} goes to the caster's pool, not Tangleroot's controller's, via
 * `AddManaOfChoice(recipient = PlayerRef(TriggeringPlayer))`.
 */
class TanglerootScenarioTest : FunSpec({

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Tangleroot))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = driver.activePlayer!!
        return Triple(driver, active, driver.getOpponent(active))
    }

    /** Drain the stack so the cast trigger resolves ahead of the creature spell itself. */
    fun GameTestDriver.resolveAll(max: Int = 10) {
        var i = 0
        while (state.stack.isNotEmpty() && pendingDecision == null && i++ < max) bothPass()
    }

    fun GameTestDriver.greenMana(playerId: EntityId): Int =
        state.getEntity(playerId)?.get<ManaPoolComponent>()?.getAmount(Color.GREEN) ?: 0

    test("the caster gets the {G}, not Tangleroot's controller") {
        val (driver, you, opponent) = newGame()
        driver.putPermanentOnBattlefield(opponent, "Tangleroot")

        driver.giveMana(you, Color.GREEN, 1)
        val elves = driver.putCardInHand(you, "Llanowar Elves")
        driver.castSpell(you, elves)
        driver.resolveAll()

        driver.greenMana(you) shouldBe 1
        driver.greenMana(opponent) shouldBe 0
        driver.assertPermanentExists(you, "Llanowar Elves")
    }

    test("a noncreature spell doesn't trigger it") {
        val (driver, you) = newGame()
        driver.putPermanentOnBattlefield(you, "Tangleroot")

        driver.giveMana(you, Color.GREEN, 1)
        val growth = driver.putCardInHand(you, "Giant Growth")
        val bear = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        driver.castSpellWithTargets(
            you, growth,
            listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(bear))
        )
        driver.resolveAll()

        driver.greenMana(you) shouldBe 0
    }
})
