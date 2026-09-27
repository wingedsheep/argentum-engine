package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.CruelDeceiver
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Cruel Deceiver (CHK #106) — "{1}: Look at the top card of your library. {2}: Reveal the top card
 * of your library. If it's a land card, this creature gains 'Whenever this creature deals damage
 * to a creature, destroy that creature' until end of turn. Activate only once each turn."
 */
class CruelDeceiverScenarioTest : FunSpec({

    val lookAbility = CruelDeceiver.activatedAbilities[0].id
    val revealAbility = CruelDeceiver.activatedAbilities[1].id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + CruelDeceiver)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.graveyard(player: com.wingedsheep.sdk.model.EntityId) =
        state.getZone(ZoneKey(player, Zone.GRAVEYARD))

    fun GameTestDriver.battlefield(player: com.wingedsheep.sdk.model.EntityId) =
        state.getZone(ZoneKey(player, Zone.BATTLEFIELD))

    fun GameTestDriver.attackIntoCourser(deceiver: com.wingedsheep.sdk.model.EntityId,
                                         courser: com.wingedsheep.sdk.model.EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, listOf(deceiver), player2)
        bothPass()
        declareBlockers(player2, mapOf(courser to listOf(deceiver)))
        passPriorityUntil(Step.END_COMBAT)
    }

    test("revealing a land grants the destroy trigger; a damaged blocker is destroyed") {
        val d = driver()
        val top = d.putCardOnTopOfLibrary(d.player1, "Swamp")
        val deceiver = d.putCreatureOnBattlefield(d.player1, "Cruel Deceiver")
        d.removeSummoningSickness(deceiver)
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        d.giveMana(d.player1, Color.BLACK, 2)

        d.submit(ActivateAbility(d.player1, deceiver, revealAbility)).outcome shouldBe Outcome.Done
        d.bothPass()
        withClue("the revealed card is not moved") {
            d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY)).first() shouldBe top
        }

        d.attackIntoCourser(deceiver, courser)

        withClue("the 3/3 Courser took only 2 damage but the granted trigger destroyed it") {
            d.graveyard(d.player2).contains(courser) shouldBe true
        }
        withClue("the 2/1 Deceiver died to the Courser's 3 damage") {
            d.graveyard(d.player1).contains(deceiver) shouldBe true
        }
    }

    test("revealing a nonland card grants nothing; the blocker survives") {
        val d = driver()
        val top = d.putCardOnTopOfLibrary(d.player1, "Grizzly Bears")
        val deceiver = d.putCreatureOnBattlefield(d.player1, "Cruel Deceiver")
        d.removeSummoningSickness(deceiver)
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        d.giveMana(d.player1, Color.BLACK, 2)

        d.submit(ActivateAbility(d.player1, deceiver, revealAbility)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY)).first() shouldBe top

        d.attackIntoCourser(deceiver, courser)

        d.battlefield(d.player2).contains(courser) shouldBe true
        d.graveyard(d.player1).contains(deceiver) shouldBe true
    }

    test("the reveal ability can be activated only once each turn") {
        val d = driver()
        d.putCardOnTopOfLibrary(d.player1, "Swamp")
        val deceiver = d.putCreatureOnBattlefield(d.player1, "Cruel Deceiver")
        d.giveMana(d.player1, Color.BLACK, 4)

        d.submit(ActivateAbility(d.player1, deceiver, revealAbility)).outcome shouldBe Outcome.Done
        d.bothPass()

        d.submitExpectFailure(ActivateAbility(d.player1, deceiver, revealAbility))
    }

    test("the look ability leaves the top card in place and can be activated repeatedly") {
        val d = driver()
        val top = d.putCardOnTopOfLibrary(d.player1, "Grizzly Bears")
        val deceiver = d.putCreatureOnBattlefield(d.player1, "Cruel Deceiver")
        d.giveMana(d.player1, Color.BLACK, 2)

        repeat(2) {
            d.submit(ActivateAbility(d.player1, deceiver, lookAbility)).outcome shouldBe Outcome.Done
            d.bothPass()
        }

        d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY)).first() shouldBe top
    }
})
