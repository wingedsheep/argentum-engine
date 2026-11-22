package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mor.cards.Mutavault
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Mutavault (MOR #148) — Land.
 *  {T}: Add {C}.
 *  {1}: This land becomes a 2/2 creature with all creature types until end of turn. It's still a land.
 *
 * Pins "all creature types" on a land that is not inherently a Changeling: the granted Changeling
 * keyword must expand to every creature type once the land is animated, and both the creature
 * status and the types must fall away at end of turn.
 */
class MutavaultScenarioTest : FunSpec({

    val animateAbilityId = Mutavault.activatedAbilities[1].id
    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Mutavault))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        return driver
    }

    test("animates into a 2/2 land creature with all creature types") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val vault = driver.putLandOnBattlefield(player, "Mutavault")
        projector.project(driver.state).hasSubtype(vault, "Goblin") shouldBe false

        driver.giveColorlessMana(player, 1)
        driver.submit(
            ActivateAbility(playerId = player, sourceId = vault, abilityId = animateAbilityId)
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        val projected = projector.project(driver.state)
        projected.hasType(vault, "CREATURE") shouldBe true
        projected.hasType(vault, "LAND") shouldBe true // it's still a land
        projected.getPower(vault) shouldBe 2
        projected.getToughness(vault) shouldBe 2
        projected.hasSubtype(vault, "Goblin") shouldBe true
        projected.hasSubtype(vault, "Elf") shouldBe true
        projected.hasSubtype(vault, "Sliver") shouldBe true
    }

    test("reverts to a plain land with no creature types at end of turn") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val vault = driver.putLandOnBattlefield(player, "Mutavault")
        driver.giveColorlessMana(player, 1)
        driver.submit(
            ActivateAbility(playerId = player, sourceId = vault, abilityId = animateAbilityId)
        ).outcome shouldBe Outcome.Done
        driver.bothPass()
        projector.project(driver.state).hasType(vault, "CREATURE") shouldBe true

        driver.passPriorityUntil(Step.UPKEEP)
        val next = projector.project(driver.state)
        next.hasType(vault, "CREATURE") shouldBe false
        next.hasType(vault, "LAND") shouldBe true
        next.hasSubtype(vault, "Goblin") shouldBe false
    }
})
