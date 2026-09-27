package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.BloodthirstyOgre
import com.wingedsheep.mtg.sets.definitions.chk.cards.GutwrencherOni
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Bloodthirsty Ogre (CHK #104) — "{T}: Put a devotion counter on this creature. {T}: Target creature
 * gets -X/-X until end of turn, where X is the number of devotion counters on this creature.
 * Activate only if you control a Demon."
 */
class BloodthirstyOgreScenarioTest : FunSpec({

    val devote = BloodthirstyOgre.activatedAbilities[0].id
    val shrink = BloodthirstyOgre.activatedAbilities[1].id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + BloodthirstyOgre + GutwrencherOni)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.devotion(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.DEVOTION) ?: 0

    fun GameTestDriver.ogre(): EntityId =
        putCreatureOnBattlefield(player1, "Bloodthirsty Ogre").also { removeSummoningSickness(it) }

    test("the first ability adds a devotion counter") {
        val d = driver()
        val ogre = d.ogre()

        d.submit(ActivateAbility(d.player1, ogre, devote)).outcome shouldBe Outcome.Done
        d.bothPass()

        d.devotion(ogre) shouldBe 1
        d.isTapped(ogre) shouldBe true
    }

    test("with a Demon, the second ability shrinks the target by the devotion count") {
        val d = driver()
        val opponent = d.getOpponent(d.player1)
        val ogre = d.ogre()
        d.putCreatureOnBattlefield(d.player1, "Gutwrencher Oni")
        val bears = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Two turns' worth of devotion, then untap to spend it.
        d.submit(ActivateAbility(d.player1, ogre, devote)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.untapPermanent(ogre)
        d.submit(ActivateAbility(d.player1, ogre, devote)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.untapPermanent(ogre)
        d.devotion(ogre) shouldBe 2

        d.submit(
            ActivateAbility(d.player1, ogre, shrink, targets = listOf(ChosenTarget.Permanent(bears)))
        ).outcome shouldBe Outcome.Done
        d.bothPass()

        withClue("two devotion counters make -2/-2, killing the 2/2") {
            d.findPermanent(opponent, "Grizzly Bears") shouldBe null
        }
        withClue("the counters are read, not spent") {
            d.devotion(ogre) shouldBe 2
        }
    }

    test("without a Demon the second ability can't be activated") {
        val d = driver()
        val opponent = d.getOpponent(d.player1)
        val ogre = d.ogre()
        val bears = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        d.submit(
            ActivateAbility(d.player1, ogre, shrink, targets = listOf(ChosenTarget.Permanent(bears)))
        ).outcome shouldNotBe Outcome.Done
        d.isTapped(ogre) shouldBe false
    }
})
