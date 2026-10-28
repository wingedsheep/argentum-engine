package com.wingedsheep.engine

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * CR 603.10a look-back for a source that **stays** on the battlefield: whether it has a conditional
 * self-granted leaves-the-battlefield ability ("as long as <condition>, this has 'whenever a
 * creature you control dies, …'") is decided by the game state immediately before the event — not
 * by the state after it, where the dying creature may have been what made the condition true, or
 * false.
 *
 * - The departure that switches the grant **off** still triggers it (Relic Vial: the only Cleric
 *   dies), alone or as part of a simultaneous batch.
 * - The departure that switches the grant **on** doesn't (a grant gated on a creature card in your
 *   graveyard: the first creature to die is what puts one there).
 * - A grant that holds both before and after triggers exactly once.
 */
class ObserverConditionalGrantLookBackTest : FunSpec({

    fun observer(name: String, condition: Condition) = card(name) {
        manaCost = "{3}"
        typeLine = "Artifact"
        staticAbility {
            ability = ConditionalStaticAbility(
                ability = GrantTriggeredAbility(
                    ability = grantedTriggeredAbility {
                        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
                        effect = Effects.GainLife(1)
                    },
                    filter = GroupFilter.source(),
                ),
                condition = condition,
            )
        }
    }

    val clericVial = observer("Cleric Vial", Conditions.YouControl(GameObjectFilter.Permanent.withSubtype("Cleric")))
    val graveWatcher = observer("Grave Watcher", Conditions.GraveyardContains(GameObjectFilter.Creature))
    val freeWrath = card("Free Wrath") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.DestroyAll(GameObjectFilter.Creature) }
    }
    val artifactSuppressor = card("Artifact Suppressor") {
        manaCost = "{3}"
        typeLine = "Enchantment"
        staticAbility { ability = LoseAllAbilities(GroupFilter.AllArtifacts) }
    }

    fun setup(vararg permanents: String): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(clericVial, graveWatcher, freeWrath, artifactSuppressor))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.activePlayer!!
        for (name in permanents) driver.putPermanentOnBattlefield(player, name)
        return driver to player
    }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) bothPass()
    }

    fun GameTestDriver.bolt(player: EntityId, victim: EntityId) {
        giveMana(player, Color.RED, 1)
        castSpell(player, putCardInHand(player, "Lightning Bolt"), listOf(victim)).error shouldBe null
        resolveAll()
    }

    test("the only Cleric dying still triggers the grant it switched off") {
        val (driver, player) = setup("Cleric Vial")
        val cleric = driver.putCreatureOnBattlefield(player, "Test Cleric")

        driver.bolt(player, cleric)

        driver.assertInGraveyard(player, "Test Cleric")
        driver.getLifeTotal(player) shouldBe 21
    }

    test("a creature dying with no Cleric around doesn't trigger") {
        val (driver, player) = setup("Cleric Vial")
        val lions = driver.putCreatureOnBattlefield(player, "Savannah Lions")

        driver.bolt(player, lions)

        driver.getLifeTotal(player) shouldBe 20
    }

    test("a grant that holds before and after the death triggers once") {
        val (driver, player) = setup("Cleric Vial")
        driver.putCreatureOnBattlefield(player, "Test Cleric")
        val lions = driver.putCreatureOnBattlefield(player, "Savannah Lions")

        driver.bolt(player, lions)

        driver.getLifeTotal(player) shouldBe 21
    }

    test("a wipe taking the only Cleric triggers for every creature that died with it") {
        val (driver, player) = setup("Cleric Vial")
        driver.putCreatureOnBattlefield(player, "Savannah Lions")
        driver.putCreatureOnBattlefield(player, "Test Cleric")
        driver.putCreatureOnBattlefield(player, "Savannah Lions")

        driver.castSpell(player, driver.putCardInHand(player, "Free Wrath")).error shouldBe null
        driver.resolveAll()

        driver.getLifeTotal(player) shouldBe 23
    }

    test("the death that switches a grant on doesn't trigger it") {
        val (driver, player) = setup("Grave Watcher")
        val first = driver.putCreatureOnBattlefield(player, "Savannah Lions")
        val second = driver.putCreatureOnBattlefield(player, "Savannah Lions")

        driver.bolt(player, first)
        driver.getLifeTotal(player) shouldBe 20

        driver.bolt(player, second)
        driver.getLifeTotal(player) shouldBe 21
    }

    test("a source that had lost all its abilities has no grant to look back to") {
        val (driver, player) = setup("Cleric Vial", "Artifact Suppressor")
        val cleric = driver.putCreatureOnBattlefield(player, "Test Cleric")

        driver.bolt(player, cleric)

        driver.getLifeTotal(player) shouldBe 20
    }
})
