package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.akh.cards.HarshMentor
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Harsh Mentor — "Whenever an opponent activates an ability of an artifact, creature, or land on
 * the battlefield, if it isn't a mana ability, this creature deals 2 damage to that player."
 *
 * player2 controls the Mentor; player1 (the active player, its opponent) activates.
 */
class HarshMentorScenarioTest : FunSpec({

    val pingBear = card("Ping Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        oracleText = "{1}: You gain 1 life."
        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.GainLife(1)
        }
    }

    // A fetch-land stand-in: the cost sacrifices the land itself (CR 113.7a).
    val crackLand = card("Crack Land") {
        typeLine = "Land"
        oracleText = "{T}, Sacrifice this land: You gain 1 life."
        activatedAbility {
            cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
            effect = Effects.GainLife(1)
        }
    }

    val manaLand = card("Mana Land") {
        typeLine = "Land"
        oracleText = "{T}: Add {C}."
        activatedAbility {
            cost = AbilityCost.Tap
            effect = AddColorlessManaEffect(1)
            manaAbility = true
        }
    }

    // A creature card whose ability works from the graveyard and exiles itself as the cost.
    val graveBeast = card("Grave Beast") {
        manaCost = "{2}{B}"
        typeLine = "Creature — Zombie"
        power = 2
        toughness = 2
        oracleText = "Exile this card from your graveyard: You gain 1 life."
        activatedAbility {
            cost = Costs.ExileSelf
            effect = Effects.GainLife(1)
            activateFromZone = Zone.GRAVEYARD
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        listOf(HarshMentor, pingBear, crackLand, manaLand, graveBeast).forEach { driver.registerCard(it) }
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard < 30) { bothPass(); guard++ }
    }

    fun GameTestDriver.activate(player: com.wingedsheep.sdk.model.EntityId, source: com.wingedsheep.sdk.model.EntityId, name: String) {
        val abilityId = cardRegistry.requireCard(name).activatedAbilities[0].id
        submitSuccess(ActivateAbility(playerId = player, sourceId = source, abilityId = abilityId))
        resolveStack()
    }

    test("an opponent activating a creature's non-mana ability takes 2 damage") {
        val driver = createDriver()
        val opponent = driver.player1
        driver.putPermanentOnBattlefield(driver.player2, "Harsh Mentor")
        val bear = driver.putPermanentOnBattlefield(opponent, "Ping Bear")
        driver.giveColorlessMana(opponent, 1)

        driver.activate(opponent, bear, "Ping Bear")

        // 20 − 2 from the Mentor + 1 from the bear's own ability.
        driver.getLifeTotal(opponent) shouldBe 19
    }

    test("a land its own cost sacrificed still counts — matched as it last existed on the battlefield") {
        val driver = createDriver()
        val opponent = driver.player1
        driver.putPermanentOnBattlefield(driver.player2, "Harsh Mentor")
        val land = driver.putPermanentOnBattlefield(opponent, "Crack Land")

        driver.activate(opponent, land, "Crack Land")

        driver.getLifeTotal(opponent) shouldBe 19
    }

    test("a mana ability doesn't trigger it") {
        val driver = createDriver()
        val opponent = driver.player1
        driver.putPermanentOnBattlefield(driver.player2, "Harsh Mentor")
        val land = driver.putPermanentOnBattlefield(opponent, "Mana Land")

        driver.activate(opponent, land, "Mana Land")

        driver.getLifeTotal(opponent) shouldBe 20
    }

    test("an ability of a creature card in the graveyard doesn't trigger it") {
        val driver = createDriver()
        val opponent = driver.player1
        driver.putPermanentOnBattlefield(driver.player2, "Harsh Mentor")
        val beast = driver.putCardInGraveyard(opponent, "Grave Beast")

        driver.activate(opponent, beast, "Grave Beast")

        driver.getLifeTotal(opponent) shouldBe 21
    }

    test("the Mentor's controller activating their own creature's ability takes no damage") {
        val driver = createDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "Harsh Mentor")
        val bear = driver.putPermanentOnBattlefield(me, "Ping Bear")
        driver.giveColorlessMana(me, 1)

        driver.activate(me, bear, "Ping Bear")

        driver.getLifeTotal(me) shouldBe 21
    }
})
