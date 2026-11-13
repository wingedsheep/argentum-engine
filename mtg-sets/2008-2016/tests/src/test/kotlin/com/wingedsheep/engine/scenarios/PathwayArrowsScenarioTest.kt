package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bfz.cards.CullingDrone
import com.wingedsheep.mtg.sets.definitions.bfz.cards.PathwayArrows
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Pathway Arrows (BFZ #225) — {1} Artifact — Equipment.
 *
 *   Equipped creature has "{2}, {T}: This creature deals 1 damage to target creature. If a colorless
 *   creature is dealt damage this way, tap it."
 *   Equip {2}
 *
 * The tap rider keys on the *target's* colour as the ability resolves (projected, so devoid counts)
 * and on the damage actually landing; a coloured target just takes the 1 damage.
 */
class PathwayArrowsScenarioTest : FunSpec({

    fun equipAbilityId() = PathwayArrows.activatedAbilities.single().id

    fun grantedAbilityId() =
        PathwayArrows.staticAbilities.filterIsInstance<GrantActivatedAbility>().single().ability.id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(PathwayArrows)
        driver.registerCard(CullingDrone)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 30), startingLife = 20)
        return driver
    }

    /** Equips Pathway Arrows to a fresh, untapped-and-ready Centaur Courser of [p1]'s. */
    fun GameTestDriver.equippedCourser(p1: EntityId): Pair<EntityId, EntityId> {
        val arrows = putPermanentOnBattlefield(p1, "Pathway Arrows")
        val courser = putCreatureOnBattlefield(p1, "Centaur Courser")
        removeSummoningSickness(courser)
        passPriorityUntil(Step.PRECOMBAT_MAIN)

        giveColorlessMana(p1, 2)
        submit(
            ActivateAbility(
                playerId = p1,
                sourceId = arrows,
                abilityId = equipAbilityId(),
                targets = listOf(ChosenTarget.Permanent(courser))
            )
        ).outcome shouldBe Outcome.Done
        bothPass()
        state.getEntity(arrows)?.get<AttachedToComponent>()?.targetId shouldBe courser
        return arrows to courser
    }

    fun GameTestDriver.shoot(p1: EntityId, host: EntityId, target: EntityId) {
        giveColorlessMana(p1, 2)
        submit(
            ActivateAbility(
                playerId = p1,
                sourceId = host,
                abilityId = grantedAbilityId(),
                targets = listOf(ChosenTarget.Permanent(target))
            )
        ).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("a colorless artifact creature is dealt 1 damage and tapped") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val opponent = driver.getOpponent(p1)
        val (arrows, courser) = driver.equippedCourser(p1)
        val golem = driver.putCreatureOnBattlefield(opponent, "Artifact Creature") // colorless 2/2

        driver.shoot(p1, courser, golem)

        driver.state.getEntity(golem)?.get<DamageComponent>()?.amount shouldBe 1
        withClue("a colorless creature dealt damage this way is tapped") {
            driver.state.getEntity(golem)?.get<TappedComponent>() shouldNotBe null
        }
        withClue("the {T} cost tapped the equipped creature, not the Equipment") {
            driver.state.getEntity(courser)?.get<TappedComponent>() shouldNotBe null
            driver.state.getEntity(arrows)?.get<TappedComponent>() shouldBe null
        }
    }

    test("a devoid creature is colorless, so it is tapped too") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val opponent = driver.getOpponent(p1)
        val (_, courser) = driver.equippedCourser(p1)
        val drone = driver.putCreatureOnBattlefield(opponent, "Culling Drone") // {1}{B}, devoid, 2/2

        driver.shoot(p1, courser, drone)

        driver.state.getEntity(drone)?.get<DamageComponent>()?.amount shouldBe 1
        driver.state.getEntity(drone)?.get<TappedComponent>() shouldNotBe null
    }

    test("a colored creature takes the damage but is not tapped") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val opponent = driver.getOpponent(p1)
        val (_, courser) = driver.equippedCourser(p1)
        val bigCourser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // green 3/3

        driver.shoot(p1, courser, bigCourser)

        driver.state.getEntity(bigCourser)?.get<DamageComponent>()?.amount shouldBe 1
        withClue("only a colorless creature is tapped") {
            driver.state.getEntity(bigCourser)?.get<TappedComponent>() shouldBe null
        }
    }
})
