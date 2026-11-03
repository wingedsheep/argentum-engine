package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.trk.cards.KaiWinnFaithlessLeader
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Kai Winn, Faithless Leader — "Whenever an opponent casts a spell with the chosen name or
 * activates an ability of a source with the chosen name, that player loses 3 life."
 *
 * player2 controls Kai Winn with a name already chosen; player1 (the active player, its opponent)
 * casts and activates. The interesting leg is a source its own cost sacrificed: it is matched
 * against the chosen name by its last-known name (CR 113.7a), the live object being gone.
 */
class KaiWinnFaithlessLeaderScenarioTest : FunSpec({

    val relic = card("Sac Relic") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "Sacrifice this artifact: You gain 1 life."
        activatedAbility {
            cost = Costs.SacrificeSelf
            effect = Effects.GainLife(1)
        }
    }

    val manaRock = card("Mana Rock") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "{T}: Add {C}."
        activatedAbility {
            cost = AbilityCost.Tap
            effect = AddColorlessManaEffect(1)
            manaAbility = true
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        listOf(KaiWinnFaithlessLeader, relic, manaRock).forEach { driver.registerCard(it) }
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Put Kai Winn onto [owner]'s battlefield with [name] already chosen as it entered. */
    fun GameTestDriver.kaiNaming(owner: EntityId, name: String): EntityId {
        val kai = putPermanentOnBattlefield(owner, "Kai Winn, Faithless Leader")
        replaceState(state.updateEntity(kai) {
            it.with(CastChoicesComponent().withChoice(ChoiceSlot.CARD_NAME, ChoiceValue.TextChoice(name)))
        })
        return kai
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard < 30) { bothPass(); guard++ }
    }

    fun GameTestDriver.activate(player: EntityId, source: EntityId, name: String) {
        val abilityId = cardRegistry.requireCard(name).activatedAbilities[0].id
        submitSuccess(ActivateAbility(playerId = player, sourceId = source, abilityId = abilityId))
        resolveStack()
    }

    test("an opponent casting a spell with the chosen name loses 3 life") {
        val driver = createDriver()
        val opponent = driver.player1
        driver.kaiNaming(driver.player2, "Grizzly Bears")
        val bears = driver.putCardInHand(opponent, "Grizzly Bears")
        driver.giveMana(opponent, Color.GREEN, 2)

        driver.castSpell(opponent, bears)
        driver.resolveStack()

        driver.getLifeTotal(opponent) shouldBe 17
        driver.getLifeTotal(driver.player2) shouldBe 20
    }

    test("a spell with a different name doesn't trigger it") {
        val driver = createDriver()
        val opponent = driver.player1
        driver.kaiNaming(driver.player2, "Hill Giant")
        val bears = driver.putCardInHand(opponent, "Grizzly Bears")
        driver.giveMana(opponent, Color.GREEN, 2)

        driver.castSpell(opponent, bears)
        driver.resolveStack()

        driver.getLifeTotal(opponent) shouldBe 20
    }

    test("a mana ability of a source with the chosen name counts — 'an ability' is unqualified") {
        val driver = createDriver()
        val opponent = driver.player1
        driver.kaiNaming(driver.player2, "Mana Rock")
        val rock = driver.putPermanentOnBattlefield(opponent, "Mana Rock")

        driver.activate(opponent, rock, "Mana Rock")

        driver.getLifeTotal(opponent) shouldBe 17
    }

    test("a source its own cost sacrificed is matched by its last-known name") {
        val driver = createDriver()
        val opponent = driver.player1
        driver.kaiNaming(driver.player2, "Sac Relic")
        val sacRelic = driver.putPermanentOnBattlefield(opponent, "Sac Relic")

        driver.activate(opponent, sacRelic, "Sac Relic")

        // 20 − 3 from Kai Winn + 1 from the relic's own ability.
        driver.getLifeTotal(opponent) shouldBe 18
    }

    test("a sacrificed source with a different name doesn't trigger it") {
        val driver = createDriver()
        val opponent = driver.player1
        driver.kaiNaming(driver.player2, "Mana Rock")
        val sacRelic = driver.putPermanentOnBattlefield(opponent, "Sac Relic")

        driver.activate(opponent, sacRelic, "Sac Relic")

        driver.getLifeTotal(opponent) shouldBe 21
    }

    test("Kai Winn's controller activating a named source loses nothing") {
        val driver = createDriver()
        val me = driver.player1
        driver.kaiNaming(me, "Sac Relic")
        val sacRelic = driver.putPermanentOnBattlefield(me, "Sac Relic")

        driver.activate(me, sacRelic, "Sac Relic")

        driver.getLifeTotal(me) shouldBe 21
    }
})
