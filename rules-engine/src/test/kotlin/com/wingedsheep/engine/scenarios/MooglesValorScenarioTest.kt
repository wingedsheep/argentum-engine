package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.MooglesValor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Moogles' Valor — {3}{W}{W} Instant
 *   "For each creature you control, create a 1/2 white Moogle creature token with lifelink.
 *    Then creatures you control gain indestructible until end of turn."
 *
 * The token count is locked in before any tokens exist (counts only the original creatures),
 * but the indestructible grant resolves afterward over "creatures you control" — which now
 * includes the freshly-created Moogles.
 */
class MooglesValorScenarioTest : FunSpec({

    val projector = StateProjector()

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(MooglesValor)
        return d
    }

    fun moogleCount(d: GameTestDriver, player: EntityId): Int =
        d.getCreatures(player).count {
            d.state.getEntity(it)?.get<CardComponent>()?.name?.contains("Moogle") == true
        }

    test("creates one Moogle per existing creature; all your creatures (incl. tokens) gain indestructible") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Two creatures already in play under our control.
        d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        d.putCreatureOnBattlefield(p1, "Centaur Courser")
        val originals = d.getCreatures(p1)
        originals.size shouldBe 2

        val spell = d.putCardInHand(p1, "Moogles' Valor")
        d.giveColorlessMana(p1, 3)
        d.giveMana(p1, Color.WHITE, 2)
        val cast = d.castSpell(p1, spell)
        cast.isSuccess shouldBe true
        d.bothPass()

        // Exactly two Moogle tokens were created (one per original creature).
        moogleCount(d, p1) shouldBe 2

        // Four creatures total, every one of them indestructible.
        val all = d.getCreatures(p1)
        all.size shouldBe 4
        all.forEach { id ->
            projector.hasProjectedKeyword(d.state, id, Keyword.INDESTRUCTIBLE) shouldBe true
        }

        // The Moogle tokens have lifelink.
        all.filter {
            d.state.getEntity(it)?.get<CardComponent>()?.name?.contains("Moogle") == true
        }.forEach { id ->
            projector.hasProjectedKeyword(d.state, id, Keyword.LIFELINK) shouldBe true
        }
    }

    test("with no creatures in play, creates no tokens") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val spell = d.putCardInHand(p1, "Moogles' Valor")
        d.giveColorlessMana(p1, 3)
        d.giveMana(p1, Color.WHITE, 2)
        d.castSpell(p1, spell)
        d.bothPass()

        moogleCount(d, p1) shouldBe 0
    }
})
