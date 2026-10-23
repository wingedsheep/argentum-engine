package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `CreateTokenEffect.triggeredAbilitiesOf` — "create a token with all triggered abilities of the
 * exiled cards" (The Book of Vile Darkness) — and the `distinctNames` axis on an exile-permanents
 * cost that the same card pays ("artifacts named Eye of Vecna and Hand of Vecna": one of each).
 *
 * The abilities are part of the token's copiable values as it is created (the Book's ruling), so a
 * gained "when this enters" ability triggers on the token's own entry.
 */
class TokenWithTriggeredAbilitiesOfCardsTest : ScenarioTestBase() {

    private val donorRelic = card("Donor Relic") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "When this artifact enters, you gain 3 life.\nAt the beginning of your upkeep, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.GainLife(3)
        }
        triggeredAbility {
            trigger = Triggers.you.beginningOf(Step.UPKEEP)
            effect = Effects.GainLife(1)
        }
    }

    private val otherRelic = card("Other Relic") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "When this artifact enters, you gain 2 life."
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.GainLife(2)
        }
    }

    private fun avatarMaker(name: String, filter: GameObjectFilter, distinctNames: Boolean) = card(name) {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "{T}, Exile two artifacts: Create a 5/5 Avatar token with all triggered abilities of the exiled cards."
        activatedAbility {
            cost = Costs.Composite(
                Costs.Tap,
                Costs.ExilePermanentsFixed(count = 2, filter = filter.youControl(), distinctNames = distinctNames),
            )
            effect = Effects.Pipeline {
                val exiled = gather(CardSource.ExiledAsCost)
                run(
                    Effects.CreateToken(
                        power = 5,
                        toughness = 5,
                        creatureTypes = setOf("Avatar"),
                        name = "Avatar",
                        triggeredAbilitiesOf = listOf(exiled),
                    )
                )
            }
        }
    }

    private val pairAltar = avatarMaker(
        "Pair Altar",
        GameObjectFilter.Artifact.named("Donor Relic") or GameObjectFilter.Artifact.named("Other Relic"),
        distinctNames = true,
    )
    private val twinAltar = avatarMaker("Twin Altar", GameObjectFilter.Artifact.named("Donor Relic"), distinctNames = false)

    private fun ScenarioTestBase.TestGame.activate(source: EntityId, exiled: List<EntityId>?) =
        execute(
            ActivateAbility(
                player1Id, source,
                cardRegistry.getCard(state.getEntity(source)!!.get<CardComponent>()!!.cardDefinitionId)!!
                    .activatedAbilities.single().id,
                costPayment = exiled?.let { AdditionalCostPayment(exiledCards = it) },
            )
        )

    private fun ScenarioTestBase.TestGame.isActivatable(source: EntityId) =
        getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == source && it.isAffordable }

    init {
        cardRegistry.register(listOf(donorRelic, otherRelic, pairAltar, twinAltar))

        fun board(altar: String, vararg relics: String) = scenario()
            .withPlayers()
            .withCardOnBattlefield(1, altar)
            .apply { relics.forEach { withCardOnBattlefield(1, it) } }
            .apply { repeat(6) { withCardInLibrary(1, "Island") } }
            .apply { repeat(6) { withCardInLibrary(2, "Island") } }
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("the token is created with the exiled cards' triggered abilities, and a gained enters ability triggers on its entry") {
            val game = board("Pair Altar", "Donor Relic", "Other Relic")
            val altar = game.findPermanent("Pair Altar")!!
            game.activate(altar, exiled = null).error shouldBe null
            game.resolveStack()

            val avatar = game.findPermanent("Avatar")
            avatar shouldNotBe null
            game.state.getEntity(avatar!!)!!.get<CardComponent>()!!.copyTriggeredAbilities shouldHaveSize 3
            // Donor Relic's "gain 3" and Other Relic's "gain 2" both triggered as the Avatar entered.
            game.getLifeTotal(1) shouldBe 25
            game.isInExile(1, "Donor Relic") shouldBe true
            game.isInExile(1, "Other Relic") shouldBe true
        }

        test("a gained upkeep ability triggers from the token on later turns") {
            val game = board("Pair Altar", "Donor Relic", "Other Relic")
            game.activate(game.findPermanent("Pair Altar")!!, exiled = null).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 25

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // opponent's upkeep: no trigger
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // our upkeep
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 26
        }

        test("two copies of one card lend two separate abilities, each triggering") {
            val game = board("Twin Altar", "Donor Relic", "Donor Relic")
            game.activate(game.findPermanent("Twin Altar")!!, exiled = null).error shouldBe null
            game.resolveStack()

            val avatar = game.findPermanent("Avatar")!!
            game.state.getEntity(avatar)!!.get<CardComponent>()!!.copyTriggeredAbilities shouldHaveSize 4
            game.getLifeTotal(1) shouldBe 26
        }

        test("a different-names exile cost isn't payable with two objects of one name") {
            val game = board("Pair Altar", "Donor Relic", "Donor Relic")
            val altar = game.findPermanent("Pair Altar")!!
            game.isActivatable(altar) shouldBe false
            (game.activate(altar, game.findPermanents("Donor Relic")).error != null) shouldBe true
            game.findPermanent("Avatar") shouldBe null
        }

        test("a different-names exile cost rejects a selection that repeats a name") {
            val game = board("Pair Altar", "Donor Relic", "Donor Relic", "Other Relic")
            val altar = game.findPermanent("Pair Altar")!!
            game.isActivatable(altar) shouldBe true
            (game.activate(altar, game.findPermanents("Donor Relic")).error != null) shouldBe true
            game.findPermanents("Donor Relic") shouldHaveSize 2

            val pick = listOf(game.findPermanents("Donor Relic").first(), game.findPermanent("Other Relic")!!)
            game.activate(altar, pick).error shouldBe null
            game.resolveStack()
            game.findPermanents("Donor Relic") shouldHaveSize 1
            game.findPermanent("Avatar") shouldNotBe null
        }
    }
}
