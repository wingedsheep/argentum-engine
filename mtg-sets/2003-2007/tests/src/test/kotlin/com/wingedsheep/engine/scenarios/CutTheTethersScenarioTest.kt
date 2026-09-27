package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.CutTheTethers
import com.wingedsheep.mtg.sets.definitions.chk.cards.WanderingOnes
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Cut the Tethers — {2}{U}{U} Sorcery
 * "For each Spirit, return it to its owner's hand unless that player pays {3}."
 *
 * The per-Spirit toll is charged to the Spirit's *owner*; non-Spirits are untouched; a player who
 * can't pay is never asked and the Spirit simply goes home.
 */
class CutTheTethersScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(CutTheTethers)
        driver.registerCard(WanderingOnes)
        return driver
    }

    fun handNames(driver: GameTestDriver, player: com.wingedsheep.sdk.model.EntityId) =
        driver.getHand(player).mapNotNull { driver.getCardName(it) }

    test("a player who pays keeps their Spirit, one who can't pay has it bounced, non-Spirits stay") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20), startingLife = 20)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(caster, "Wandering Ones")
        driver.putCreatureOnBattlefield(caster, "Grizzly Bears")
        driver.putCreatureOnBattlefield(opponent, "Wandering Ones")

        val tethers = driver.putCardInHand(caster, "Cut the Tethers")
        driver.giveMana(caster, Color.BLUE, 4)
        driver.castSpell(caster, tethers).outcome shouldBe Outcome.Done
        // Float the opponent's toll only after the caster's pool has been spent on the spell.
        driver.giveMana(opponent, Color.BLUE, 3)
        driver.bothPass()

        // The caster's pool is empty, so their own Spirit returns with no prompt; the opponent
        // can afford {3} and is asked about theirs.
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe opponent
        driver.submitYesNo(opponent, true).error shouldBe null

        driver.pendingDecision shouldBe null
        driver.findPermanent(caster, "Wandering Ones") shouldBe null
        handNames(driver, caster) shouldContain "Wandering Ones"
        driver.findPermanent(caster, "Grizzly Bears") shouldNotBe null
        driver.findPermanent(opponent, "Wandering Ones") shouldNotBe null
        handNames(driver, opponent) shouldNotContain "Wandering Ones"
    }

    test("the Spirit's owner, not its controller, is the one asked to pay") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20), startingLife = 20)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Owned by the opponent, but the caster has stolen it.
        val spirit = driver.putCreatureOnBattlefield(opponent, "Wandering Ones")
        driver.replaceState(driver.state.updateEntity(spirit) { c -> c.with(ControllerComponent(caster)) })

        val tethers = driver.putCardInHand(caster, "Cut the Tethers")
        driver.giveMana(caster, Color.BLUE, 4)
        driver.castSpell(caster, tethers).outcome shouldBe Outcome.Done
        // Both players could pay; only the owner may be offered the choice.
        driver.giveMana(caster, Color.BLUE, 3)
        driver.giveMana(opponent, Color.BLUE, 3)
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe opponent
        driver.submitYesNo(opponent, false).error shouldBe null

        driver.pendingDecision shouldBe null
        driver.state.getBattlefield().contains(spirit) shouldBe false
        handNames(driver, opponent) shouldContain "Wandering Ones"
        handNames(driver, caster) shouldNotContain "Wandering Ones"
    }
})
