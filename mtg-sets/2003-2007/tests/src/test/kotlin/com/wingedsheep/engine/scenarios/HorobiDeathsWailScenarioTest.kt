package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.HorobiDeathsWail
import com.wingedsheep.mtg.sets.definitions.lea.cards.IcyManipulator
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Horobi, Death's Wail (CHK #117) — "Flying. Whenever a creature becomes the target of a spell or
 * ability, destroy that creature."
 *
 * The trigger subject is *any* creature: either player's, Horobi itself included, and the
 * targeting source may be a spell or an ability, controlled by anyone.
 */
class HorobiDeathsWailScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + HorobiDeathsWail + IcyManipulator)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun resolveStack(d: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && d.state.stack.isNotEmpty() && !d.isPaused) d.bothPass()
    }

    test("an opponent's creature targeted by its own controller's spell is destroyed and the spell fizzles") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Horobi, Death's Wail")
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")

        d.passPriority(d.player1)
        val growth = d.putCardInHand(d.player2, "Giant Growth")
        d.giveMana(d.player2, Color.GREEN, 1)
        d.castSpell(d.player2, growth, listOf(bears)).outcome shouldBe Outcome.Done
        resolveStack(d)

        withClue("Horobi destroys the targeted creature, whoever controls it or the spell") {
            d.findPermanent(d.player2, "Grizzly Bears") shouldBe null
            d.getGraveyardCardNames(d.player2) shouldContain "Grizzly Bears"
        }
        withClue("Giant Growth lost its only target and did not resolve, but still went to the graveyard") {
            d.getGraveyardCardNames(d.player2) shouldContain "Giant Growth"
        }
    }

    test("Horobi's controller targeting their own creature loses it too") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Horobi, Death's Wail")
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")

        val growth = d.putCardInHand(d.player1, "Giant Growth")
        d.giveMana(d.player1, Color.GREEN, 1)
        d.castSpell(d.player1, growth, listOf(bears)).outcome shouldBe Outcome.Done
        resolveStack(d)

        d.findPermanent(d.player1, "Grizzly Bears") shouldBe null
    }

    test("Horobi itself becoming a target is destroyed") {
        val d = driver()
        val horobi = d.putCreatureOnBattlefield(d.player1, "Horobi, Death's Wail")

        d.passPriority(d.player1)
        val growth = d.putCardInHand(d.player2, "Giant Growth")
        d.giveMana(d.player2, Color.GREEN, 1)
        d.castSpell(d.player2, growth, listOf(horobi)).outcome shouldBe Outcome.Done
        resolveStack(d)

        d.findPermanent(d.player1, "Horobi, Death's Wail") shouldBe null
        d.getGraveyardCardNames(d.player1) shouldContain "Horobi, Death's Wail"
    }

    test("an activated ability's target counts too, not only spells") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Horobi, Death's Wail")
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val manipulator = d.putPermanentOnBattlefield(d.player1, "Icy Manipulator")
        d.giveColorlessMana(d.player1, 1)

        d.submit(
            ActivateAbility(
                d.player1,
                manipulator,
                IcyManipulator.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Permanent(bears))
            )
        ).outcome shouldBe Outcome.Done
        resolveStack(d)

        d.findPermanent(d.player2, "Grizzly Bears") shouldBe null
    }

    test("a non-creature permanent becoming a target is left alone") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Horobi, Death's Wail")
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val manipulator = d.putPermanentOnBattlefield(d.player1, "Icy Manipulator")
        val otherManipulator = d.putPermanentOnBattlefield(d.player2, "Icy Manipulator")
        d.giveColorlessMana(d.player1, 1)

        d.submit(
            ActivateAbility(
                d.player1,
                manipulator,
                IcyManipulator.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Permanent(otherManipulator))
            )
        ).outcome shouldBe Outcome.Done
        resolveStack(d)

        withClue("the targeted artifact is not a creature, so it survives") {
            d.getPermanents(d.player2) shouldContain otherManipulator
        }
        d.getPermanents(d.player2) shouldContain bears
        d.getGraveyardCardNames(d.player2) shouldNotContain "Icy Manipulator"
    }
})
