package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.atq.cards.AshnodsAltar
import com.wingedsheep.mtg.sets.definitions.iko.cards.KinnanBonderProdigy
import com.wingedsheep.mtg.sets.definitions.inv.cards.PhyrexianAltar
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.basicLand
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Kinnan, Bonder Prodigy (IKO #192).
 *
 * The mirror static must fire for a nonland permanent you tap for mana (a mana dork) and not for a
 * land or for a mana ability without {T} in its cost (Ashnod's Altar, Phyrexian Altar's color-choice
 * path, per the card's ruling) — a Treasure's {T}-and-sacrifice ability does count — and the dig must
 * let only a non-Human creature card onto the battlefield.
 */
class KinnanBonderProdigyScenarioTest : FunSpec({

    val TestForest = basicLand("Forest") {}

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TestForest, KinnanBonderProdigy, AshnodsAltar, PhyrexianAltar, PredefinedTokens.Treasure))
        driver.initMirrorMatch(Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.greenInPool(player: EntityId): Int =
        state.getEntity(player)?.get<ManaPoolComponent>()?.green ?: 0

    test("tapping a nonland mana creature adds one extra mana of the type it produced") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Kinnan, Bonder Prodigy")
        val elves = driver.putCreatureOnBattlefield(me, "Llanowar Elves")
        driver.removeSummoningSickness(elves)

        val result = driver.submit(
            ActivateAbility(me, elves, TestCards.LlanowarElves.activatedAbilities[0].id)
        )
        withClue("activation error: ${result.error}") { result.error shouldBe null }
        driver.greenInPool(me) shouldBe 2
    }

    test("a Treasure sacrificed as part of its own {T} mana ability still gets the bonus") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Kinnan, Bonder Prodigy")
        val treasure = driver.putPermanentOnBattlefield(me, "Treasure")

        val result = driver.submit(
            ActivateAbility(
                me, treasure, PredefinedTokens.Treasure.activatedAbilities.single().id,
                manaColorChoice = Color.GREEN,
            )
        )
        withClue("activation error: ${result.error}") { result.error shouldBe null }
        driver.greenInPool(me) shouldBe 2
    }

    test("tapping a land for mana gets no bonus") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Kinnan, Bonder Prodigy")
        val forest = driver.putPermanentOnBattlefield(me, "Forest")

        val result = driver.submit(ActivateAbility(me, forest, TestForest.activatedAbilities[0].id))
        withClue("activation error: ${result.error}") { result.error shouldBe null }
        driver.greenInPool(me) shouldBe 1
    }

    test("a nonland mana ability without {T} in its cost gets no bonus") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Kinnan, Bonder Prodigy")
        val altar = driver.putPermanentOnBattlefield(me, "Ashnod's Altar")
        val elves = driver.putCreatureOnBattlefield(me, "Llanowar Elves")

        val result = driver.submit(
            ActivateAbility(
                me, altar, AshnodsAltar.activatedAbilities[0].id,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(elves))
            )
        )
        withClue("activation error: ${result.error}") { result.error shouldBe null }
        driver.state.getEntity(me)?.get<ManaPoolComponent>()?.colorless shouldBe 2
    }

    test("a non-{T} mana ability that pauses for a color choice gets no bonus either") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Kinnan, Bonder Prodigy")
        val altar = driver.putPermanentOnBattlefield(me, "Phyrexian Altar")
        val elves = driver.putCreatureOnBattlefield(me, "Llanowar Elves")

        val result = driver.submit(
            ActivateAbility(
                me, altar, PhyrexianAltar.activatedAbilities[0].id,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(elves))
            )
        )
        withClue("activation error: ${result.error}") { result.error shouldBe null }
        val decision = driver.pendingDecision as ChooseColorDecision
        driver.submitDecision(me, ColorChosenResponse(decision.id, Color.GREEN))
        driver.greenInPool(me) shouldBe 1
    }

    test("the dig puts a non-Human creature onto the battlefield; Humans are not selectable") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val kinnan = driver.putCreatureOnBattlefield(me, "Kinnan, Bonder Prodigy")

        val human = driver.putCardOnTopOfLibrary(me, "Blade of the Ninth Watch")
        val centaur = driver.putCardOnTopOfLibrary(me, "Centaur Courser")

        driver.giveColorlessMana(me, 5)
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveMana(me, Color.BLUE, 1)
        val activation = driver.submit(
            ActivateAbility(me, kinnan, KinnanBonderProdigy.activatedAbilities[0].id)
        )
        withClue("activation error: ${activation.error}") { activation.error shouldBe null }
        driver.bothPass()

        val decision = driver.pendingDecision as SelectCardsDecision
        decision.options shouldContain centaur
        decision.options shouldNotContain human
        driver.submitCardSelection(me, listOf(centaur)).outcome shouldBe Outcome.Done

        driver.state.getZone(ZoneKey(me, Zone.BATTLEFIELD)) shouldContain centaur
        driver.state.getZone(ZoneKey(me, Zone.BATTLEFIELD)) shouldNotContain human
    }
})
