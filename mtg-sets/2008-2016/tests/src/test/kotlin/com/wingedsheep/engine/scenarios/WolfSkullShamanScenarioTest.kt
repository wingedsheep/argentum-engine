package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mor.cards.WolfSkullShaman
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Wolf-Skull Shaman (MOR #140) — the first kinship card.
 *
 *   Kinship — At the beginning of your upkeep, you may look at the top card of your library. If it
 *   shares a creature type with this creature, you may reveal it. If you do, create a 2/2 green
 *   Wolf creature token.
 *
 * Pins the composed kinship shape: the creature-type match is read between the library card and
 * the Shaman, the reveal is optional even on a match, nothing is made without a match, and the
 * looked-at card stays on top of the library either way.
 */
class WolfSkullShamanScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WolfSkullShaman))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        return driver
    }

    fun wolves(driver: GameTestDriver, player: EntityId): Int =
        driver.getCreatures(player).count { id ->
            val entity = driver.state.getEntity(id)
            entity?.get<TokenComponent>() != null &&
                entity.get<CardComponent>()?.typeLine?.subtypes?.any { it.value == "Wolf" } == true
        }

    fun topOfLibrary(driver: GameTestDriver, player: EntityId): EntityId? =
        driver.state.getZone(ZoneKey(player, Zone.LIBRARY)).firstOrNull()

    /**
     * Put the Shaman out with [topCard] on top of your library, advance to your next upkeep and
     * resolve the kinship trigger, answering "look?" with [look] and the reveal pick with [reveal].
     * Returns the top card's id.
     */
    fun runKinship(driver: GameTestDriver, topCard: String, look: Boolean, reveal: Boolean): EntityId {
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(you, "Wolf-Skull Shaman")
        val top = driver.putCardOnTopOfLibrary(you, topCard)

        driver.passPriorityUntil(Step.UPKEEP, you)
        var guard = 0
        while (guard++ < 16 && (driver.state.stack.isNotEmpty() || driver.pendingDecision != null)) {
            when (val decision = driver.pendingDecision) {
                null -> driver.bothPass()
                is YesNoDecision -> driver.submitYesNo(you, look)
                is SelectCardsDecision -> {
                    // Pick the top card only when it is actually offered as eligible.
                    val eligible = top in decision.options && top !in decision.nonSelectableOptions
                    driver.submitCardSelection(you, if (reveal && eligible) listOf(top) else emptyList())
                }
                else -> error("unexpected decision $decision")
            }
        }
        driver.state.step shouldBe Step.UPKEEP
        return top
    }

    test("a top card sharing a creature type can be revealed to create a Wolf") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        // Llanowar Elves is an Elf Druid; the Shaman is an Elf Shaman — they share Elf.
        val top = runKinship(driver, "Llanowar Elves", look = true, reveal = true)

        wolves(driver, you) shouldBe 1
        withClue("the revealed card stays on top of the library") {
            topOfLibrary(driver, you) shouldBe top
        }
    }

    test("the reveal is optional even when the card shares a creature type") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val top = runKinship(driver, "Llanowar Elves", look = true, reveal = false)

        wolves(driver, you) shouldBe 0
        topOfLibrary(driver, you) shouldBe top
    }

    test("a top card sharing no creature type makes nothing") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        // Savannah Lions is a Cat — no type in common with an Elf Shaman, so it can't be revealed.
        val top = runKinship(driver, "Savannah Lions", look = true, reveal = true)

        wolves(driver, you) shouldBe 0
        topOfLibrary(driver, you) shouldBe top
    }

    test("declining to look makes nothing") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val top = runKinship(driver, "Llanowar Elves", look = false, reveal = true)

        wolves(driver, you) shouldBe 0
        topOfLibrary(driver, you) shouldBe top
    }
})
