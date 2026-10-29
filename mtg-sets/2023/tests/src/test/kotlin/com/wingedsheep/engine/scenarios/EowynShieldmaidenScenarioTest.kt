package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.avr.cards.Cloudshift
import com.wingedsheep.mtg.sets.definitions.ltc.cards.EowynShieldmaiden
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Éowyn, Shieldmaiden — "At the beginning of combat on your turn, if another Human entered the
 * battlefield under your control this turn, create two 2/2 red Human Knight creature tokens with
 * trample and haste. Then if you control six or more Humans, draw a card."
 *
 * "Another" excludes Éowyn's own entry, and the intervening "if" means no Human entry → no
 * trigger, so no draw even with six Humans (ruling).
 */
class EowynShieldmaidenScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EowynShieldmaiden, Cloudshift))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun cast(driver: GameTestDriver, player: EntityId, name: String, vararg mana: Pair<Color, Int>) {
        val card = driver.putCardInHand(player, name)
        mana.forEach { (color, n) -> driver.giveMana(player, color, n) }
        driver.castSpell(player, card)
        driver.bothPass()
    }

    fun knights(driver: GameTestDriver, player: EntityId): List<EntityId> =
        driver.getCreatures(player).filter {
            val card = driver.state.getEntity(it)?.get<CardComponent>()
            card?.name != "First Strike Knight" && card?.typeLine?.subtypes?.map { s -> s.value }
                ?.containsAll(listOf("Human", "Knight")) == true &&
                card.name != "Éowyn, Shieldmaiden"
        }

    fun toCombat(driver: GameTestDriver) {
        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        if (driver.state.stack.isNotEmpty()) driver.bothPass()
    }

    test("another Human entering this turn makes two trample, haste Human Knights") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Éowyn, Shieldmaiden")
        cast(driver, me, "Blade of the Ninth Watch", Color.RED to 2)
        val handBefore = driver.getHandSize(me)

        toCombat(driver)

        val tokens = knights(driver, me)
        tokens.size shouldBe 2
        tokens.forEach {
            driver.state.projectedState.hasKeyword(it, Keyword.TRAMPLE) shouldBe true
            driver.state.projectedState.hasKeyword(it, Keyword.HASTE) shouldBe true
        }
        // Four Humans (Éowyn, Blade, two tokens) — short of six, no draw.
        driver.getHandSize(me) shouldBe handBefore
    }

    test("Éowyn's own entry is not another Human") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        cast(driver, me, "Éowyn, Shieldmaiden", Color.BLUE to 1, Color.RED to 1, Color.WHITE to 3)

        toCombat(driver)

        knights(driver, me).size shouldBe 0
    }

    test("Éowyn blinked this turn: her earlier entry was another object and counts (CR 400.7)") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        cast(driver, me, "Éowyn, Shieldmaiden", Color.BLUE to 1, Color.RED to 1, Color.WHITE to 3)
        val eowyn = driver.getCreatures(me).first {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Éowyn, Shieldmaiden"
        }
        val shift = driver.putCardInHand(me, "Cloudshift")
        driver.giveMana(me, Color.WHITE, 1)
        driver.castSpell(me, shift, listOf(eowyn))
        driver.bothPass()
        // Same entity id, new entry — so the count must tell the two entries apart by timestamp.
        driver.getCreatures(me).contains(eowyn) shouldBe true

        toCombat(driver)

        knights(driver, me).size shouldBe 2
    }

    test("a non-Human entering does not trigger it") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Éowyn, Shieldmaiden")
        cast(driver, me, "Grizzly Bears", Color.GREEN to 2)

        toCombat(driver)

        knights(driver, me).size shouldBe 0
    }

    test("six or more Humans after the tokens draws a card") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Éowyn, Shieldmaiden")
        driver.putCreatureOnBattlefield(me, "First Strike Knight")
        driver.putCreatureOnBattlefield(me, "Banding Scout")
        cast(driver, me, "Blade of the Ninth Watch", Color.RED to 2)
        val handBefore = driver.getHandSize(me)

        toCombat(driver)

        knights(driver, me).size shouldBe 2
        // Éowyn, First Strike Knight, Banding Scout, Blade, two tokens = six Humans.
        driver.getHandSize(me) shouldBe handBefore + 1
    }
})
