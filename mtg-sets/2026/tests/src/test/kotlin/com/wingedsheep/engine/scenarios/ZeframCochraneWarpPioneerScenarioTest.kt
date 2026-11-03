package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.trk.cards.ZeframCochraneWarpPioneer
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Zefram Cochrane, Warp Pioneer (TRK #90) — {2}{U} 2/3.
 *
 * "Once during each of your turns, you may cast an artifact spell from the top of your library."
 * The permission is a [com.wingedsheep.sdk.scripting.ConditionalStaticAbility] gated on your turn;
 * legal-action enumeration must unwrap it (it used to drop conditional grants entirely) and must
 * not offer the cast on an opponent's turn, even for a flash artifact.
 */
class ZeframCochraneWarpPioneerScenarioTest : FunSpec({

    val zefram = "Zefram Cochrane, Warp Pioneer"

    val flashTrinket = CardDefinition.artifact(
        name = "Zefram Test Flash Trinket",
        manaCost = ManaCost.ZERO,
    ).copy(keywords = setOf(Keyword.FLASH))
    val secondTrinket = CardDefinition.artifact(name = "Zefram Second Trinket", manaCost = ManaCost.ZERO)
    val shuttle = CardDefinition.artifact(
        name = "Zefram Test Shuttle",
        manaCost = ManaCost.ZERO,
        subtypes = setOf(Subtype("Spacecraft")),
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ZeframCochraneWarpPioneer, flashTrinket, secondTrinket, shuttle))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.canCastFromLibrary(player: EntityId, cardName: String): Boolean =
        legalActions(player).any {
            it.actionType == "CastSpell" && it.sourceZone == "LIBRARY" && it.description.contains(cardName)
        }

    test("casts one artifact spell from the top on your turn, then the permission is spent") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putCreatureOnBattlefield(you, zefram)

        val first = driver.putCardOnTopOfLibrary(you, "Zefram Second Trinket")
        driver.canCastFromLibrary(you, "Zefram Second Trinket") shouldBe true
        driver.castSpell(you, first).error shouldBe null
        driver.bothPass()

        driver.putCardOnTopOfLibrary(you, "Zefram Test Flash Trinket")
        driver.canCastFromLibrary(you, "Zefram Test Flash Trinket") shouldBe false
    }

    test("does not offer a nonartifact spell from the top") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putCreatureOnBattlefield(you, zefram)
        driver.putCardOnTopOfLibrary(you, "Grizzly Bears")

        driver.canCastFromLibrary(you, "Grizzly Bears") shouldBe false
    }

    test("does not let you cast a flash artifact from the top during an opponent's turn") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.putCreatureOnBattlefield(you, zefram)

        driver.passPriorityUntil(Step.UPKEEP, opponent)
        driver.passPriority(opponent).error shouldBe null
        driver.priorityPlayer shouldBe you
        val onTop = driver.putCardOnTopOfLibrary(you, "Zefram Test Flash Trinket")

        // Control: you hold priority and could cast a flash artifact from hand at this speed.
        driver.putCardInHand(you, "Zefram Test Flash Trinket")
        driver.legalActions(you).any { it.actionType == "CastSpell" && it.sourceZone != "LIBRARY" } shouldBe true
        driver.canCastFromLibrary(you, "Zefram Test Flash Trinket") shouldBe false
        driver.castSpell(you, onTop).error shouldBe "Card is not in your hand"
    }

    test("Spacecraft you control enter with two additional charge counters") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putCreatureOnBattlefield(you, zefram)
        val ship = driver.putCardInHand(you, "Zefram Test Shuttle")
        driver.castSpell(you, ship).error shouldBe null
        driver.bothPass()

        driver.state.getEntity(ship)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) shouldBe 2
    }
})
