package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ths.cards.NykthosShrineToNyx
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Nykthos, Shrine to Nyx — "{2}, {T}: Choose a color. Add an amount of mana of that color equal to
 * your devotion to that color." The {2} is paid from other lands, and the mana added is sized by
 * the color chosen as the ability resolves.
 */
class NykthosShrineToNyxScenarioTest : FunSpec({

    val devotion = NykthosShrineToNyx.activatedAbilities[1].id

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(NykthosShrineToNyx))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(you, "Force of Nature") // {3}{G}{G}
        d.putCreatureOnBattlefield(you, "Centaur Courser") // {2}{G}
        d.putLandOnBattlefield(you, "Forest")
        d.putLandOnBattlefield(you, "Forest")
        return Triple(d, you, d.putLandOnBattlefield(you, "Nykthos, Shrine to Nyx"))
    }

    fun GameTestDriver.pool(p: EntityId) = state.getEntity(p)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    test("pays {2} and adds green mana equal to devotion to green") {
        val (d, you, nykthos) = setup()
        d.submit(ActivateAbility(you, nykthos, devotion, manaColorChoice = Color.GREEN)).error shouldBe null
        d.pool(you).green shouldBe 3
        d.pool(you).colorless shouldBe 0
    }

    test("a color you have no devotion to adds nothing") {
        val (d, you, nykthos) = setup()
        d.submit(ActivateAbility(you, nykthos, devotion, manaColorChoice = Color.RED)).error shouldBe null
        d.pool(you).isEmpty shouldBe true
    }

    test("auto-pay activates the devotion ability, paying its {2} from other lands") {
        val (d, you, _) = setup()
        d.putLandOnBattlefield(you, "Forest")
        d.putLandOnBattlefield(you, "Forest")
        // Four Forests + Nykthos: two Forests pay {2} for three green, two more Forests — five mana.
        val force = d.putCardInHand(you, "Force of Nature")
        d.castSpell(you, force).error shouldBe null
        d.state.stack.contains(force) shouldBe true
    }
})
