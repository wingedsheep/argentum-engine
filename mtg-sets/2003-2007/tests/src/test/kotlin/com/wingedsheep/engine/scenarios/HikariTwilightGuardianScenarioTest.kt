package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.HikariTwilightGuardian
import com.wingedsheep.mtg.sets.definitions.chk.cards.KamiOfTheWaningMoon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hikari, Twilight Guardian (CHK) — "Whenever you cast a Spirit or Arcane spell, you may exile
 * Hikari. If you do, return it to the battlefield under its owner's control at the beginning of
 * the next end step."
 */
class HikariTwilightGuardianScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + HikariTwilightGuardian + KamiOfTheWaningMoon)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("casting a Spirit lets you exile Hikari, and it returns at the beginning of the next end step") {
        val d = driver()
        val p1 = d.player1
        d.putCreatureOnBattlefield(p1, "Hikari, Twilight Guardian")
        val kami = d.putCardInHand(p1, "Kami of the Waning Moon")
        d.giveMana(p1, Color.BLACK, 3)

        d.castSpell(p1, kami).error shouldBe null
        // Hikari's trigger has no target; Kami of the Waning Moon is not on the battlefield yet.
        d.bothPass() // Hikari's trigger resolves -> may prompt
        d.submitYesNo(p1, true)

        withClue("Hikari is exiled while the Spirit spell is still on the stack") {
            d.findPermanent(p1, "Hikari, Twilight Guardian") shouldBe null
            d.getExileCardNames(p1) shouldContain "Hikari, Twilight Guardian"
            d.getStackSpellNames() shouldBe listOf("Kami of the Waning Moon")
        }

        d.bothPass() // Kami resolves
        d.findPermanent(p1, "Hikari, Twilight Guardian") shouldBe null

        d.passPriorityUntil(Step.END)
        // Resolve the delayed return trigger.
        var guard = 0
        while (d.findPermanent(p1, "Hikari, Twilight Guardian") == null && guard++ < 5) d.bothPass()

        withClue("Hikari is back on the battlefield under its owner's control") {
            d.findPermanent(p1, "Hikari, Twilight Guardian") shouldNotBe null
            d.getExileCardNames(p1) shouldNotContain "Hikari, Twilight Guardian"
        }
    }

    test("declining leaves Hikari on the battlefield") {
        val d = driver()
        val p1 = d.player1
        val hikari = d.putCreatureOnBattlefield(p1, "Hikari, Twilight Guardian")
        val kami = d.putCardInHand(p1, "Kami of the Waning Moon")
        d.giveMana(p1, Color.BLACK, 3)

        d.castSpell(p1, kami).error shouldBe null
        d.bothPass()
        d.submitYesNo(p1, false)

        d.findPermanent(p1, "Hikari, Twilight Guardian") shouldBe hikari
        d.getExileCardNames(p1) shouldNotContain "Hikari, Twilight Guardian"
    }

    test("a spell that is neither Spirit nor Arcane does not trigger it") {
        val d = driver()
        val p1 = d.player1
        val hikari = d.putCreatureOnBattlefield(p1, "Hikari, Twilight Guardian")
        val bears = d.putCardInHand(p1, "Grizzly Bears")
        d.giveMana(p1, Color.GREEN, 2)

        d.castSpell(p1, bears).error shouldBe null
        d.pendingDecision shouldBe null
        d.getStackSpellNames() shouldBe listOf("Grizzly Bears")
        d.findPermanent(p1, "Hikari, Twilight Guardian") shouldBe hikari
    }
})
