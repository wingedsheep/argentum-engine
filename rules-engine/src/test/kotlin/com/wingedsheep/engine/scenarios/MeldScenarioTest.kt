package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.MeldedComponent
import com.wingedsheep.engine.state.components.identity.copiableCardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Meld (CR 701.42) through inline test cards: a host whose ability melds it with a partner into a
 * result that declares the pair (`meldOf`).
 *
 * Pins: both are exiled and come back as one permanent with the result's characteristics
 * (CR 701.42a); "you both own and control" gates the whole thing; a pair that isn't the result's
 * meld pair, or a token, stays in exile (CR 701.42b/c); the mana value is the sum of the front faces
 * (CR 712.8g); leaving the battlefield is one permanent leaving and two cards arriving (CR 712.21);
 * a melded permanent can't be transformed (CR 712.4c) and isn't a "transformed permanent"
 * (CR 701.27g); "it enters tapped and attacking".
 */
class MeldScenarioTest : ScenarioTestBase() {

    private val host = card("Test Meld Host") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Human"
        power = 2
        toughness = 2
        activatedAbility {
            cost = Costs.Free
            effect = Effects.Meld(Filters.Creature.named("Test Meld Partner"), into = "Test Meld Result")
        }
        activatedAbility {
            cost = Costs.Free
            effect = Effects.Meld(Filters.Creature.named("Test Meld Partner"), into = "Test Wrong Meld Result")
        }
        activatedAbility {
            cost = Costs.Free
            effect = Effects.Meld(
                Filters.Creature.named("Test Meld Partner"), into = "Test Meld Result", tappedAndAttacking = true
            )
        }
    }

    private val partner = card("Test Meld Partner") {
        manaCost = "{3}"
        typeLine = "Artifact Creature — Golem"
        power = 1
        toughness = 1
    }

    private val result = card("Test Meld Result") {
        manaCost = ""
        meldOf("Test Meld Host", "Test Meld Partner")
        typeLine = "Legendary Creature — Avatar"
        power = 7
        toughness = 7
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.GainLife(3)
        }
    }

    private val wrongResult = card("Test Wrong Meld Result") {
        manaCost = ""
        meldOf("Test Meld Host", "Test Somebody Else")
        typeLine = "Legendary Creature — Avatar"
        power = 5
        toughness = 5
    }

    private val deathWatcher = card("Test Meld Death Watcher") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
            effect = Effects.GainLife(1)
        }
    }

    private val destroy = card("Test Meld Destroy") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Destroy(t)
        }
    }

    private val bounce = card("Test Meld Bounce") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.ReturnToHand(t)
        }
    }

    init {
        listOf(host, partner, result, wrongResult, deathWatcher, destroy, bounce).forEach(cardRegistry::register)

        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Test Meld Host")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        fun TestGame.meld(ability: Int = 0) {
            val hostId = findPermanent("Test Meld Host").shouldNotBeNull()
            val abilityId = host.script.activatedAbilities[ability].id
            execute(ActivateAbility(player1Id, hostId, abilityId)).error shouldBe null
            resolveStack()
        }

        fun TestGame.exile(player: EntityId) = state.getZone(ZoneKey(player, Zone.EXILE))
            .map { state.getEntity(it)!!.get<CardComponent>()!!.name }

        test("melds the pair into the result: one permanent, both cards out of exile, enters triggers fire") {
            val game = board().withCardOnBattlefield(1, "Test Meld Partner").build()
            val hostId = game.findPermanent("Test Meld Host")!!
            val partnerId = game.findPermanent("Test Meld Partner")!!

            game.meld()

            val melded = game.findPermanent("Test Meld Result").shouldNotBeNull()
            withClue("the host's entity is the melded permanent") { melded shouldBe hostId }
            game.findPermanent("Test Meld Partner") shouldBe null
            game.findPermanent("Test Meld Host") shouldBe null
            withClue("neither card is left in exile") { game.exile(game.player1Id) shouldBe emptyList() }
            game.state.getEntity(melded)!!.get<MeldedComponent>()!!.partnerId shouldBe partnerId
            game.state.projectedState.getPower(melded) shouldBe 7
            withClue("the result entered the battlefield as a new object") { game.getLifeTotal(1) shouldBe 23 }
        }

        test("CR 712.8g: the melded permanent's mana value is the sum of the front faces'") {
            val game = board().withCardOnBattlefield(1, "Test Meld Partner").build()
            game.meld()
            val melded = game.findPermanent("Test Meld Result")!!
            game.state.getEntity(melded)!!.get<CardComponent>()!!.manaValue shouldBe 5
        }

        test("CR 712.8g: a copy of a melded permanent has mana value 0") {
            val game = board().withCardOnBattlefield(1, "Test Meld Partner").build()
            game.meld()
            val melded = game.findPermanent("Test Meld Result")!!
            game.state.getEntity(melded)!!.copiableCardComponent()!!.manaValue shouldBe 0
        }

        test("CR 701.42b: with a token copy and the real partner, the real card is the one melded") {
            val game = board()
                .withCardOnBattlefield(1, "Test Meld Partner", isToken = true)
                .withCardOnBattlefield(1, "Test Meld Partner")
                .build()
            game.meld()
            game.findPermanent("Test Meld Result").shouldNotBeNull()
            withClue("the token is untouched") { game.findPermanent("Test Meld Partner").shouldNotBeNull() }
        }

        test("CR 712.21: a melded permanent that became a copy still leaves as its two front faces") {
            val game = board()
                .withCardOnBattlefield(1, "Test Meld Partner")
                .withCardInHand(1, "Test Meld Destroy")
                .build()
            game.meld()
            val melded = game.findPermanent("Test Meld Result")!!
            // A "becomes a copy" effect snapshots the meld result as the permanent's original identity.
            val resultCard = game.state.getEntity(melded)!!.get<CardComponent>()!!
            game.state = game.state.updateEntity(melded) {
                it.with(CopyOfComponent(resultCard.cardDefinitionId, "Test Meld Partner", resultCard))
            }

            game.castSpell(1, "Test Meld Destroy", targetId = melded).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Test Meld Host") shouldBe true
            game.isInGraveyard(1, "Test Meld Partner") shouldBe true
            game.isInGraveyard(1, "Test Meld Result") shouldBe false
        }

        test("nothing happens without a partner") {
            val game = board().build()
            game.meld()
            game.findPermanent("Test Meld Host").shouldNotBeNull()
            game.exile(game.player1Id) shouldBe emptyList()
        }

        test("nothing happens when you don't own and control the partner") {
            val game = board().withCardOnBattlefield(2, "Test Meld Partner").build()
            game.meld()
            game.findPermanent("Test Meld Host").shouldNotBeNull()
            game.findPermanent("Test Meld Partner").shouldNotBeNull()
            game.findPermanent("Test Meld Result") shouldBe null
        }

        test("CR 701.42b/c: cards that aren't the result's meld pair are exiled and stay there") {
            val game = board().withCardOnBattlefield(1, "Test Meld Partner").build()
            game.meld(ability = 1)
            game.findPermanent("Test Wrong Meld Result") shouldBe null
            game.exile(game.player1Id) shouldContainExactlyInAnyOrder listOf("Test Meld Host", "Test Meld Partner")
        }

        test("CR 701.42c: a token partner can't be melded — the host stays exiled, the token ceases to exist") {
            val game = board().withCardOnBattlefield(1, "Test Meld Partner", isToken = true).build()
            game.meld()
            game.findPermanent("Test Meld Result") shouldBe null
            game.isInExile(1, "Test Meld Host") shouldBe true
            game.findPermanent("Test Meld Partner") shouldBe null
        }

        test("CR 712.21: destroyed, it dies once and both cards go to the graveyard front face up") {
            val game = board()
                .withCardOnBattlefield(1, "Test Meld Partner")
                .withCardOnBattlefield(1, "Test Meld Death Watcher")
                .withCardInHand(1, "Test Meld Destroy")
                .build()
            game.meld()
            val melded = game.findPermanent("Test Meld Result")!!
            val lifeBefore = game.getLifeTotal(1)

            game.castSpell(1, "Test Meld Destroy", targetId = melded).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Test Meld Host") shouldBe true
            game.isInGraveyard(1, "Test Meld Partner") shouldBe true
            game.isInGraveyard(1, "Test Meld Result") shouldBe false
            game.state.getEntity(melded)!!.has<MeldedComponent>() shouldBe false
            withClue("one permanent died, so one dies trigger") { game.getLifeTotal(1) shouldBe lifeBefore + 1 }
        }

        test("CR 712.21: bounced, both cards go to their owner's hand") {
            val game = board()
                .withCardOnBattlefield(1, "Test Meld Partner")
                .withCardInHand(1, "Test Meld Bounce")
                .build()
            game.meld()
            val melded = game.findPermanent("Test Meld Result")!!

            game.castSpell(1, "Test Meld Bounce", targetId = melded).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Test Meld Host") shouldBe true
            game.isInHand(1, "Test Meld Partner") shouldBe true
            game.isInHand(1, "Test Meld Result") shouldBe false
        }

        test("CR 712.4c / 701.27g: a melded permanent can't be transformed and isn't a transformed permanent") {
            val game = board()
                .withCardOnBattlefield(1, "Test Meld Partner")
                .withCardInHand(1, "Transform Target Creature")
                .withLandsOnBattlefield(1, "Island", 4)
                .build()
            game.meld()
            val melded = game.findPermanent("Test Meld Result")!!

            game.castSpell(1, "Transform Target Creature", targetId = melded).error shouldBe null
            game.resolveStack()

            game.findPermanent("Test Meld Result") shouldBe melded
            services.predicateEvaluator.matches(
                game.state, game.state.projectedState, melded,
                GameObjectFilter.Creature.transformed(), PredicateContext(controllerId = game.player1Id)
            ) shouldBe false
        }

        test("'it enters tapped and attacking'") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Test Meld Host")
                .withCardOnBattlefield(1, "Test Meld Partner")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            game.declareAttackers(mapOf("Test Meld Host" to 2, "Test Meld Partner" to 2)).error shouldBe null
            game.meld(ability = 2)

            val melded = game.findPermanent("Test Meld Result").shouldNotBeNull()
            val container = game.state.getEntity(melded)!!
            container.has<TappedComponent>() shouldBe true
            container.get<AttackingComponent>()!!.defenderId shouldBe game.player2Id
        }
    }
}
