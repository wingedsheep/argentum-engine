package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.CardSource
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * `CardSource.SacrificedThisWay` — the cards a sacrifice (cost or same-resolution effect) made, in
 * whatever public zone each went to (graveyard, or exile under a redirect replacement), gathered only
 * while each is still the object the sacrifice made (CR 400.7).
 */
class SacrificedThisWayGatherTest : ScenarioTestBase() {

    private fun returner(name: String, cost: com.wingedsheep.sdk.scripting.AbilityCost) = card(name) {
        manaCost = "{0}"
        typeLine = "Artifact"
        activatedAbility {
            this.cost = cost
            effect = Effects.Pipeline {
                toHand(gather(CardSource.SacrificedThisWay))
            }
        }
    }

    private val sacReturner = returner(
        "Test Sacrifice Returner",
        Costs.SacrificePermanents(GameObjectFilter.Creature, minCount = 0, excludeSelf = true)
    )
    private val exileReturner = returner(
        "Test Exile Returner",
        Costs.ExilePermanents(GameObjectFilter.Creature, minCount = 0, excludeSelf = true)
    )
    private val tapReturner = returner(
        "Test Tap Returner",
        Costs.TapPermanentsVariable(GameObjectFilter.Creature, minCount = 0, excludeSelf = true)
    )
    private val effectReturner = card("Test Effect Returner") {
        manaCost = "{0}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.Pipeline {
                run(Effects.SacrificeAnyNumber(GameObjectFilter.Creature, excludeSource = true))
                toHand(gather(CardSource.SacrificedThisWay))
            }
        }
    }

    private val graveyardExiler = card("Test Graveyard Exiler") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        replacementEffect(
            RedirectZoneChange(
                newDestination = Zone.EXILE,
                appliesTo = EventPattern.ZoneChangeEvent(filter = GameObjectFilter.Any, to = Zone.GRAVEYARD),
            )
        )
    }

    init {
        listOf(sacReturner, exileReturner, tapReturner, effectReturner, graveyardExiler).forEach(cardRegistry::register)

        fun board(artifact: String, token: Boolean = false) = scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(1, artifact)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Hill Giant", isToken = token)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun activate(game: TestGame, artifact: String, picks: List<com.wingedsheep.sdk.model.EntityId>) {
            val source = game.findPermanent(artifact)!!
            val ability = cardRegistry.getCard(artifact)!!.activatedAbilities.single().id
            val result = game.execute(
                ActivateAbility(
                    game.player1Id, source, ability,
                    costPayment = AdditionalCostPayment(variableCostPermanents = picks)
                )
            )
            withClue("${result.error}") { result.error shouldBe null }
            game.resolveStack()
        }

        test("a variable sacrifice cost's cards are gathered from the graveyard") {
            val game = board("Test Sacrifice Returner")
            activate(game, "Test Sacrifice Returner", listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Hill Giant")!!))
            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Hill Giant") shouldBe true
        }

        test("a sacrificed token has ceased to exist and isn't gathered") {
            val game = board("Test Sacrifice Returner", token = true)
            activate(game, "Test Sacrifice Returner", listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Hill Giant")!!))
            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Hill Giant") shouldBe false
        }

        test("a sacrificed card a replacement exiled instead is gathered from exile") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Test Sacrifice Returner")
                .withCardOnBattlefield(1, "Test Graveyard Exiler")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            activate(game, "Test Sacrifice Returner", listOf(game.findPermanent("Grizzly Bears")!!))
            game.isInHand(1, "Grizzly Bears") shouldBe true
        }

        test("a sacrificed card that has left the graveyard since is a new object and isn't gathered") {
            val game = board("Test Sacrifice Returner")
            val bears = game.findPermanent("Grizzly Bears")!!
            val source = game.findPermanent("Test Sacrifice Returner")!!
            val ability = sacReturner.activatedAbilities.single().id
            game.execute(
                ActivateAbility(
                    game.player1Id, source, ability,
                    costPayment = AdditionalCostPayment(variableCostPermanents = listOf(bears))
                )
            ).error shouldBe null
            // With the ability on the stack, the card leaves the graveyard for exile.
            game.state = game.state.moveToZone(
                bears,
                com.wingedsheep.engine.state.ZoneKey(game.player1Id, Zone.GRAVEYARD),
                com.wingedsheep.engine.state.ZoneKey(game.player1Id, Zone.EXILE),
            )
            game.resolveStack()
            game.isInExile(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe false
        }

        test("an exile variable cost sacrifices nothing") {
            val game = board("Test Exile Returner")
            activate(game, "Test Exile Returner", listOf(game.findPermanent("Grizzly Bears")!!))
            game.isInExile(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe false
        }

        test("a tap variable cost sacrifices nothing") {
            val game = board("Test Tap Returner")
            activate(game, "Test Tap Returner", listOf(game.findPermanent("Grizzly Bears")!!))
            (game.findPermanent("Grizzly Bears") != null) shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe false
        }

        test("a sacrifice effect earlier in the same resolution feeds the gather") {
            val game = board("Test Effect Returner")
            val source = game.findPermanent("Test Effect Returner")!!
            val ability = effectReturner.activatedAbilities.single().id
            game.execute(ActivateAbility(game.player1Id, source, ability)).error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(game.findPermanent("Grizzly Bears")!!))
            game.isInHand(1, "Grizzly Bears") shouldBe true
            (game.findPermanent("Hill Giant") != null) shouldBe true
        }
    }
}
