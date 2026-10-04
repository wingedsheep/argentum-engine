package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `DistributeDamageAmongCollection` — a creature deals damage divided among an untargeted pipeline
 * collection, the split chosen at resolution by the effect's chooser.
 *
 * Rules pinned here: the recipients aren't targets, so the division is made as the effect resolves
 * (CR 608.2d) by the named chooser — the *target's* controller under `ControllerOfTarget`, not the
 * spell's controller; a collection member that left the battlefield is a new object (CR 400.7) and
 * can't be dealt the damage; the damage is dealt by the named creature, not the resolving spell;
 * zero power deals nothing. Card-level coverage lives in `MasterOfTheWildHuntScenarioTest`.
 */
class DistributeDamageAmongCollectionTest : FunSpec({

    // "Choose target creature and target creature. Exile the second. The first deals damage equal to
    // its power divided as its controller chooses among creatures you control."
    val Splitter = card("Distribute Back Test") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            val dealer = target(TargetFilter.Creature)
            val exiled = target(TargetFilter.Creature)
            effect = Effects.Pipeline {
                val mine = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Creature))
                run(Effects.Exile(exiled))
                run(
                    Effects.DistributeDamageAmongCollection(
                        amount = DynamicAmounts.powerOf(dealer),
                        among = mine,
                        damageSource = dealer,
                        chooser = Chooser.ControllerOfTarget
                    )
                )
            }
        }
    }

    val ZeroPower = card("Zero Power Test Wall") {
        manaCost = "{1}"
        typeLine = "Artifact Creature — Wall"
        power = 0
        toughness = 4
    }

    val Steal = card("Distribute Steal Test") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { val t = target(TargetFilter.Creature); effect = Effects.GainControl(t) }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Splitter, ZeroPower, Steal))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.cast(dealer: EntityId, exiled: EntityId) {
        val spell = putCardInHand(player1, "Distribute Back Test")
        giveMana(player1, Color.RED, 1)
        castSpell(player1, spell, listOf(dealer, exiled)).error shouldBe null
        bothPass()
    }

    fun GameTestDriver.damageOn(id: EntityId): Int = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    test("the target's controller divides, the target deals it, and a member that left drops out") {
        val d = newDriver()
        val force = d.putCreatureOnBattlefield(d.player2, "Force of Nature") // 5/5
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        val guide = d.putCreatureOnBattlefield(d.player1, "Goblin Guide")
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")

        d.cast(dealer = force, exiled = courser)

        val decision = d.state.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        decision.playerId shouldBe d.player2
        decision.totalAmount shouldBe 5
        decision.minPerTarget shouldBe 0
        decision.targets shouldContainExactlyInAnyOrder listOf(lions, guide)

        val before = d.events.size
        d.submitDecision(d.player2, DistributionResponse(decision.id, mapOf(lions to 1, guide to 4))).error shouldBe null

        val damage = d.events.drop(before).filterIsInstance<DamageDealtEvent>()
        damage.map { it.targetId to it.amount } shouldContainExactlyInAnyOrder listOf(lions to 1, guide to 4)
        damage.map { it.sourceId }.toSet() shouldBe setOf(force)
        d.findPermanent(d.player1, "Savannah Lions") shouldBe null
        d.findPermanent(d.player1, "Goblin Guide") shouldBe null
    }

    test("targeting your own creature makes you the chooser, and it may be dealt its own damage") {
        val d = newDriver()
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser") // 3/3
        val guide = d.putCreatureOnBattlefield(d.player1, "Goblin Guide")
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")

        d.cast(dealer = courser, exiled = lions)

        val decision = d.state.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        decision.playerId shouldBe d.player1
        decision.targets shouldContainExactlyInAnyOrder listOf(courser, guide)
        d.submitDecision(d.player1, DistributionResponse(decision.id, mapOf(courser to 2, guide to 1))).error shouldBe null

        d.damageOn(courser) shouldBe 2
        d.findPermanent(d.player1, "Goblin Guide") shouldBe null
    }

    test("a stolen target's current controller divides, not its owner") {
        val d = newDriver()
        val force = d.putCreatureOnBattlefield(d.player2, "Force of Nature")
        val guide = d.putCreatureOnBattlefield(d.player1, "Goblin Guide")
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val steal = d.putCardInHand(d.player1, "Distribute Steal Test")
        d.castSpell(d.player1, steal, listOf(force)).error shouldBe null
        d.bothPass()
        d.state.projectedState.getController(force) shouldBe d.player1

        d.cast(dealer = force, exiled = courser)

        val decision = d.state.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        decision.playerId shouldBe d.player1
        // Stolen, it's now one of "creatures you control" too.
        decision.targets shouldContainExactlyInAnyOrder listOf(force, guide, lions)
    }

    test("a single remaining member takes everything without a prompt") {
        val d = newDriver()
        val force = d.putCreatureOnBattlefield(d.player2, "Force of Nature")
        d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")

        d.cast(dealer = force, exiled = lions)

        d.state.pendingDecision shouldBe null
        d.findPermanent(d.player1, "Centaur Courser") shouldBe null
        d.getGraveyardCardNames(d.player1).contains("Centaur Courser") shouldBe true
    }

    test("zero power deals nothing and asks nothing") {
        val d = newDriver()
        val wall = d.putCreatureOnBattlefield(d.player2, "Zero Power Test Wall")
        val courser = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val guide = d.putCreatureOnBattlefield(d.player1, "Goblin Guide")
        val lions = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")

        val before = d.events.size
        d.cast(dealer = wall, exiled = lions)

        d.state.pendingDecision shouldBe null
        d.events.drop(before).filterIsInstance<DamageDealtEvent>().shouldBeEmpty()
        d.damageOn(courser) shouldBe 0
        d.damageOn(guide) shouldBe 0
    }
})
