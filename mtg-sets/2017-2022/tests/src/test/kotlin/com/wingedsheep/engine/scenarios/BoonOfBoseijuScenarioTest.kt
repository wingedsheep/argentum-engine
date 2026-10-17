package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.BoonOfBoseiju
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Boon of Boseiju — {1}{G} Instant
 * Target creature gets +X/+X until end of turn, where X is the greatest mana value among
 * permanents you control. Untap it.
 */
class BoonOfBoseijuScenarioTest : FunSpec({

    val bigArtifact = card("Boon Test Monolith") {
        manaCost = "{5}"
        typeLine = "Artifact"
    }

    val opposingColossus = card("Boon Test Colossus") {
        manaCost = "{7}"
        typeLine = "Artifact"
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + BoonOfBoseiju + bigArtifact + opposingColossus)
        initMirrorMatch(deck = Deck.of("Forest" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("pumps by the greatest mana value among your permanents and untaps the creature") {
        val d = driver()
        val p1 = d.player1
        val lions = d.putCreatureOnBattlefield(p1, "Savannah Lions") // 1/1, MV 1
        d.putPermanentOnBattlefield(p1, bigArtifact.name) // MV 5 — the greatest
        // An opposing permanent with a bigger mana value (7) doesn't count.
        d.putPermanentOnBattlefield(d.player2, opposingColossus.name)
        d.tapPermanent(lions)

        val boon = d.putCardInHand(p1, BoonOfBoseiju.name)
        d.giveMana(p1, Color.GREEN, 2)
        d.castSpell(p1, boon, targets = listOf(lions)).outcome shouldBe Outcome.Done
        d.bothPass()

        d.state.projectedState.getPower(lions) shouldBe 6
        d.state.projectedState.getToughness(lions) shouldBe 6
        d.isTapped(lions) shouldBe false
    }
})
