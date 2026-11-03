package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.trk.cards.AssaultDrone
import com.wingedsheep.mtg.sets.definitions.trk.cards.StarfleetAcademy
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Starfleet Academy — "Federation — {5}, {T}: Choose a color. Add an amount of mana of that color
 * equal to the number of creature types among non-Borg creatures you control."
 * The {5} is paid from other lands; Borg creatures don't contribute their types.
 */
class StarfleetAcademyScenarioTest : FunSpec({

    val federation = StarfleetAcademy.activatedAbilities[1].id

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(StarfleetAcademy, AssaultDrone))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        repeat(5) { d.putLandOnBattlefield(you, "Forest") }
        return Triple(d, you, d.putLandOnBattlefield(you, "Starfleet Academy"))
    }

    fun GameTestDriver.pool(p: EntityId) = state.getEntity(p)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    test("adds mana of the chosen color equal to creature types among non-Borg creatures") {
        val (d, you, academy) = setup()
        d.putCreatureOnBattlefield(you, "Centaur Courser") // Centaur Warrior
        d.putCreatureOnBattlefield(you, "Goblin Guide") // Goblin Scout
        d.putCreatureOnBattlefield(you, "Assault Drone") // Borg — not counted
        d.submit(ActivateAbility(you, academy, federation, manaColorChoice = Color.BLUE)).error shouldBe null
        d.pool(you).blue shouldBe 4
        d.pool(you).green shouldBe 0
        d.pool(you).colorless shouldBe 0
    }

    test("shared creature types count once") {
        val (d, you, academy) = setup()
        d.putCreatureOnBattlefield(you, "Centaur Courser")
        d.putCreatureOnBattlefield(you, "Centaur Courser")
        d.submit(ActivateAbility(you, academy, federation, manaColorChoice = Color.RED)).error shouldBe null
        d.pool(you).red shouldBe 2
    }

    test("only Borg creatures adds nothing") {
        val (d, you, academy) = setup()
        d.putCreatureOnBattlefield(you, "Assault Drone")
        d.submit(ActivateAbility(you, academy, federation, manaColorChoice = Color.WHITE)).error shouldBe null
        d.pool(you).isEmpty shouldBe true
    }
})
