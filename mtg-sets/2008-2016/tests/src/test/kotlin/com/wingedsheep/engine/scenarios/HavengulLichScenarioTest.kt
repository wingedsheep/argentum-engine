package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.cards.HavengulLich
import com.wingedsheep.mtg.sets.definitions.mrd.cards.SpikeshotGoblin
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Havengul Lich (DKA #139) — "{1}: You may cast target creature card in a graveyard this turn.
 * When you cast it this turn, this creature gains all activated abilities of that card until end
 * of turn."
 *
 *  - The permission reaches an opponent's graveyard, and casting the card there puts the delayed
 *    trigger on the stack above the creature spell; the Lich gains the card's activated abilities
 *    while it is still a spell (2019-10-04 ruling) and can use them as its own.
 *  - The trigger watches *that card*: casting some other creature spell doesn't fire it.
 *  - "It" is the object the card was in the graveyard (CR 400.7): the same card cast after it
 *    left the graveyard some other way is a new object, and the trigger doesn't fire.
 *  - If you don't cast the card that turn, the Lich gains nothing.
 */
class HavengulLichScenarioTest : FunSpec({

    val lichAbility = HavengulLich.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + HavengulLich + SpikeshotGoblin)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.lich(): EntityId =
        putCreatureOnBattlefield(player1, "Havengul Lich").also { removeSummoningSickness(it) }

    fun GameTestDriver.ownerOf(card: EntityId): EntityId =
        if (card in state.getGraveyard(player1)) player1 else player2

    /** Pay {1} and resolve the Lich's ability targeting [card]. */
    fun GameTestDriver.activateLich(lich: EntityId, card: EntityId) {
        giveColorlessMana(player1, 1)
        submit(ActivateAbility(player1, lich, lichAbility, targets = listOf(ChosenTarget.Card(card, ownerOf(card), Zone.GRAVEYARD))))
            .error shouldBe null
        bothPass()
    }

    fun GameTestDriver.gainedAbilityIds(entityId: EntityId): List<AbilityId> =
        state.grantedActivatedAbilities.filter { it.entityId == entityId }.map { it.ability.id }

    test("casting the card from an opponent's graveyard gives the Lich its activated abilities") {
        val d = driver()
        val opponent = d.getOpponent(d.player1)
        val lich = d.lich()
        val goblin = d.putCardInGraveyard(opponent, "Spikeshot Goblin")
        d.activateLich(lich, goblin)

        d.giveMana(d.player1, Color.RED, 1)
        d.giveColorlessMana(d.player1, 2)
        d.submit(CastSpell(d.player1, goblin)).error shouldBe null

        withClue("the delayed trigger goes on the stack above the creature spell") {
            d.state.stack.size shouldBe 2
        }
        d.bothPass()
        withClue("the Lich gains the ability while the Goblin is still a spell on the stack") {
            d.state.stack shouldBe listOf(goblin)
            d.gainedAbilityIds(lich).size shouldBe 1
        }
        d.bothPass()
        d.state.getZone(ZoneKey(d.player1, Zone.BATTLEFIELD)).contains(goblin) shouldBe true

        val gained = d.gainedAbilityIds(lich).single()
        d.giveMana(d.player1, Color.RED, 1)
        d.submit(ActivateAbility(d.player1, lich, gained, targets = listOf(ChosenTarget.Player(opponent))))
            .error shouldBe null
        d.bothPass()
        withClue("the copied \"damage equal to its power\" uses the Lich's 4") {
            d.getLifeTotal(opponent) shouldBe 16
        }
    }

    test("casting a different creature spell doesn't fire the trigger") {
        val d = driver()
        val lich = d.lich()
        val goblin = d.putCardInGraveyard(d.player1, "Spikeshot Goblin")
        d.activateLich(lich, goblin)

        val bears = d.putCardInHand(d.player1, "Grizzly Bears")
        d.giveMana(d.player1, Color.GREEN, 2)
        d.submit(CastSpell(d.player1, bears)).error shouldBe null
        d.state.stack shouldBe listOf(bears)
        d.bothPass()
        d.gainedAbilityIds(lich) shouldBe emptyList()
    }

    test("the same card cast after leaving the graveyard another way is a new object") {
        val d = driver()
        val lich = d.lich()
        val goblin = d.putCardInGraveyard(d.player1, "Spikeshot Goblin")
        d.activateLich(lich, goblin)

        // The Goblin returns to its owner's hand: a new object (CR 400.7).
        d.replaceState(
            d.state.removeFromZone(ZoneKey(d.player1, Zone.GRAVEYARD), goblin)
                .addToZone(ZoneKey(d.player1, Zone.HAND), goblin)
        )
        d.giveMana(d.player1, Color.RED, 1)
        d.giveColorlessMana(d.player1, 2)
        d.submit(CastSpell(d.player1, goblin)).error shouldBe null
        withClue("only the creature spell is on the stack — no Lich trigger") {
            d.state.stack shouldBe listOf(goblin)
        }
        d.bothPass()
        d.gainedAbilityIds(lich) shouldBe emptyList()
    }

    test("without the cast, the permission ends with the turn and the Lich gains nothing") {
        val d = driver()
        val lich = d.lich()
        val goblin = d.putCardInGraveyard(d.player1, "Spikeshot Goblin")
        d.activateLich(lich, goblin)
        d.gainedAbilityIds(lich) shouldBe emptyList()

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.getOpponent(d.player1))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.player1)
        d.giveMana(d.player1, Color.RED, 1)
        d.giveColorlessMana(d.player1, 2)
        withClue("\"this turn\" — next turn the Goblin can't be cast from the graveyard") {
            d.submit(CastSpell(d.player1, goblin)).error shouldNotBe null
        }
    }
})
