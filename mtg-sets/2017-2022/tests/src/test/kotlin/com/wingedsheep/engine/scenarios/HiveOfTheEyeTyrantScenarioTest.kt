package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.HiveOfTheEyeTyrant
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Hive of the Eye Tyrant (AFR #258) — Land.
 *
 * "{3}{B}: Until end of turn, this land becomes a 3/3 black Beholder creature with menace and
 *  'Whenever this creature attacks, exile target card from defending player's graveyard.'
 *  It's still a land."
 *
 * Pins the runtime-granted, *targeted* attack trigger: it fires off the animated land, may only
 * target a card in the defending player's graveyard (never the attacker's own), and exiles it.
 */
class HiveOfTheEyeTyrantScenarioTest : FunSpec({
    fun createDriver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.registerCard(HiveOfTheEyeTyrant)
        it.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.animate(player: EntityId, hive: EntityId) {
        giveMana(player, Color.BLACK, 4)
        submitSuccess(ActivateAbility(player, hive, HiveOfTheEyeTyrant.activatedAbilities[1].id))
        bothPass()
    }

    test("animated, it is a 3/3 black Beholder land with menace") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val hive = driver.putLandOnBattlefield(player, "Hive of the Eye Tyrant")

        driver.animate(player, hive)
        val projected = driver.state.projectedState
        projected.hasType(hive, "LAND") shouldBe true
        projected.hasType(hive, "CREATURE") shouldBe true
        projected.hasSubtype(hive, "Beholder") shouldBe true
        projected.getPower(hive) shouldBe 3
        projected.getToughness(hive) shouldBe 3
        projected.getColors(hive) shouldBe setOf("BLACK")
        projected.hasKeyword(hive, Keyword.MENACE) shouldBe true
    }

    test("attacking exiles target card from the defending player's graveyard, not its controller's") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val hive = driver.putLandOnBattlefield(player, "Hive of the Eye Tyrant")
        driver.removeSummoningSickness(hive)
        val theirs = driver.putCardInGraveyard(opponent, "Lightning Bolt")
        val mine = driver.putCardInGraveyard(player, "Lightning Bolt")

        driver.animate(player, hive)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(hive), opponent)

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseTargetsDecision>()
        decision.legalTargets[0]!! shouldContainExactly listOf(theirs)
        decision.legalTargets[0]!! shouldNotContain mine

        driver.submitTargetSelection(player, listOf(theirs))
        driver.bothPass()

        driver.getExile(opponent) shouldContain theirs
        driver.getGraveyard(player) shouldContain mine
    }

    test("the animation and its menace end at end of turn") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val hive = driver.putLandOnBattlefield(player, "Hive of the Eye Tyrant")

        driver.animate(player, hive)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, opponent)

        val projected = driver.state.projectedState
        projected.hasType(hive, "CREATURE") shouldBe false
        projected.hasKeyword(hive, Keyword.MENACE) shouldBe false
    }
})
