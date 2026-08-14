package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.soi.cards.WestvaleAbbey
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Westvale Abbey // Ormendahl, Profane Prince (SOI) — the land that eats five creatures.
 *
 * The interesting seam is the flip ability's cost/effect pairing: `{T}` is part of the activation
 * cost, so the land is tapped by the time the ability resolves, and "then untap it" has to leave
 * Ormendahl untapped — which, with haste, is what lets him attack the turn he arrives.
 */
class WestvaleAbbeyScenarioTest : FunSpec({

    val projector = StateProjector()

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(WestvaleAbbey)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun faceName(driver: GameTestDriver, id: EntityId): String? =
        driver.state.getEntity(id)?.get<CardComponent>()?.name

    /** Activate one of the Abbey's abilities, auto-answering any cost decisions, then drain. */
    fun activate(driver: GameTestDriver, player: EntityId, source: EntityId, abilityId: AbilityId) {
        driver.submitSuccess(
            ActivateAbility(playerId = player, sourceId = source, abilityId = abilityId)
        )
        var guard = 0
        while (guard++ < 30 && (driver.state.stack.isNotEmpty() || driver.isPaused)) {
            if (driver.isPaused) driver.autoResolveDecision() else driver.bothPass()
        }
    }

    // [0] = {T}: Add {C}; [1] = the Cleric maker; [2] = the flip.
    val abilities = WestvaleAbbey.activatedAbilities

    test("the Cleric ability costs 1 life and makes a 1/1") {
        val (driver, you) = newGame()
        val abbey = driver.putPermanentOnBattlefield(you, "Westvale Abbey")
        driver.giveColorlessMana(you, 5)

        val before = driver.getCreatures(you).size
        activate(driver, you, abbey, abilities[1].id)

        driver.getCreatures(you).size shouldBe before + 1
        driver.getLifeTotal(you) shouldBe 19
    }

    test("sacrificing five creatures transforms the land and untaps it") {
        val (driver, you) = newGame()
        val abbey = driver.putPermanentOnBattlefield(you, "Westvale Abbey")
        repeat(5) { driver.putCreatureOnBattlefield(you, "Grizzly Bears") }
        driver.giveColorlessMana(you, 5)

        activate(driver, you, abbey, abilities[2].id)

        faceName(driver, abbey) shouldBe "Ormendahl, Profane Prince"
        driver.getCreatures(you).size shouldBe 1 // the five Bears are gone; Ormendahl is the creature
        driver.isTapped(abbey) shouldBe false

        val projected = projector.project(driver.state)
        projected.getPower(abbey) shouldBe 9
        projected.getToughness(abbey) shouldBe 7
        projected.hasKeyword(abbey, Keyword.FLYING) shouldBe true
        projected.hasKeyword(abbey, Keyword.LIFELINK) shouldBe true
        projected.hasKeyword(abbey, Keyword.INDESTRUCTIBLE) shouldBe true
        projected.hasKeyword(abbey, Keyword.HASTE) shouldBe true
    }

    test("the flip ability isn't available without five creatures to sacrifice") {
        val (driver, you) = newGame()
        val abbey = driver.putPermanentOnBattlefield(you, "Westvale Abbey")
        repeat(4) { driver.putCreatureOnBattlefield(you, "Grizzly Bears") }
        driver.giveColorlessMana(you, 5)

        driver.submit(
            ActivateAbility(playerId = you, sourceId = abbey, abilityId = abilities[2].id)
        ).error shouldNotBe null
        faceName(driver, abbey) shouldBe "Westvale Abbey"
    }
})
