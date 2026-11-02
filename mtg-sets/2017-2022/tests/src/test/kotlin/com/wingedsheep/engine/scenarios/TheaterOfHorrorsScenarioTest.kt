package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.rna.cards.TheaterOfHorrors
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Theater of Horrors: the linked-exile play permission is gated by both "during your turn" and the
 * "an opponent lost life this turn" tracker (a conditional static over the linked-exile grant).
 */
class TheaterOfHorrorsScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(TheaterOfHorrors))
        initMirrorMatch(Deck.of("Island" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.canCast(me: EntityId, id: EntityId) =
        legalActions(me).any { it.affordable && (it.action as? CastSpell)?.cardId == id }

    fun GameTestDriver.canPlayLand(me: EntityId, id: EntityId) =
        legalActions(me).any { it.affordable && (it.action as? PlayLand)?.cardId == id }

    fun GameTestDriver.ping(me: EntityId, theater: EntityId, opp: EntityId) {
        giveMana(me, Color.RED, 4)
        submit(
            ActivateAbility(
                playerId = me,
                sourceId = theater,
                abilityId = TheaterOfHorrors.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Player(opp))
            )
        ).error shouldBe null
        bothPass()
    }

    /** Puts [cardName] on top of my library and passes to my next upkeep, resolving the exile trigger. */
    fun GameTestDriver.exileAtUpkeep(me: EntityId, theater: EntityId, cardName: String): EntityId {
        val card = putCardOnTopOfLibrary(me, cardName)
        passPriorityUntil(Step.UPKEEP, activePlayer = me)
        stackSize shouldBe 1
        bothPass()
        state.getZone(ZoneKey(me, Zone.EXILE)) shouldBe listOf(card)
        state.getEntity(theater)!!.get<LinkedExileComponent>()!!.exiledIds shouldBe listOf(card)
        return card
    }

    test("upkeep exiles the top card; it becomes castable only once an opponent has lost life") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val theater = d.putPermanentOnBattlefield(me, TheaterOfHorrors.name)
        val bears = d.exileAtUpkeep(me, theater, "Grizzly Bears")
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.giveMana(me, Color.GREEN, 2)
        d.canCast(me, bears) shouldBe false

        d.ping(me, theater, opp)
        d.getLifeTotal(opp) shouldBe 19

        d.canCast(me, bears) shouldBe true
        d.castSpell(me, bears).outcome shouldBe Outcome.Done
        d.bothPass()
        d.assertPermanentExists(me, "Grizzly Bears")
    }

    test("exiled lands may be played once an opponent has lost life") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val theater = d.putPermanentOnBattlefield(me, TheaterOfHorrors.name)
        val forest = d.exileAtUpkeep(me, theater, "Forest")
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.canPlayLand(me, forest) shouldBe false
        d.ping(me, theater, opp)
        d.canPlayLand(me, forest) shouldBe true
        d.playLand(me, forest).outcome shouldBe Outcome.Done
        d.assertPermanentExists(me, "Forest")
    }

    test("no permission on an opponent's turn even after that opponent lost life") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val theater = d.putPermanentOnBattlefield(me, TheaterOfHorrors.name)
        val bolt = d.exileAtUpkeep(me, theater, "Lightning Bolt")

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = opp)
        d.passPriority(opp)
        d.ping(me, theater, opp)
        d.getLifeTotal(opp) shouldBe 19
        if (d.priorityPlayer != me) d.passPriority(d.priorityPlayer!!)

        d.giveMana(me, Color.RED)
        d.canCast(me, bolt) shouldBe false
    }
})
