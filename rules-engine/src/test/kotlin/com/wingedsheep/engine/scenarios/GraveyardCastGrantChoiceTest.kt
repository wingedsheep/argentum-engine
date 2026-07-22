package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GraveyardCastRiderSelection
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * When more than one `MayCastFromGraveyard` permission applies to the same creature — a free grant
 * with no rider and a rider-bearing grant (The Tomb of Aclazotz: "enters with a finality counter and
 * is a Vampire") — the player picks *which permission* they're casting under before paying costs
 * (CR 601.2b), rather than the engine auto-imposing the rider. The choice is carried on
 * `CastSpell.graveyardCastRider`; the enumerator offers one legal action per distinct permission, and
 * `CastZoneResolver.findMayCastFromGraveyardGrant` honors the selection (with an anti-cheat fallback
 * so a player can't dodge a rider that is the *only* applicable permission).
 *
 * Two inline artifact "benches" stand in for the two permissions so the choice is isolated from the
 * Tomb's transform/activation flow.
 */
class GraveyardCastGrantChoiceTest : FunSpec({

    val projector = StateProjector()

    val graveRat = card("Grave Rat") {
        manaCost = "{1}{B}"
        colorIdentity = "B"
        typeLine = "Creature — Rat"
        power = 1
        toughness = 1
    }
    val freeBench = card("Free Reanimation Bench") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility { ability = MayCastFromGraveyard(GameObjectFilter.Creature) }
    }
    val riderBench = card("Cursed Reanimation Bench") {
        manaCost = "{2}"
        typeLine = "Artifact"
        staticAbility {
            ability = MayCastFromGraveyard(
                filter = GameObjectFilter.Creature,
                entersWithCounter = CounterType.FINALITY,
                addedSubtypeOnEntry = "Vampire",
            )
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(graveRat, freeBench, riderBench))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 40 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    fun giveRatMana(driver: GameTestDriver, player: EntityId) {
        driver.giveMana(player, Color.BLACK, 1)
        driver.giveColorlessMana(player, 1)
    }

    fun finalityCount(driver: GameTestDriver, perm: EntityId): Int =
        driver.state.getEntity(perm)!!.get<CountersComponent>()?.getCount(CounterType.FINALITY) ?: 0

    test("both permissions applying to one creature offer two distinct cast actions (free vs rider)") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Free Reanimation Bench")
        driver.putPermanentOnBattlefield(player, "Cursed Reanimation Bench")
        val rat = driver.putCardInGraveyard(player, "Grave Rat")
        giveRatMana(driver, player)

        val casts = driver.legalActions(player)
            .filter { it.sourceZone == "GRAVEYARD" && (it.action as? CastSpell)?.cardId == rat }
        casts shouldHaveSize 2

        val rider = casts.map { it.action as CastSpell }.single { it.graveyardCastRider?.entersWithCounter == CounterType.FINALITY }
        rider.graveyardCastRider shouldBe GraveyardCastRiderSelection(CounterType.FINALITY, "Vampire")
        casts.map { it.action as CastSpell }.count { it.graveyardCastRider == GraveyardCastRiderSelection() } shouldBe 1
        casts.single { (it.action as CastSpell).graveyardCastRider?.entersWithCounter == CounterType.FINALITY }
            .description shouldContain "finality counter"
    }

    test("casting under the free permission: the creature enters WITHOUT a finality counter or Vampire") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Free Reanimation Bench")
        driver.putPermanentOnBattlefield(player, "Cursed Reanimation Bench")
        val rat = driver.putCardInGraveyard(player, "Grave Rat")
        giveRatMana(driver, player)

        driver.submit(CastSpell(playerId = player, cardId = rat, graveyardCastRider = GraveyardCastRiderSelection()))
            .isSuccess shouldBe true
        driver.bothPass()
        resolveStack(driver)

        val perm = driver.findPermanent(player, "Grave Rat")
        perm shouldNotBe null
        withClue("free permission chosen — no rider imposed") {
            finalityCount(driver, perm!!) shouldBe 0
            projector.project(driver.state).getSubtypes(perm).contains("Vampire") shouldBe false
        }
    }

    test("casting under the rider permission: the creature enters WITH a finality counter and Vampire") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Free Reanimation Bench")
        driver.putPermanentOnBattlefield(player, "Cursed Reanimation Bench")
        val rat = driver.putCardInGraveyard(player, "Grave Rat")
        giveRatMana(driver, player)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = rat,
                graveyardCastRider = GraveyardCastRiderSelection(CounterType.FINALITY, "Vampire"),
            )
        ).isSuccess shouldBe true
        driver.bothPass()
        resolveStack(driver)

        val perm = driver.findPermanent(player, "Grave Rat")!!
        finalityCount(driver, perm) shouldBe 1
        projector.project(driver.state).getSubtypes(perm).contains("Vampire") shouldBe true
    }

    test("anti-cheat: when the rider grant is the only applicable permission, an explicit no-rider choice still gets the rider") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Cursed Reanimation Bench") // rider grant only
        val rat = driver.putCardInGraveyard(player, "Grave Rat")
        giveRatMana(driver, player)

        // Client claims the free (no-rider) permission, but none applies — must not dodge the rider.
        driver.submit(CastSpell(playerId = player, cardId = rat, graveyardCastRider = GraveyardCastRiderSelection()))
            .isSuccess shouldBe true
        driver.bothPass()
        resolveStack(driver)

        val perm = driver.findPermanent(player, "Grave Rat")!!
        finalityCount(driver, perm) shouldBe 1
    }
})
