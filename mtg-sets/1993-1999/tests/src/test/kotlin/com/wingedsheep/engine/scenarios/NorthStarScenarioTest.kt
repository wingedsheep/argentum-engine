package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.leg.cards.NorthStar
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * North Star (LEG) — "{4}, {T}: For one spell this turn, you may spend mana as though it were mana
 * of any type to pay that spell's mana cost. (Additional costs are still paid normally.)"
 *
 * A Mountains-only player casts Grizzly Bears ({1}{G}) off North Star; the rider is spent by that
 * spell, so a second Bears can't be cast the same way.
 */
class NorthStarScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("a green spell is cast with red mana, once") {
        val d = driver()
        val p1 = d.activePlayer!!
        val star = d.putPermanentOnBattlefield(p1, "North Star")
        repeat(4) { d.putPermanentOnBattlefield(p1, "Mountain") }
        val bears = d.putCardInHand(p1, "Grizzly Bears")
        val secondBears = d.putCardInHand(p1, "Grizzly Bears")

        d.submit(
            ActivateAbility(playerId = p1, sourceId = star, abilityId = NorthStar.script.activatedAbilities[0].id)
        ).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.pendingAnyManaTypeSpells shouldHaveSize 1

        withClue("North Star's {4} tapped the four Mountains; two more pay for the Bears") {
            repeat(2) { d.putPermanentOnBattlefield(p1, "Mountain") }
            d.submit(CastSpell(p1, bears)).outcome shouldBe Outcome.Done
        }
        d.bothPass()
        d.findPermanent(p1, "Grizzly Bears") shouldNotBe null
        d.state.pendingAnyManaTypeSpells.shouldBeEmpty()

        repeat(2) { d.putPermanentOnBattlefield(p1, "Mountain") }
        d.submit(CastSpell(p1, secondBears)).outcome shouldNotBe Outcome.Done
    }
})
