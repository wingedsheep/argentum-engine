package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ForetellCard
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Mystic Reflection (KHM #69) — {1}{U} Instant. "Choose target nonlegendary creature. The next time
 * one or more creatures or planeswalkers enter this turn, they enter as copies of the chosen
 * creature." Foretell {U}.
 *
 * The granted copy replacement's rules are pinned in `GrantedEntryCopyReplacementTest`; this covers
 * the card: cast from hand on an opponent's creature, the caster's next creature enters as a copy of
 * it, and the one after enters as itself; foretold, it is cast for {U} on a later turn.
 */
class MysticReflectionScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.names(player: EntityId) =
        getCreatures(player).mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }.sorted()

    fun GameTestDriver.castCreature(player: EntityId, name: String) {
        val card = putCardInHand(player, name)
        giveMana(player, Color.GREEN, 3)
        castSpell(player, card).error shouldBe null
        bothPass()
    }

    test("cast from hand, the next creature you cast enters as a copy of the chosen creature") {
        val d = driver()
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        val giant = d.putCreatureOnBattlefield(opp, "Hill Giant")
        val reflection = d.putCardInHand(p, "Mystic Reflection")
        d.giveMana(p, Color.BLUE, 2)
        d.castSpell(p, reflection, listOf(giant)).error shouldBe null
        d.bothPass()

        d.castCreature(p, "Grizzly Bears")
        withClue("the Bears entered as a copy of the opponent's Hill Giant") {
            d.names(p) shouldBe listOf("Hill Giant")
        }
        d.state.grantedReplacementEffects.shouldBeEmpty()

        d.castCreature(p, "Grizzly Bears")
        d.names(p) shouldBe listOf("Grizzly Bears", "Hill Giant")
    }

    test("foretold, it is cast for its foretell cost on a later turn") {
        val d = driver()
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        val reflection = d.putCardInHand(p, "Mystic Reflection")
        d.giveMana(p, Color.BLUE, 2)
        d.submitSuccess(ForetellCard(p, reflection))

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        val giant = d.putCreatureOnBattlefield(opp, "Hill Giant")
        d.passPriority(opp)
        d.giveMana(p, Color.BLUE, 1)
        d.castSpell(p, reflection, listOf(giant)).error shouldBe null
        d.bothPass()

        // The opponent's creature, entering on their own turn, becomes the copy.
        val bears = d.putCardInHand(opp, "Grizzly Bears")
        d.giveMana(opp, Color.GREEN, 2)
        d.castSpell(opp, bears).error shouldBe null
        d.bothPass()
        d.names(opp) shouldBe listOf("Hill Giant", "Hill Giant")
    }
})
