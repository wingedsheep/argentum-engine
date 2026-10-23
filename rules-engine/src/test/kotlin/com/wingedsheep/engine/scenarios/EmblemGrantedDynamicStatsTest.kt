package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A dynamic "+X/+X" an emblem has itself — `CreatePermanentEmblem(ownedStaticAbilities =
 * listOf(GrantDynamicStats(group, amount, amount)))`, the shape of Ellywick Tumblestrum's
 * "creatures you control … get +2/+2 for each differently named dungeon you've completed".
 *
 * An emblem's abilities work for as long as it exists (CR 114.4), so the amount must be re-read
 * every projection rather than frozen when the emblem is created, "you" must be the emblem's
 * controller, and the group must pick up creatures that arrive later.
 */
class EmblemGrantedDynamicStatsTest : FunSpec({

    val creaturesYouControl = GroupFilter(GameObjectFilter.Creature.youControl())
    val twicePerLand = DynamicAmounts.landsYouControl() * 2

    val landsEmblem = card("Lands Emblem") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.CreatePermanentEmblem(
                ownedStaticAbilities = listOf(GrantDynamicStats(creaturesYouControl, twicePerLand, twicePerLand)),
                emblemDescription = "Creatures you control get +2/+2 for each land you control."
            )
        }
    }

    val plainBear = card("Plain Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(landsEmblem, plainBear))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.giveEmblem(playerId: EntityId) {
        giveMana(playerId, Color.GREEN, 1)
        castSpell(playerId, putCardInHand(playerId, "Lands Emblem"))
        bothPass()
    }

    fun GameTestDriver.stats(id: EntityId) =
        state.projectedState.getPower(id) to state.projectedState.getToughness(id)

    test("the bonus tracks the amount as it changes, and reaches creatures that enter after the emblem") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        driver.giveEmblem(active)
        val bear = driver.putCreatureOnBattlefield(active, "Plain Bear")
        withClue("No lands yet: the bonus is +0/+0") { driver.stats(bear) shouldBe (2 to 2) }

        driver.putLandOnBattlefield(active, "Forest")
        driver.putLandOnBattlefield(active, "Forest")
        withClue("Two more lands: +4/+4 more, read at projection rather than frozen at creation") {
            driver.stats(bear) shouldBe (6 to 6)
        }
    }

    test("'you' is the emblem's controller — an opponent's lands don't count and their creatures get nothing") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        driver.giveEmblem(active)
        val bear = driver.putCreatureOnBattlefield(active, "Plain Bear")
        val theirBear = driver.putCreatureOnBattlefield(opponent, "Plain Bear")
        val before = driver.stats(bear)

        driver.putLandOnBattlefield(opponent, "Forest")
        driver.putLandOnBattlefield(opponent, "Forest")
        driver.stats(bear) shouldBe before
        driver.putLandOnBattlefield(active, "Forest")
        withClue("The opponent's creature is outside 'creatures you control'") {
            driver.stats(theirBear) shouldBe (2 to 2)
        }
    }
})
