package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dmu.cards.SerraParagon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Serra Paragon — {2}{W}{W} Creature — Angel 3/4, Flying.
 * "Once during each of your turns, you may play a land from your graveyard or cast a permanent spell
 * with mana value 3 or less from your graveyard. If you do, it gains 'When this permanent is put into
 * a graveyard from the battlefield, exile it and you gain 2 life.'"
 *
 * Pins the one shared allowance (a land play spends it for casts and vice versa), the filter (no
 * four-drop, no instant), and the granted trigger on both a cast creature and a played land — which
 * outlives Paragon itself.
 */
class SerraParagonScenarioTest : FunSpec({

    val bear = card("Paragon Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val bigBeast = card("Paragon Test Beast") {
        manaCost = "{3}{G}"
        typeLine = "Creature — Beast"
        power = 4
        toughness = 4
    }
    val shock = card("Paragon Test Shock") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            effect = Effects.DrawCards(1)
        }
    }
    val ruin = card("Paragon Test Ruin") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell {
            val permanent = target(TargetFilter.Permanent)
            effect = Effects.Destroy(permanent)
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SerraParagon, bear, bigBeast, shock, ruin))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun graveyardActions(driver: GameTestDriver, player: EntityId, cardId: EntityId) =
        driver.legalActions(player).filter {
            it.sourceZone == "GRAVEYARD" &&
                ((it.action as? CastSpell)?.cardId == cardId || (it.action as? PlayLand)?.cardId == cardId)
        }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 40 && driver.state.stack.isNotEmpty()) {
            if (driver.isPaused) driver.autoResolveDecision() else driver.bothPass()
        }
    }

    fun castFromGraveyard(driver: GameTestDriver, player: EntityId, cardId: EntityId) {
        driver.giveMana(player, Color.GREEN, 1)
        driver.giveColorlessMana(player, 1)
        driver.submit(CastSpell(playerId = player, cardId = cardId)).outcome shouldBe Outcome.Done
        resolveStack(driver)
    }

    fun destroy(driver: GameTestDriver, player: EntityId, target: EntityId) {
        val spell = driver.putCardInHand(player, "Paragon Test Ruin")
        driver.giveMana(player, Color.BLACK, 1)
        driver.castSpell(player, spell, listOf(target)).outcome shouldBe Outcome.Done
        resolveStack(driver)
    }

    test("offers a land and a permanent spell of mana value 3 or less — not a four-drop or an instant") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Serra Paragon")
        val forest = driver.putCardInGraveyard(player, "Forest")
        val bearCard = driver.putCardInGraveyard(player, "Paragon Test Bear")
        val beast = driver.putCardInGraveyard(player, "Paragon Test Beast")
        val instant = driver.putCardInGraveyard(player, "Paragon Test Shock")
        driver.giveMana(player, Color.GREEN, 4)
        driver.giveMana(player, Color.RED, 1)

        graveyardActions(driver, player, forest).map { it.action::class } shouldBe listOf(PlayLand::class)
        graveyardActions(driver, player, bearCard).map { it.action::class } shouldBe listOf(CastSpell::class)
        withClue("mana value 4 is over the cap") { graveyardActions(driver, player, beast) shouldHaveSize 0 }
        withClue("an instant is not a permanent spell") { graveyardActions(driver, player, instant) shouldHaveSize 0 }
    }

    test("playing a land from the graveyard spends the turn's allowance for casts too") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Serra Paragon")
        val forest = driver.putCardInGraveyard(player, "Forest")
        val bearCard = driver.putCardInGraveyard(player, "Paragon Test Bear")

        driver.submit(PlayLand(player, forest)).outcome shouldBe Outcome.Done
        driver.findPermanent(player, "Forest") shouldNotBe null

        driver.giveMana(player, Color.GREEN, 1)
        driver.giveColorlessMana(player, 1)
        withClue("once during each of your turns — the land play was the one use") {
            graveyardActions(driver, player, bearCard) shouldHaveSize 0
        }
    }

    test("casting a spell from the graveyard spends the allowance for land plays too") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Serra Paragon")
        val forest = driver.putCardInGraveyard(player, "Forest")
        val bearCard = driver.putCardInGraveyard(player, "Paragon Test Bear")

        castFromGraveyard(driver, player, bearCard)
        driver.findPermanent(player, "Paragon Test Bear") shouldNotBe null

        withClue("the land drop is unused, but Paragon's permission is spent") {
            graveyardActions(driver, player, forest) shouldHaveSize 0
        }
        driver.submit(PlayLand(player, forest)).outcome shouldNotBe Outcome.Done
    }

    test("a creature cast this way keeps the exile-and-gain-life trigger after Paragon is gone") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val paragon = driver.putPermanentOnBattlefield(player, "Serra Paragon")
        val bearCard = driver.putCardInGraveyard(player, "Paragon Test Bear")
        castFromGraveyard(driver, player, bearCard)
        val bearPermanent = driver.findPermanent(player, "Paragon Test Bear")!!

        destroy(driver, player, paragon)
        driver.getGraveyardCardNames(player) shouldContain "Serra Paragon"
        val life = driver.getLifeTotal(player)

        destroy(driver, player, bearPermanent)
        driver.getExile(player).map { driver.getCardName(it) } shouldContain "Paragon Test Bear"
        driver.getGraveyardCardNames(player) shouldNotContain "Paragon Test Bear"
        driver.getLifeTotal(player) shouldBe life + 2
    }

    test("a land played this way gains the trigger too") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Serra Paragon")
        val forest = driver.putCardInGraveyard(player, "Forest")
        driver.submit(PlayLand(player, forest)).outcome shouldBe Outcome.Done
        val life = driver.getLifeTotal(player)

        destroy(driver, player, forest)
        driver.getExile(player) shouldContain forest
        driver.getLifeTotal(player) shouldBe life + 2
    }

    test("a land played from hand gets no trigger and leaves the allowance unspent") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Serra Paragon")
        val handForest = driver.putCardInHand(player, "Forest")
        val bearCard = driver.putCardInGraveyard(player, "Paragon Test Bear")
        driver.submit(PlayLand(player, handForest)).outcome shouldBe Outcome.Done
        val life = driver.getLifeTotal(player)

        destroy(driver, player, handForest)
        driver.getGraveyardCardNames(player) shouldContain "Forest"
        driver.getLifeTotal(player) shouldBe life

        driver.giveMana(player, Color.GREEN, 1)
        driver.giveColorlessMana(player, 1)
        graveyardActions(driver, player, bearCard) shouldHaveSize 1
    }
})
