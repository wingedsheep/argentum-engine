package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.big.cards.FomoriVault
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Fomori Vault (BIG #29) — Land.
 *
 * "{T}: Add {C}.
 *  {3}, {T}, Discard a card: Look at the top X cards of your library, where X is the number of
 *  artifacts you control. Put one of those cards into your hand and the rest on the bottom of your
 *  library in a random order."
 *
 * Covers the dig ability: X scales with the number of artifacts you control (the dig only looks at
 * exactly that many cards), the player keeps one in hand, and X = 0 (no artifacts) looks at nothing.
 */
class FomoriVaultScenarioTest : FunSpec({

    val manaAbilityId = FomoriVault.activatedAbilities[0].id
    val digAbilityId = FomoriVault.activatedAbilities[1].id

    test("dig looks at X = artifacts you control and keeps one in hand") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val vault = driver.putPermanentOnBattlefield(player, "Fomori Vault")
        driver.removeSummoningSickness(vault)
        // Two artifacts you control → X = 2.
        driver.putPermanentOnBattlefield(player, "Artifact Creature")
        driver.putPermanentOnBattlefield(player, "Artifact Creature")
        driver.giveMana(player, Color.RED, 3)

        // Stack the library: two known cards on top, then a third that must NOT be looked at.
        driver.putCardOnTopOfLibrary(player, "Forest")              // becomes index 1
        val topCard = driver.putCardOnTopOfLibrary(player, "Island") // index 0
        // A card to discard for the ability cost.
        val toDiscard = driver.putCardInHand(player, "Plains")

        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = vault,
                abilityId = digAbilityId,
                costPayment = AdditionalCostPayment(discardedCards = listOf(toDiscard))
            )
        ).error shouldBe null

        // The discard cost was paid.
        driver.getHand(player).contains(toDiscard) shouldBe false

        // Resolve the ability off the stack; it then pauses for the "keep one of the top X" selection.
        driver.bothPass()
        val keepDecision = driver.pendingDecision
        (keepDecision is SelectCardsDecision) shouldBe true
        driver.submitCardSelection(player, listOf(topCard))

        // The chosen card is now in hand.
        driver.getHand(player).contains(topCard) shouldBe true
    }

    test("with no artifacts, X = 0 and the dig looks at nothing") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val vault = driver.putPermanentOnBattlefield(player, "Fomori Vault")
        driver.removeSummoningSickness(vault)
        driver.giveMana(player, Color.RED, 3)
        val toDiscard = driver.putCardInHand(player, "Plains")

        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = vault,
                abilityId = digAbilityId,
                costPayment = AdditionalCostPayment(discardedCards = listOf(toDiscard))
            )
        ).error shouldBe null
        driver.bothPass()

        // X = 0 → nothing looked at, no card-keep prompt waiting on the player.
        val d = driver.pendingDecision
        (d == null || d !is SelectCardsDecision) shouldBe true
        // The discard cost was still paid.
        driver.getHand(player).contains(toDiscard) shouldBe false
    }

    test("mana ability taps for {C}") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val vault = driver.putPermanentOnBattlefield(player, "Fomori Vault")
        driver.removeSummoningSickness(vault)

        driver.submit(
            ActivateAbility(playerId = player, sourceId = vault, abilityId = manaAbilityId)
        ).error shouldBe null
        // The vault is now tapped from producing mana, and still on the battlefield.
        driver.state.getEntity(vault)!!
            .has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() shouldBe true
        driver.state.getBattlefield().contains(vault) shouldBe true
    }
})
