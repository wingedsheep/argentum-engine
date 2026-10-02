package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.Mirrex
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mirrex (ONE #254) — Land — Sphere.
 *   {T}: Add {C}.
 *   {T}: Add one mana of any color. Activate only if this land entered this turn.
 *   {3}, {T}: Create a 1/1 colorless Phyrexian Mite artifact creature token with toxic 1 and
 *   "This token can't block."
 */
class MirrexScenarioTest : FunSpec({

    val colorlessAbilityId = Mirrex.activatedAbilities[0].id
    val anyColorAbilityId = Mirrex.activatedAbilities[1].id
    val miteAbilityId = Mirrex.activatedAbilities[2].id

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(Mirrex) + PredefinedTokens.allTokens)
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
    }

    fun GameTestDriver.offersAbility(player: EntityId, source: EntityId, abilityId: AbilityId) =
        legalActions(player).any { val a = it.action; a is ActivateAbility && a.sourceId == source && a.abilityId == abilityId }

    test("the turn it is played, {T} adds one mana of any color") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mirrex = d.putCardInHand(you, "Mirrex")
        d.playLand(you, mirrex)

        d.offersAbility(you, mirrex, anyColorAbilityId) shouldBe true
        d.submitSuccess(
            ActivateAbility(playerId = you, sourceId = mirrex, abilityId = anyColorAbilityId, manaColorChoice = Color.GREEN)
        )

        val pool = d.state.getEntity(you)?.get<ManaPoolComponent>()!!
        pool.getAmount(Color.GREEN) shouldBe 1
        d.isTapped(mirrex) shouldBe true
    }

    test("a Mirrex that didn't enter this turn can't make colored mana, only {C}") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mirrex = d.putCardInHand(you, "Mirrex")
        d.playLand(you, mirrex)
        d.offersAbility(you, mirrex, anyColorAbilityId) shouldBe true

        // Through the opponent's turn and back to ours: Mirrex no longer entered this turn.
        d.passPriorityUntil(Step.UPKEEP)
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.UPKEEP)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe you

        d.offersAbility(you, mirrex, anyColorAbilityId) shouldBe false
        d.submitExpectFailure(
            ActivateAbility(playerId = you, sourceId = mirrex, abilityId = anyColorAbilityId, manaColorChoice = Color.GREEN)
        )
        d.isTapped(mirrex) shouldBe false

        d.submitSuccess(ActivateAbility(playerId = you, sourceId = mirrex, abilityId = colorlessAbilityId))
        val pool = d.state.getEntity(you)?.get<ManaPoolComponent>()!!
        pool.getAmount(Color.GREEN) shouldBe 0
        pool.colorless shouldBe 1
    }

    test("{3}, {T}: creates a Phyrexian Mite token") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mirrex = d.putLandOnBattlefield(you, "Mirrex")
        repeat(3) { d.putLandOnBattlefield(you, "Forest") }

        d.submitSuccess(ActivateAbility(playerId = you, sourceId = mirrex, abilityId = miteAbilityId))
        d.stackSize shouldBe 1
        d.bothPass()
        d.stackSize shouldBe 0

        d.findPermanent(you, "Phyrexian Mite") shouldNotBe null
        d.isTapped(mirrex) shouldBe true
    }
})
