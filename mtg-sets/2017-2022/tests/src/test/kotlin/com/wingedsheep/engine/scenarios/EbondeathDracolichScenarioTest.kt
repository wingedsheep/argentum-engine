package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.player.CreatureDeathRecord
import com.wingedsheep.engine.state.components.player.CreatureDeathsThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.EbondeathDracolich
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ebondeath, Dracolich (AFR #100) — {2}{B}{B} 5/2 Legendary Creature — Zombie Dragon.
 *
 * "Flash / Flying / Ebondeath enters tapped. / You may cast this card from your graveyard if a
 *  creature not named Ebondeath, Dracolich died this turn."
 *
 * The graveyard permission is gated on `Conditions.CreatureNotNamedDiedThisTurn`, which reads each
 * death's last-known name off the game-wide death record.
 */
class EbondeathDracolichScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(EbondeathDracolich))
        initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.canCastFromGraveyard(player: EntityId, cardId: EntityId): Boolean =
        LegalActionEnumerator.create(cardRegistry).enumerate(state, player, EnumerationMode.FULL)
            .any { it.sourceZone == "GRAVEYARD" && (it.action as? CastSpell)?.cardId == cardId }

    fun GameTestDriver.bolt(target: EntityId) {
        val bolt = putCardInHand(player1, "Lightning Bolt")
        giveMana(player1, Color.RED, 1)
        castSpell(player1, bolt, targets = listOf(target)).error shouldBe null
        bothPass()
    }

    test("nothing died: not castable from the graveyard") {
        val d = newDriver()
        val ebondeath = d.putCardInGraveyard(d.player1, "Ebondeath, Dracolich")
        d.giveMana(d.player1, Color.BLACK, 4)
        d.canCastFromGraveyard(d.player1, ebondeath) shouldBe false
    }

    test("only a creature named Ebondeath, Dracolich died: not castable") {
        val d = newDriver()
        val ebondeath = d.putCardInGraveyard(d.player1, "Ebondeath, Dracolich")
        d.addComponent(
            d.player1,
            CreatureDeathsThisTurnComponent(listOf(CreatureDeathRecord("Ebondeath, Dracolich", setOf("Zombie", "Dragon"))))
        )
        d.giveMana(d.player1, Color.BLACK, 4)
        d.canCastFromGraveyard(d.player1, ebondeath) shouldBe false
    }

    test("an opponent's creature died: cast from the graveyard at instant speed, enters tapped with flying") {
        val d = newDriver()
        val you = d.player1
        val ebondeath = d.putCardInGraveyard(you, "Ebondeath, Dracolich")
        d.bolt(d.putPermanentOnBattlefield(d.player2, "Savannah Lions"))

        // End step of your own turn — only flash makes a creature castable here.
        d.passPriorityUntil(Step.END)
        d.giveMana(you, Color.BLACK, 4)
        d.canCastFromGraveyard(you, ebondeath) shouldBe true

        d.castSpell(you, ebondeath).error shouldBe null
        d.bothPass()

        val permanent = d.findPermanent(you, "Ebondeath, Dracolich")!!
        d.isTapped(permanent) shouldBe true
        d.state.projectedState.hasKeyword(permanent, Keyword.FLYING) shouldBe true
        d.state.getZone(you, Zone.GRAVEYARD).contains(ebondeath) shouldBe false
    }

    test("the permission ends with the turn") {
        val d = newDriver()
        val ebondeath = d.putCardInGraveyard(d.player1, "Ebondeath, Dracolich")
        d.bolt(d.putPermanentOnBattlefield(d.player1, "Savannah Lions"))
        d.giveMana(d.player1, Color.BLACK, 4)
        d.canCastFromGraveyard(d.player1, ebondeath) shouldBe true

        d.passPriorityUntil(Step.UPKEEP, activePlayer = d.player2)
        d.giveMana(d.player1, Color.BLACK, 4)
        d.canCastFromGraveyard(d.player1, ebondeath) shouldBe false
    }
})
