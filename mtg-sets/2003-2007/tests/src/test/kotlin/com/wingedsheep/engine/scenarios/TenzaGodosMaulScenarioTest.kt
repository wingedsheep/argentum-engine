package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.IsamaruHoundOfKonda
import com.wingedsheep.mtg.sets.definitions.chk.cards.TenzaGodosMaul
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tenza, Godo's Maul — {3} Legendary Artifact — Equipment (CHK #271)
 *
 * "Equipped creature gets +1/+1. As long as it's legendary, it gets an additional +2/+2.
 *  As long as it's red, it has trample. Equip {1}"
 */
class TenzaGodosMaulScenarioTest : FunSpec({

    val equipAbilityId = TenzaGodosMaul.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + TenzaGodosMaul + IsamaruHoundOfKonda)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.equipTo(creatureName: String): EntityId {
        val maul = putPermanentOnBattlefield(player1, "Tenza, Godo's Maul")
        val creature = putCreatureOnBattlefield(player1, creatureName)
        giveColorlessMana(player1, 1)
        submit(
            ActivateAbility(
                playerId = player1,
                sourceId = maul,
                abilityId = equipAbilityId,
                targets = listOf(ChosenTarget.Permanent(creature))
            )
        ).outcome shouldBe Outcome.Done
        bothPass()
        state.getEntity(maul)?.get<AttachedToComponent>()?.targetId shouldBe creature
        return creature
    }

    test("a nonlegendary nonred creature gets only +1/+1 and no trample") {
        val d = driver()
        val lions = d.equipTo("Savannah Lions") // 1/1 white
        d.state.projectedState.getPower(lions) shouldBe 2
        d.state.projectedState.getToughness(lions) shouldBe 2
        d.state.projectedState.hasKeyword(lions, Keyword.TRAMPLE) shouldBe false
    }

    test("a legendary creature gets an additional +2/+2") {
        val d = driver()
        val isamaru = d.equipTo("Isamaru, Hound of Konda") // 2/2 legendary white
        withClue("+1/+1 plus the legendary +2/+2") {
            d.state.projectedState.getPower(isamaru) shouldBe 5
            d.state.projectedState.getToughness(isamaru) shouldBe 5
        }
        d.state.projectedState.hasKeyword(isamaru, Keyword.TRAMPLE) shouldBe false
    }

    test("a red creature has trample but no legendary bonus") {
        val d = driver()
        val guide = d.equipTo("Goblin Guide") // 2/1 red, nonlegendary
        d.state.projectedState.getPower(guide) shouldBe 3
        d.state.projectedState.getToughness(guide) shouldBe 2
        d.state.projectedState.hasKeyword(guide, Keyword.TRAMPLE) shouldBe true
    }
})
