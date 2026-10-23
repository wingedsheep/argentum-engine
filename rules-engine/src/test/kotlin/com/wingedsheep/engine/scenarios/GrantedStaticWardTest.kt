package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ward a permanent *gains* from a resolved effect — `GrantStaticAbility(GrantWard(…))` — rather
 * than has printed (Hall of Storm Giants: "until end of turn, this land becomes a 7/7 blue Giant
 * creature with ward {3}"). The layer system projects the WARD keyword through the grant record;
 * these pin that the ward trigger comes with it, for a self grant and a battlefield-scope one, that
 * it ends with the grant's duration, and that a later "loses all abilities" ends it too.
 */
class GrantedStaticWardTest : FunSpec({

    val selfWarder = card("Self Warder") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Giant"
        power = 2
        toughness = 2
        activatedAbility {
            cost = Costs.Free
            effect = Effects.GrantStaticAbility(
                GrantWard(WardCost.Life(2), Filters.Self), EffectTarget.Self, Duration.EndOfTurn
            )
        }
    }

    val groupWarder = card("Group Warder") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Giant"
        power = 2
        toughness = 2
        activatedAbility {
            cost = Costs.Free
            effect = Effects.GrantStaticAbility(
                GrantWard(WardCost.Life(2), GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true)),
                EffectTarget.Self,
                Duration.EndOfTurn
            )
        }
    }

    val plainBear = card("Plain Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    val muzzle = card("Muzzle Ray") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            target = TargetObject(filter = TargetFilter.Creature)
            effect = Effects.RemoveAllAbilities(EffectTarget.ContextTarget(0))
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(selfWarder, groupWarder, plainBear, muzzle))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.activateOnly(player: EntityId, permanent: EntityId, cardDef: com.wingedsheep.sdk.model.CardDefinition) {
        submit(ActivateAbility(player, permanent, cardDef.script.activatedAbilities.single().id)).error shouldBe null
        bothPass()
    }

    fun GameTestDriver.boltFrom(caster: EntityId, target: EntityId) {
        giveMana(caster, Color.RED, 1)
        castSpellWithTargets(caster, putCardInHand(caster, "Lightning Bolt"), listOf(ChosenTarget.Permanent(target)))
        bothPass()
    }

    test("a permanent that gains ward from a resolved effect has ward's trigger, not just the keyword") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        val warder = driver.putCreatureOnBattlefield(active, "Self Warder")

        driver.activateOnly(active, warder, selfWarder)
        driver.state.projectedState.getKeywords(warder) shouldContain Keyword.WARD.name

        driver.passPriority(active)
        driver.boltFrom(opponent, warder)

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<YesNoDecision>()
        decision.playerId shouldBe opponent
        driver.submitYesNo(opponent, false)
        repeat(2) { if (driver.state.priorityPlayerId != null) driver.bothPass() }
        driver.findPermanent(active, "Self Warder") shouldNotBe null
    }

    test("without the grant there is no ward") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        val warder = driver.putCreatureOnBattlefield(active, "Self Warder")

        driver.passPriority(active)
        driver.boltFrom(opponent, warder)

        driver.pendingDecision shouldBe null
        driver.findPermanent(active, "Self Warder") shouldBe null
    }

    test("an until-end-of-turn ward grant is gone next turn") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        val warder = driver.putCreatureOnBattlefield(active, "Self Warder")

        driver.activateOnly(active, warder, selfWarder)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, opponent)
        driver.state.projectedState.getKeywords(warder) shouldNotContain Keyword.WARD.name

        driver.boltFrom(opponent, warder)

        driver.pendingDecision shouldBe null
        driver.findPermanent(active, "Self Warder") shouldBe null
    }

    test("a battlefield-scope ward grant covers the group it names, not the holder") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        val warder = driver.putCreatureOnBattlefield(active, "Group Warder")
        val bear = driver.putCreatureOnBattlefield(active, "Plain Bear")

        driver.activateOnly(active, warder, groupWarder)
        driver.state.projectedState.getKeywords(bear) shouldContain Keyword.WARD.name
        driver.state.projectedState.getKeywords(warder) shouldNotContain Keyword.WARD.name

        driver.passPriority(active)
        driver.boltFrom(opponent, bear)

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<YesNoDecision>()
        decision.playerId shouldBe opponent
    }

    test("losing all abilities after gaining ward takes the ward trigger away") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        val warder = driver.putCreatureOnBattlefield(active, "Self Warder")

        driver.activateOnly(active, warder, selfWarder)
        driver.giveMana(active, Color.BLUE, 1)
        driver.castSpellWithTargets(active, driver.putCardInHand(active, "Muzzle Ray"), listOf(ChosenTarget.Permanent(warder)))
        driver.bothPass()
        driver.state.projectedState.getKeywords(warder) shouldNotContain Keyword.WARD.name

        driver.passPriority(active)
        driver.boltFrom(opponent, warder)

        driver.pendingDecision shouldBe null
        driver.findPermanent(active, "Self Warder") shouldBe null
    }
})
