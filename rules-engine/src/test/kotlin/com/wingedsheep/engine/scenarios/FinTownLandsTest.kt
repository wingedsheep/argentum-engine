package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.BaronAirshipKingdom
import com.wingedsheep.mtg.sets.definitions.fin.cards.GohnTownOfRuin
import com.wingedsheep.mtg.sets.definitions.fin.cards.GongagaReactorTown
import com.wingedsheep.mtg.sets.definitions.fin.cards.GuadosalamFarplaneGateway
import com.wingedsheep.mtg.sets.definitions.fin.cards.InsomniaCrownCity
import com.wingedsheep.mtg.sets.definitions.fin.cards.RabanastreRoyalCity
import com.wingedsheep.mtg.sets.definitions.fin.cards.SharlayanNationOfScholars
import com.wingedsheep.mtg.sets.definitions.fin.cards.TrenoDarkCity
import com.wingedsheep.mtg.sets.definitions.fin.cards.VectorImperialCapital
import com.wingedsheep.mtg.sets.definitions.fin.cards.WindurstFederationCenter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * FIN "Town" dual taplands. Each is a Land — Town that enters tapped and has two separate
 * tap-for-one-color mana abilities (`activatedAbilities[0]` and `[1]`). This test verifies,
 * for every land:
 *   - it enters tapped when played from hand, and
 *   - each of its two mana abilities adds one mana of the correct color.
 */
class FinTownLandsTest : FunSpec({

    fun poolColor(pool: ManaPoolComponent, color: Color): Int = when (color) {
        Color.WHITE -> pool.white
        Color.BLUE -> pool.blue
        Color.BLACK -> pool.black
        Color.RED -> pool.red
        Color.GREEN -> pool.green
        else -> pool.colorless
    }

    // (card, name, ability[0] color, ability[1] color) — colors in declared ability order.
    val towns: List<Quadruple> = listOf(
        Quadruple(BaronAirshipKingdom, "Baron, Airship Kingdom", Color.BLUE, Color.RED),
        Quadruple(GohnTownOfRuin, "Gohn, Town of Ruin", Color.BLACK, Color.GREEN),
        Quadruple(GongagaReactorTown, "Gongaga, Reactor Town", Color.RED, Color.GREEN),
        Quadruple(GuadosalamFarplaneGateway, "Guadosalam, Farplane Gateway", Color.GREEN, Color.BLUE),
        Quadruple(InsomniaCrownCity, "Insomnia, Crown City", Color.WHITE, Color.BLACK),
        Quadruple(RabanastreRoyalCity, "Rabanastre, Royal City", Color.RED, Color.WHITE),
        Quadruple(SharlayanNationOfScholars, "Sharlayan, Nation of Scholars", Color.WHITE, Color.BLUE),
        Quadruple(TrenoDarkCity, "Treno, Dark City", Color.BLUE, Color.BLACK),
        Quadruple(VectorImperialCapital, "Vector, Imperial Capital", Color.BLACK, Color.RED),
        Quadruple(WindurstFederationCenter, "Windurst, Federation Center", Color.GREEN, Color.WHITE),
    )

    for ((card, name, color0, color1) in towns) {
        fun createDriver(): GameTestDriver {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all)
            driver.registerCard(card)
            return driver
        }

        test("$name enters tapped when played from hand") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
            val p1 = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val land = driver.putCardInHand(p1, name)
            driver.playLand(p1, land).isSuccess shouldBe true

            driver.state.getEntity(land)?.has<TappedComponent>() shouldBe true
        }

        test("$name taps for ${color0.name} (ability 0)") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
            val p1 = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val land = driver.putPermanentOnBattlefield(p1, name)
            driver.untapPermanent(land)
            val ability = card.activatedAbilities[0].id
            driver.submit(ActivateAbility(playerId = p1, sourceId = land, abilityId = ability)).isSuccess shouldBe true

            val pool = driver.state.getEntity(p1)?.get<ManaPoolComponent>()!!
            poolColor(pool, color0) shouldBe 1
        }

        test("$name taps for ${color1.name} (ability 1)") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
            val p1 = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val land = driver.putPermanentOnBattlefield(p1, name)
            driver.untapPermanent(land)
            val ability = card.activatedAbilities[1].id
            driver.submit(ActivateAbility(playerId = p1, sourceId = land, abilityId = ability)).isSuccess shouldBe true

            val pool = driver.state.getEntity(p1)?.get<ManaPoolComponent>()!!
            poolColor(pool, color1) shouldBe 1
        }
    }
})

private data class Quadruple(
    val card: CardDefinition,
    val name: String,
    val color0: Color,
    val color1: Color,
)
