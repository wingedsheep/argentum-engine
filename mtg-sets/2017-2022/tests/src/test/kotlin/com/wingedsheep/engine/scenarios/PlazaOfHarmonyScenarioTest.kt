package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.gtc.cards.GruulGuildgate
import com.wingedsheep.mtg.sets.definitions.rna.cards.PlazaOfHarmony
import com.wingedsheep.mtg.sets.definitions.rtr.cards.AzoriusGuildgate
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Plaza of Harmony (RNA #254).
 *
 *  When this land enters, if you control two or more Gates, you gain 3 life.
 *  {T}: Add {C}.
 *  {T}: Add one mana of any type that a Gate you control could produce.
 *
 * The second mana ability reads only Gates you control: a Forest or an opponent's Gate doesn't widen
 * it, and with no Gate it makes nothing. The life gain is an intervening "if" on two Gates.
 */
class PlazaOfHarmonyScenarioTest : FunSpec({

    val colorlessAbility = PlazaOfHarmony.activatedAbilities[0].id
    val gateAbility = PlazaOfHarmony.activatedAbilities[1].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(PlazaOfHarmony)
        driver.registerCard(AzoriusGuildgate)
        driver.registerCard(GruulGuildgate)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.pool(player: EntityId): ManaPoolComponent =
        state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    fun GameTestDriver.tapForGate(player: EntityId, plaza: EntityId, color: Color) =
        submit(ActivateAbility(player, plaza, gateAbility, manaColorChoice = color))

    test("a Gate you control lets it make that Gate's colors") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putLandOnBattlefield(me, "Azorius Guildgate")
        val plaza = driver.putLandOnBattlefield(me, "Plaza of Harmony")

        driver.tapForGate(me, plaza, Color.BLUE).outcome shouldBe Outcome.Done
        driver.pool(me).blue shouldBe 1
    }

    test("colors only a non-Gate land or an opponent's Gate could make aren't available") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putLandOnBattlefield(me, "Azorius Guildgate")
        driver.putLandOnBattlefield(me, "Forest")
        driver.putLandOnBattlefield(opponent, "Gruul Guildgate")
        val plaza = driver.putLandOnBattlefield(me, "Plaza of Harmony")

        driver.submit(ActivateAbility(me, plaza, gateAbility))
        val choice = driver.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
        withClue("green comes only from my Forest (not a Gate) and the opponent's Gruul Guildgate") {
            choice.availableColors shouldBe setOf(Color.WHITE, Color.BLUE)
        }
        driver.submitDecision(me, ColorChosenResponse(choice.id, Color.WHITE)).error shouldBe null
        driver.pool(me).white shouldBe 1
    }

    test("its own {C} ability always works, even with no Gate") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val plaza = driver.putLandOnBattlefield(me, "Plaza of Harmony")

        driver.submit(ActivateAbility(me, plaza, colorlessAbility)).outcome shouldBe Outcome.Done
        driver.pool(me).colorless shouldBe 1
    }

    test("entering with two Gates you control gains 3 life") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putLandOnBattlefield(me, "Azorius Guildgate")
        driver.putLandOnBattlefield(me, "Gruul Guildgate")
        val plaza = driver.putCardInHand(me, "Plaza of Harmony")

        driver.playLand(me, plaza).error shouldBe null
        driver.bothPass()
        driver.getLifeTotal(me) shouldBe 23
    }

    test("entering with only one Gate gains nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putLandOnBattlefield(me, "Azorius Guildgate")
        driver.putLandOnBattlefield(opponent, "Gruul Guildgate")
        val plaza = driver.putCardInHand(me, "Plaza of Harmony")

        driver.playLand(me, plaza).error shouldBe null
        driver.bothPass()
        withClue("the opponent's Gate doesn't count toward two Gates you control") {
            driver.getLifeTotal(me) shouldBe 20
        }
    }
})
