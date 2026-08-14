package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.HexproofFromComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ecl.cards.GiltLeafsEmbrace
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldNotBe

class CastSpellSourceHexproofEnumerationTest : FunSpec({
    test("cast enumeration excludes a permanent with hexproof from the spell's color") {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all + GiltLeafsEmbrace)
            initMirrorMatch(Deck.of("Forest" to 40))
        }
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        val aura = driver.putCardInHand(caster, "Gilt-Leaf's Embrace")
        val legalOwnTarget = driver.putCreatureOnBattlefield(caster, "Grizzly Bears")
        val protectedOpponent = driver.putCreatureOnBattlefield(opponent, "Hill Giant")
        driver.addComponent(protectedOpponent, HexproofFromComponent(colors = setOf(Color.GREEN)))
        repeat(3) { driver.putLandOnBattlefield(caster, "Forest") }

        val cast = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, caster)
            .singleOrNull { (it.action as? CastSpell)?.cardId == aura }

        cast shouldNotBe null
        cast!!.validTargets.shouldContainExactly(legalOwnTarget)
    }
})
