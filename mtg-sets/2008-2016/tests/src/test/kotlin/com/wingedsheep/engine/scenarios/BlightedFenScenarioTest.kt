package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bfz.cards.BlightedFen
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Blighted Fen (BFZ #230) — Land.
 *
 *   {T}: Add {C}.
 *   {4}{B}, {T}, Sacrifice this land: Target opponent sacrifices a creature of their choice.
 */
class BlightedFenScenarioTest : FunSpec({

    val edictAbility = BlightedFen.activatedAbilities[1].id

    test("sacrificing the Fen makes the target opponent sacrifice a creature") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(BlightedFen))
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)

        val fen = d.putLandOnBattlefield(you, "Blighted Fen")
        d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        d.giveMana(you, Color.BLACK, 1)
        d.giveColorlessMana(you, 4)

        d.submit(
            ActivateAbility(
                playerId = you,
                sourceId = fen,
                abilityId = edictAbility,
                targets = listOf(ChosenTarget.Player(opponent)),
            )
        ).outcome shouldBe Outcome.Done
        d.getGraveyardCardNames(you).contains("Blighted Fen") shouldBe true

        d.bothPass()
        d.getCreatures(opponent).size shouldBe 0
        d.getGraveyardCardNames(opponent).contains("Grizzly Bears") shouldBe true
    }
})
