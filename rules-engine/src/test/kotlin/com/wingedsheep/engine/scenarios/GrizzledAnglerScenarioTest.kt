package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.combat.MustAttackThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.emn.cards.GrizzledAngler
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Grizzled Angler // Grisly Anglerfish (EMN) — the mill-then-conditionally-transform tap ability,
 * and the back face's "creatures your opponents control attack this turn if able".
 *
 * The graveyard check runs *after* the mill, so a colorless creature card milled by this very
 * activation is what flips it; a graveyard with no colorless creature leaves it front face up.
 */
class GrizzledAnglerScenarioTest : FunSpec({

    val projector = StateProjector()

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun faceName(driver: GameTestDriver, id: EntityId): String? =
        driver.state.getEntity(id)?.get<CardComponent>()?.name

    /** Activate the named face's sole activated ability and drain the stack. */
    fun activate(driver: GameTestDriver, player: EntityId, source: EntityId, abilityId: AbilityId) {
        driver.submitSuccess(
            ActivateAbility(playerId = player, sourceId = source, abilityId = abilityId)
        )
        var guard = 0
        while (guard++ < 20 && (driver.state.stack.isNotEmpty() || driver.isPaused)) {
            if (driver.isPaused) driver.autoResolveDecision() else driver.bothPass()
        }
    }

    test("milling no colorless creature card leaves it front face up") {
        val (driver, you) = newGame()
        val angler = driver.putCreatureOnBattlefield(you, "Grizzled Angler")
        driver.removeSummoningSickness(angler)

        activate(driver, you, angler, GrizzledAngler.activatedAbilities.first().id)

        driver.getGraveyard(you).size shouldBe 2 // two Islands
        faceName(driver, angler) shouldBe "Grizzled Angler"
    }

    test("milling a colorless creature card transforms it into Grisly Anglerfish") {
        val (driver, you) = newGame()
        val angler = driver.putCreatureOnBattlefield(you, "Grizzled Angler")
        driver.removeSummoningSickness(angler)

        // Juggernaut is a colorless artifact creature — put it where the mill will find it.
        driver.putCardOnTopOfLibrary(you, "Juggernaut")

        activate(driver, you, angler, GrizzledAngler.activatedAbilities.first().id)

        driver.getGraveyardCardNames(you).contains("Juggernaut") shouldBe true
        faceName(driver, angler) shouldBe "Grisly Anglerfish"

        val projected = projector.project(driver.state)
        projected.getPower(angler) shouldBe 4
        projected.getToughness(angler) shouldBe 5
    }

    test("a colorless creature already in the graveyard doesn't flip it on its own") {
        val (driver, you) = newGame()
        val angler = driver.putCreatureOnBattlefield(you, "Grizzled Angler")
        driver.putCardInGraveyard(you, "Juggernaut")

        // No activation, no flip — the transform only happens while the ability resolves.
        faceName(driver, angler) shouldBe "Grizzled Angler"
    }

    test("the back face's {6} ability forces the opponent's creatures to attack, not yours") {
        val (driver, you) = newGame()
        val opponent = driver.getOpponent(you)
        val angler = driver.putCreatureOnBattlefield(you, "Grizzled Angler")
        driver.removeSummoningSickness(angler)
        driver.putCardOnTopOfLibrary(you, "Juggernaut")
        activate(driver, you, angler, GrizzledAngler.activatedAbilities.first().id)
        faceName(driver, angler) shouldBe "Grisly Anglerfish"

        val theirCourser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val yourCourser = driver.putCreatureOnBattlefield(you, "Centaur Courser")

        driver.giveColorlessMana(you, 6)
        val backAbility = GrizzledAngler.backFace!!.activatedAbilities.first().id
        activate(driver, you, angler, backAbility)

        driver.state.getEntity(theirCourser)?.get<MustAttackThisTurnComponent>() shouldBe
            MustAttackThisTurnComponent
        driver.state.getEntity(yourCourser)?.get<MustAttackThisTurnComponent>() shouldBe null
    }
})
