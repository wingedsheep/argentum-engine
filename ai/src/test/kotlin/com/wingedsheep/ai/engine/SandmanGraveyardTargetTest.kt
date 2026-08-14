package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Regression: the AI must be able to activate Sandman, Shifting Scoundrel's graveyard ability
 * ("{3}{G}{G}: Return this card and target land card from your graveyard to the battlefield
 * tapped"). The ability surfaces a single-target requirement with `targetZone = null` (only
 * multi-requirement abilities populate `targetZone`), so the AI's target builder must fall back
 * to authoritative game state and produce a `ChosenTarget.Card(... GRAVEYARD)` — not a
 * `ChosenTarget.Permanent`, which the engine rejects, leaving the AI unable to reanimate.
 */
class SandmanGraveyardTargetTest : FunSpec({

    fun createRegistry(): CardRegistry {
        val registry = CardRegistry()
        registry.register(TestCards.all)
        for (set in MtgSetCatalog.all) {
            registry.register(set.cards)
            registry.register(set.basicLands)
        }
        return registry
    }

    test("AI activates Sandman's graveyard ability with a correctly-zoned card target") {
        val registry = createRegistry()
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.player1

        val sandman = driver.putCardInGraveyard(player, "Sandman, Shifting Scoundrel")
        val land = driver.putCardInGraveyard(player, "Forest")
        driver.giveMana(player, Color.GREEN, 5) // {3}{G}{G}

        val ai = AIPlayer.create(registry, player)
        val action = ai.chooseAction(driver.state)

        // The AI chose to reanimate — and it built the right kind of target.
        (action is ActivateAbility) shouldBe true
        val activation = action as ActivateAbility
        activation.sourceId shouldBe sandman
        activation.targets shouldBe listOf(ChosenTarget.Card(land, ownerId = player, zone = Zone.GRAVEYARD))

        // The action the AI produced is actually legal — the engine accepts it (before the fix
        // it was a ChosenTarget.Permanent and failed with "invalid target").
        driver.submitSuccess(action)
    }
})
