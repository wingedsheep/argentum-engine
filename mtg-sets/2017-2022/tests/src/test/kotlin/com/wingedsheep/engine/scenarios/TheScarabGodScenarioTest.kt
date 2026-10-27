package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hou.cards.TheScarabGod
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * The Scarab God — {3}{U}{B} Legendary Creature — God 5/5
 * {2}{U}{B}: Exile target creature card from a graveyard. Create a token that's a copy of it,
 * except it's a 4/4 black Zombie.
 * When The Scarab God dies, return it to its owner's hand at the beginning of the next end step.
 */
class TheScarabGodScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + TheScarabGod)
        initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("exiles a creature card from an opponent's graveyard and makes a 4/4 black Zombie copy") {
        val d = driver()
        val you = d.player1
        val opp = d.player2
        val god = d.putCreatureOnBattlefield(you, "The Scarab God")
        val deadBears = d.putCardInGraveyard(opp, "Grizzly Bears")

        d.giveMana(you, Color.BLUE, 2)
        d.giveMana(you, Color.BLACK, 2)
        d.submit(
            ActivateAbility(
                you,
                god,
                TheScarabGod.activatedAbilities.single().id,
                listOf(ChosenTarget.Card(deadBears, opp, Zone.GRAVEYARD)),
            )
        ).outcome shouldBe Outcome.Done
        d.bothPass()

        d.getExile(opp) shouldContain deadBears
        val token = d.getCreatures(you).single { it != god }
        val projected = d.state.projectedState
        projected.getPower(token) shouldBe 4
        projected.getToughness(token) shouldBe 4
        projected.getColors(token) shouldBe setOf("BLACK")
        projected.getSubtypes(token) shouldBe setOf("Zombie")
        d.state.getEntity(token)?.get<CardComponent>()?.name shouldBe
            "Grizzly Bears"
    }

    test("returns to its owner's hand at the beginning of the next end step after it dies") {
        val d = driver()
        val you = d.player1
        val god = d.putCreatureOnBattlefield(you, "The Scarab God")

        val murder = d.putCardInHand(you, "Murder")
        d.giveMana(you, Color.BLACK, 3)
        d.castSpell(you, murder, listOf(god)).outcome shouldBe Outcome.Done
        d.bothPass() // Murder resolves
        d.bothPass() // dies trigger resolves, scheduling the delayed return

        d.state.getZone(ZoneKey(you, Zone.GRAVEYARD)).contains(god) shouldBe true
        d.passPriorityUntil(Step.END)
        d.bothPass()

        d.state.getZone(ZoneKey(you, Zone.HAND)).contains(god) shouldBe true
        d.state.getZone(ZoneKey(you, Zone.GRAVEYARD)).contains(god) shouldBe false
    }
})
