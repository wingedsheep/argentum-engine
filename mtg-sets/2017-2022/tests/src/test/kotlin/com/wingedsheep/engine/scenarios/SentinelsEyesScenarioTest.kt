package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Sentinel's Eyes (THB #36).
 *
 * "Enchant creature / Enchanted creature gets +1/+1 and has vigilance. / Escape—{W}, Exile two other
 * cards from your graveyard."
 *
 * Escape on an Aura: cast from the graveyard (still targeting, as any Aura spell does) for {W} plus
 * two other cards, it enters attached to the chosen creature and grants +1/+1 and vigilance. Unlike
 * an escaped sorcery it stays on the battlefield, and returns to the graveyard (not exile) when the
 * creature dies — ready to escape again.
 */
class SentinelsEyesScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        return driver to driver.activePlayer!!
    }

    fun escape(player: EntityId, eyes: EntityId, creature: EntityId, fodder: List<EntityId>) = CastSpell(
        playerId = player,
        cardId = eyes,
        targets = listOf(ChosenTarget.Permanent(creature)),
        useAlternativeCost = true,
        alternativeCostType = AlternativeCostType.ESCAPE,
        additionalCostPayment = AdditionalCostPayment(exiledCards = fodder),
        paymentStrategy = PaymentStrategy.FromPool
    )

    test("escapes onto a creature for {W} and two other cards, granting +1/+1 and vigilance") {
        val (driver, player) = setup()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val eyes = driver.putCardInGraveyard(player, "Sentinel's Eyes")
        val fodder = (1..2).map { driver.putCardInGraveyard(player, "Plains") }
        driver.giveMana(player, Color.WHITE, 1)

        val result = driver.submit(escape(player, eyes, bears, fodder))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.bothPass()

        driver.state.getEntity(eyes)?.get<AttachedToComponent>()?.targetId shouldBe bears
        driver.getExile(player) shouldContainAll fodder
        driver.getGraveyard(player) shouldNotContain eyes
        driver.getExile(player) shouldNotContain eyes

        driver.state.projectedState.getPower(bears) shouldBe 3
        driver.state.projectedState.getToughness(bears) shouldBe 3
        driver.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
    }

    test("one other card can't pay the escape cost") {
        val (driver, player) = setup()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val eyes = driver.putCardInGraveyard(player, "Sentinel's Eyes")
        val fodder = listOf(driver.putCardInGraveyard(player, "Plains"))
        driver.giveMana(player, Color.WHITE, 1)

        driver.submitExpectFailure(escape(player, eyes, bears, fodder))
        driver.getGraveyard(player) shouldContain eyes
        driver.state.projectedState.getPower(bears) shouldBe 2
    }
})
