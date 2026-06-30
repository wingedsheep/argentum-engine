package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.AvatarTheLastAirbenderSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Earthen Ally (TLA) — {G} Creature — Human Soldier Ally 0/2
 *
 *  - "This creature gets +1/+0 for each color among Allies you control." Earthen Ally is itself a
 *    green Ally, so the count is at least 1 (its own green) when it's the only Ally; a multicolored
 *    Ally contributes each of its colors to the distinct set.
 *  - "{2}{W}{U}{B}{R}{G}: Earthbend 5." — an instant-speed activated ability that animates a target
 *    land you control into a 0/0 creature-land with haste and five +1/+1 counters (so a 5/5).
 */
class EarthenAllyScenarioTest : FunSpec({

    // Mono-white Ally — adds white to the distinct-color tally.
    val whiteAlly = card("White Ally") {
        manaCost = "{W}"
        typeLine = "Creature — Human Soldier Ally"
        power = 1
        toughness = 1
    }

    // Multicolored (blue-black) Ally — proves a single Ally contributes BOTH of its colors.
    val blueBlackAlly = card("Tide Ally") {
        manaCost = "{U}{B}"
        typeLine = "Creature — Merfolk Ally"
        power = 1
        toughness = 1
    }

    // A mono-red NON-Ally — its red must NOT count toward "colors among Allies".
    val redNonAlly = card("Red Soldier") {
        manaCost = "{R}"
        typeLine = "Creature — Human Soldier"
        power = 1
        toughness = 1
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + AvatarTheLastAirbenderSet.cards)
        driver.registerCard(whiteAlly)
        driver.registerCard(blueBlackAlly)
        driver.registerCard(redNonAlly)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("alone, Earthen Ally counts its own green — a 1/2") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val earthen = driver.putCreatureOnBattlefield(me, "Earthen Ally")

        // 1 color among Allies (its own green) -> +1/+0 over base 0/2.
        driver.state.projectedState.getPower(earthen) shouldBe 1
        driver.state.projectedState.getToughness(earthen) shouldBe 2
    }

    test("power scales with the distinct colors among Allies you control") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val earthen = driver.putCreatureOnBattlefield(me, "Earthen Ally")

        // Add a white Ally: {G, W} -> 2 colors -> 2/2.
        driver.putCreatureOnBattlefield(me, "White Ally")
        driver.state.projectedState.getPower(earthen) shouldBe 2

        // Add a blue-black Ally: {G, W, U, B} -> 4 colors -> 4/2 (the one Ally added BOTH U and B).
        driver.putCreatureOnBattlefield(me, "Tide Ally")
        driver.state.projectedState.getPower(earthen) shouldBe 4
        driver.state.projectedState.getToughness(earthen) shouldBe 2

        // A non-Ally red creature must not contribute red — count stays at 4.
        driver.putCreatureOnBattlefield(me, "Red Soldier")
        driver.state.projectedState.getPower(earthen) shouldBe 4
    }

    test("Earthbend 5 animates a target land into a 5/5 creature-land with haste") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        driver.putCreatureOnBattlefield(me, "Earthen Ally")
        val forest = driver.putLandOnBattlefield(me, "Forest")

        // {2}{W}{U}{B}{R}{G}
        driver.giveColorlessMana(me, 2)
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveMana(me, Color.RED, 1)
        driver.giveMana(me, Color.GREEN, 1)

        val earthbendId = driver.cardRegistry.requireCard("Earthen Ally").activatedAbilities[0].id
        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = driver.getCreatures(me).first { driver.getCardName(it) == "Earthen Ally" },
                abilityId = earthbendId,
                targets = listOf(ChosenTarget.Permanent(forest))
            )
        )
        driver.bothPass() // resolve Earthbend

        val projected = driver.state.projectedState
        projected.hasType(forest, "LAND") shouldBe true
        projected.hasType(forest, "CREATURE") shouldBe true
        // 0/0 base + five +1/+1 counters.
        projected.getPower(forest) shouldBe 5
        projected.getToughness(forest) shouldBe 5
        projected.hasKeyword(forest, Keyword.HASTE) shouldBe true
    }
})
