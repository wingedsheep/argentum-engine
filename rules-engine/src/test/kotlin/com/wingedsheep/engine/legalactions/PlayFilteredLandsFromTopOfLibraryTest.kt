package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The `landFilter` axis of [PlayLandsAndCastFilteredFromTopOfLibrary] — "you may play *snow* lands
 * from the top of your library" (Isu the Abominable). The enumerator offers a land on top only when
 * it matches, and the play-land handler rejects a non-matching one submitted anyway. The default
 * every-land filter keeps its old behaviour, and two permissions' spell filters combine rather than
 * the first one shadowing the other.
 */
class PlayFilteredLandsFromTopOfLibraryTest : FunSpec({

    val forestsFromTop = card("Forest Scout") {
        manaCost = "{G}"
        typeLine = "Creature — Elf Scout"
        power = 1
        toughness = 1
        staticAbility {
            ability = PlayLandsAndCastFilteredFromTopOfLibrary(
                spellFilter = null,
                landFilter = GameObjectFilter.Land.withSubtype("Forest")
            )
        }
    }

    val anyLandFromTop = card("Land Scout") {
        manaCost = "{G}"
        typeLine = "Creature — Elf Scout"
        power = 1
        toughness = 1
        staticAbility {
            ability = PlayLandsAndCastFilteredFromTopOfLibrary(spellFilter = null)
        }
    }

    val creaturesFromTop = card("Creature Scout") {
        manaCost = "{G}"
        typeLine = "Creature — Elf Scout"
        power = 1
        toughness = 1
        staticAbility {
            ability = PlayLandsAndCastFilteredFromTopOfLibrary(spellFilter = GameObjectFilter.Creature)
        }
    }

    fun driver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(forestsFromTop, anyLandFromTop, creaturesFromTop))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.offersLandPlay(player: EntityId, cardId: EntityId): Boolean =
        legalActions(player).any { (it.action as? PlayLand)?.cardId == cardId && it.sourceZone == "LIBRARY" }

    test("a land matching the land filter is offered and played from the top") {
        val driver = driver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Forest Scout")
        val forest = driver.putCardOnTopOfLibrary(me, "Forest")

        driver.offersLandPlay(me, forest) shouldBe true
        driver.playLand(me, forest).outcome shouldBe Outcome.Done
        driver.findPermanent(me, "Forest") shouldNotBe null
    }

    test("a land outside the land filter is neither offered nor playable") {
        val driver = driver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Forest Scout")
        val island = driver.putCardOnTopOfLibrary(me, "Island")

        driver.offersLandPlay(me, island) shouldBe false
        driver.playLand(me, island).outcome.shouldBeInstanceOf<Outcome.Rejected>()
        driver.findPermanent(me, "Island") shouldBe null
    }

    test("the default land filter still allows any land") {
        val driver = driver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Land Scout")
        val island = driver.putCardOnTopOfLibrary(me, "Island")

        driver.offersLandPlay(me, island) shouldBe true
        driver.playLand(me, island).outcome shouldBe Outcome.Done
    }

    test("a lands-only permission doesn't shadow another permission's spell filter") {
        val driver = driver()
        val me = driver.activePlayer!!
        // The lands-only grant enters first; the creature grant must still be read.
        driver.putCreatureOnBattlefield(me, "Forest Scout")
        driver.putCreatureOnBattlefield(me, "Creature Scout")
        val bears = driver.putCardOnTopOfLibrary(me, "Grizzly Bears")
        driver.giveMana(me, Color.GREEN, 2)

        driver.legalActions(me).any { (it.action as? CastSpell)?.cardId == bears } shouldBe true
    }
})
