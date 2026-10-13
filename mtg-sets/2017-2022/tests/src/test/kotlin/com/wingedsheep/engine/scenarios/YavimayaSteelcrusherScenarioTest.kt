package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DominariaUnitedForest274
import com.wingedsheep.mtg.sets.definitions.dmu.cards.YavimayaSteelcrusher
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class YavimayaSteelcrusherScenarioTest : FunSpec({
    val relic = card("Steelcrusher Relic") { typeLine = "Artifact" }
    fun setup() = GameTestDriver().apply {
        registerCards(listOf(YavimayaSteelcrusher, DominariaUnitedForest274, relic))
        initMirrorMatch(deck = Deck.of("Forest" to 30), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    test("{1}, sacrifice it: destroy target artifact") {
        val d = setup(); val p = d.activePlayer!!; val opp = d.getOpponent(p)
        val crusher = d.putCreatureOnBattlefield(p, YavimayaSteelcrusher.name)
        val artifact = d.putPermanentOnBattlefield(opp, relic.name)
        d.giveColorlessMana(p, 1)
        val action = d.legalActions(p).map { it.action }.filterIsInstance<ActivateAbility>()
            .single { it.sourceId == crusher }
        d.submit(action.copy(targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(artifact))))
            .error shouldBe null
        d.getGraveyard(p) shouldContain crusher
        d.bothPass().error shouldBe null
        d.getGraveyard(opp) shouldContain artifact
    }
})
