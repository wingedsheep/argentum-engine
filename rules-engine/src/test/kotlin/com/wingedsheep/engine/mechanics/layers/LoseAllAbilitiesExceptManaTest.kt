package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `LoseAllAbilities(exceptManaAbilities = true)` — "all lands lose all abilities except mana
 * abilities" (Blood Sun). The affected object keeps its mana abilities (CR 605.1a) and loses every
 * other ability; a full "loses all abilities" alongside it, in either order, takes the mana
 * abilities too. A land entering under it has already lost "enters tapped" and its enters trigger
 * (CR 614.12), whether it is played or put onto the battlefield by an effect.
 */
class LoseAllAbilitiesExceptManaTest : ScenarioTestBase() {

    private val sun = card("Test Dusk Sun") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        staticAbility { ability = LoseAllAbilities(GroupFilter.AllLands, exceptManaAbilities = true) }
    }

    private val land = card("Test Dusk Land") {
        typeLine = "Land"
        replacementEffect(EntersTapped())
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.GainLife(2)
        }
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddColorlessMana(1)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.GainLife(1)
        }
    }

    private val choiceLand = card("Test Dusk Choice Land") {
        typeLine = "Land"
        replacementEffect(EntersWithChoice(ChoiceType.COLOR))
        replacementEffect(EntersWithCounters(CounterType.CHARGE, count = 2, selfOnly = true))
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddColorlessMana(1)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }

    private val blank = card("Test Land Blank") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Permanent)
            effect = Effects.RemoveAllAbilities(t)
        }
    }

    private val reanimateLand = card("Test Land Return") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.CardInGraveyard)
            effect = Effects.PutOntoBattlefield(t)
        }
    }

    init {
        cardRegistry.register(sun)
        cardRegistry.register(land)
        cardRegistry.register(blank)
        cardRegistry.register(choiceLand)
        cardRegistry.register(reanimateLand)

        val manaAbility = { cardRegistry.getCard("Test Dusk Land")!!.script.activatedAbilities.first { it.isManaAbility } }
        val lifeAbility = { cardRegistry.getCard("Test Dusk Land")!!.script.activatedAbilities.first { !it.isManaAbility } }

        fun baseGame() = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Test Dusk Sun")
            .withCardOnBattlefield(1, "Test Dusk Land")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("a land keeps its mana ability and loses its other activated ability") {
            val game = baseGame().build()
            val landId = game.findPermanent("Test Dusk Land")!!
            val projected = game.state.projectedState
            projected.hasLostAllAbilities(landId) shouldBe true
            projected.hasLostManaAbilities(landId) shouldBe false

            withClue("the non-mana ability is neither offered nor accepted") {
                game.getLegalActions(1).any {
                    (it.action as? ActivateAbility)?.let { a -> a.sourceId == landId && a.abilityId == lifeAbility().id } == true
                } shouldBe false
                game.execute(ActivateAbility(game.player1Id, landId, lifeAbility().id)).error shouldNotBe null
            }
            withClue("the mana ability still activates") {
                game.getLegalActions(1).any {
                    (it.action as? ActivateAbility)?.let { a -> a.sourceId == landId && a.abilityId == manaAbility().id } == true
                } shouldBe true
                game.execute(ActivateAbility(game.player1Id, landId, manaAbility().id)).error shouldBe null
                game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.colorless shouldBe 1
            }
        }

        test("a full ability removal on top of it takes the mana ability too") {
            val game = baseGame().withCardInHand(1, "Test Land Blank").build()
            val landId = game.findPermanent("Test Dusk Land")!!
            game.castSpell(1, "Test Land Blank", landId).error shouldBe null
            game.resolveStack()

            game.state.projectedState.hasLostManaAbilities(landId) shouldBe true
            game.execute(ActivateAbility(game.player1Id, landId, manaAbility().id)).error shouldNotBe null
        }

        test("a full removal that came first still wins once the except-mana static arrives") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Test Dusk Land")
                .withCardInHand(1, "Test Land Blank")
                .withCardInHand(1, "Test Dusk Sun")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val landId = game.findPermanent("Test Dusk Land")!!
            game.castSpell(1, "Test Land Blank", landId).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Test Dusk Sun").error shouldBe null
            game.resolveStack()

            game.state.projectedState.hasLostManaAbilities(landId) shouldBe true
        }

        test("a land played under it enters untapped and its enters trigger never fires") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Test Dusk Sun")
                .withCardInHand(1, "Test Dusk Land")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.execute(PlayLand(game.player1Id, game.findCardsInHand(1, "Test Dusk Land").single())).error shouldBe null
            game.resolveStack()

            val landId = game.findPermanent("Test Dusk Land")!!
            game.state.getEntity(landId)!!.has<TappedComponent>() shouldBe false
            game.getLifeTotal(1) shouldBe 20
        }

        test("a land put onto the battlefield by an effect under it enters untapped") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Test Dusk Sun")
                .withCardInGraveyard(1, "Test Dusk Land")
                .withCardInHand(1, "Test Land Return")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val inGraveyard = game.state.getGraveyard(game.player1Id).single {
                game.state.getEntity(it)!!.get<com.wingedsheep.engine.state.components.identity.CardComponent>()!!.name == "Test Dusk Land"
            }
            game.castSpellTargetingGraveyardCard(1, "Test Land Return", listOf(inGraveyard)).error shouldBe null
            game.resolveStack()

            val landId = game.findPermanent("Test Dusk Land")!!
            game.state.getEntity(landId)!!.has<TappedComponent>() shouldBe false
            game.getLifeTotal(1) shouldBe 20
        }

        test("a land put onto the battlefield under it neither asks its as-enters choice nor gets its counters") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Test Dusk Sun")
                .withCardInGraveyard(1, "Test Dusk Choice Land")
                .withCardInHand(1, "Test Land Return")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val inGraveyard = game.state.getGraveyard(game.player1Id).single()
            game.castSpellTargetingGraveyardCard(1, "Test Land Return", listOf(inGraveyard)).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            val landId = game.findPermanent("Test Dusk Choice Land")!!
            (game.state.getEntity(landId)!!.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0) shouldBe 0
        }

        test("without the static, the same land enters tapped and triggers") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Test Dusk Land")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.execute(PlayLand(game.player1Id, game.findCardsInHand(1, "Test Dusk Land").single())).error shouldBe null
            game.resolveStack()

            game.state.getEntity(game.findPermanent("Test Dusk Land")!!)!!.has<TappedComponent>() shouldBe true
            game.getLifeTotal(1) shouldBe 22
        }

        test("nonland permanents are untouched") {
            val game = baseGame().build()
            val sunId = game.findPermanent("Test Dusk Sun")!!
            game.state.projectedState.hasLostAllAbilities(sunId) shouldBe false
        }
    }
}
