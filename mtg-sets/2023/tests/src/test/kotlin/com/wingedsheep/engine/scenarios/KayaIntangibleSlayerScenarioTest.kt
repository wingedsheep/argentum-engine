package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.KayaIntangibleSlayer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Kaya, Intangible Slayer (ONE #205).
 *
 * +2 drains each opponent for 3; 0 draws two and lets each opponent scry 1; −3 exiles a creature
 * or enchantment and, unless it was an Aura, gives Kaya's controller a 1/1 white flying Spirit
 * copy of it that keeps its other types.
 */
class KayaIntangibleSlayerScenarioTest : ScenarioTestBase() {

    private val drainAbility = KayaIntangibleSlayer.activatedAbilities[0].id
    private val drawAbility = KayaIntangibleSlayer.activatedAbilities[1].id
    private val exileAbility = KayaIntangibleSlayer.activatedAbilities[2].id

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        // The scenario builder skips the "enters with its starting loyalty" step, so seed it.
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun TestGame.kaya(): EntityId {
        val kaya = findPermanent("Kaya, Intangible Slayer")!!
        seedLoyalty(this, kaya, 6)
        return kaya
    }

    private fun TestGame.minusThree(target: EntityId) {
        execute(
            ActivateAbility(player1Id, kaya(), exileAbility, targets = listOf(ChosenTarget.Permanent(target)))
        ).error shouldBe null
        resolveStack()
    }

    init {
        context("Kaya, Intangible Slayer") {

            test("+2: each opponent loses 3 life and you gain 3 life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kaya, Intangible Slayer")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.execute(ActivateAbility(game.player1Id, game.kaya(), drainAbility)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 23
                game.getLifeTotal(2) shouldBe 17
            }

            test("0: you draw two cards, then the opponent may scry 1 in their own library") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kaya, Intangible Slayer")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val handBefore = game.handSize(1)
                game.execute(ActivateAbility(game.player1Id, game.kaya(), drawAbility)).error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe handBefore + 2

                val ask = game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                ask.playerId shouldBe game.player2Id
                game.answerYesNo(true).error shouldBe null

                val scry = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                scry.playerId shouldBe game.player2Id
                scry.options.size shouldBe 1
                val top = scry.options.single()
                game.state.getLibrary(game.player2Id).first() shouldBe top
                game.selectCards(listOf(top)).error shouldBe null

                withClue("the opponent bottomed their top card") {
                    game.state.getLibrary(game.player2Id).last() shouldBe top
                }
                game.getPendingDecision() shouldBe null
            }

            test("−3 on a creature: exiles it and creates a 1/1 white flying Spirit copy with its other types") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kaya, Intangible Slayer")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val giant = game.findPermanent("Hill Giant")!!
                game.minusThree(giant)

                game.isInExile(2, "Hill Giant") shouldBe true
                val token = game.findPermanent("Hill Giant").shouldNotBeNull()
                game.state.getEntity(token)!!.has<TokenComponent>() shouldBe true
                val projected = game.state.projectedState
                projected.getController(token) shouldBe game.player1Id
                projected.getPower(token) shouldBe 1
                projected.getToughness(token) shouldBe 1
                projected.getColors(token) shouldBe setOf(Color.WHITE.name)
                projected.hasKeyword(token, Keyword.FLYING) shouldBe true
                projected.isCreature(token) shouldBe true
                projected.hasSubtype(token, "Spirit") shouldBe true
                withClue("\"in addition to its other types\" keeps Giant") {
                    projected.hasSubtype(token, "Giant") shouldBe true
                }
            }

            test("−3 on a Clone copying a creature: the token copies the copied creature, not Clone") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kaya, Intangible Slayer")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInHand(1, "Clone")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Clone").error shouldBe null
                game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(listOf(giant)).error shouldBe null
                val clone = game.findPermanents("Hill Giant").single { it != giant }

                game.minusThree(clone)

                game.isInExile(1, "Clone") shouldBe true
                withClue("the token copies Hill Giant and does not get Clone's enter-as-a-copy choice") {
                    game.getPendingDecision() shouldBe null
                    val token = game.findPermanents("Hill Giant").single { it != giant }
                    game.state.getEntity(token)!!.has<TokenComponent>() shouldBe true
                    game.state.projectedState.hasSubtype(token, "Giant") shouldBe true
                    game.state.projectedState.getPower(token) shouldBe 1
                }
            }

            test("−3 on a non-Aura enchantment: the copy is an enchantment creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kaya, Intangible Slayer")
                    .withCardOnBattlefield(2, "Glorious Anthem")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.minusThree(game.findPermanent("Glorious Anthem")!!)

                game.isInExile(2, "Glorious Anthem") shouldBe true
                val token = game.findPermanent("Glorious Anthem").shouldNotBeNull()
                val projected = game.state.projectedState
                projected.getController(token) shouldBe game.player1Id
                projected.isCreature(token) shouldBe true
                projected.hasType(token, "ENCHANTMENT") shouldBe true
                projected.hasSubtype(token, "Spirit") shouldBe true
                withClue("the copied anthem pumps its own controller's creatures, itself included: 1/1 +1/+1") {
                    projected.getPower(token) shouldBe 2
                    projected.getToughness(token) shouldBe 2
                }
            }

            test("−3 on an Aura: exiles it and creates no token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kaya, Intangible Slayer")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(2, "Pacifism", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.minusThree(game.findPermanent("Pacifism")!!)

                game.isInExile(2, "Pacifism") shouldBe true
                game.findPermanent("Pacifism") shouldBe null
                game.state.getBattlefield().count { game.state.getEntity(it)?.has<TokenComponent>() == true } shouldBe 0
            }
        }
    }
}
