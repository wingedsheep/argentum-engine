package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.cmr.cards.WarRoom
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * War Room — "{T}: Add {C}." and "{3}, {T}, Pay life equal to the number of colors in your
 * commanders' color identity: Draw a card." The life is priced from the activator's commanders;
 * with too little life, or no commander at all (ruling), the draw can't be activated.
 */
class WarRoomScenarioTest : FunSpec({

    val azoriusCommander = CardDefinition.creature(
        name = "War Room Test Azorius Commander",
        manaCost = ManaCost.parse("{1}{W}{U}"),
        subtypes = setOf(Subtype("Human")),
        power = 2,
        toughness = 2,
        supertypes = setOf(Supertype.LEGENDARY),
    )
    val manaAbility = WarRoom.activatedAbilities[0].id
    val drawAbility = WarRoom.activatedAbilities[1].id

    fun commanderGame(): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + WarRoom + azoriusCommander)
        val players = d.initMultiplayer(
            decks = listOf(Deck.of("Island" to 99), Deck.of("Island" to 99)),
            format = Format.Commander(),
            commanders = listOf(azoriusCommander.name, azoriusCommander.name),
            skipMulligans = true,
            startingPlayer = 0,
        )
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d to players[0]
    }

    fun GameTestDriver.canDraw(player: EntityId, room: EntityId) = legalActions(player).any {
        val action = it.action as? ActivateAbility
        action?.sourceId == room && action.abilityId == drawAbility && it.affordable
    }

    test("{T}: Add {C}") {
        val (d, me) = commanderGame()
        val room = d.putLandOnBattlefield(me, WarRoom.name)
        d.submitSuccess(ActivateAbility(me, room, manaAbility))
        d.state.getEntity(me)!!.get<ManaPoolComponent>()!!.colorless shouldBe 1
    }

    test("with a two-color commander, drawing costs {3} and 2 life") {
        val (d, me) = commanderGame()
        d.setLifeTotal(me, 10)
        val room = d.putLandOnBattlefield(me, WarRoom.name)
        d.giveColorlessMana(me, 3)
        val hand = d.getHandSize(me)

        d.canDraw(me, room) shouldBe true
        d.submitSuccess(ActivateAbility(me, room, drawAbility))
        d.getLifeTotal(me) shouldBe 8
        d.isTapped(room) shouldBe true
        d.bothPass()
        d.getHandSize(me) shouldBe hand + 1
    }

    test("with less life than the colors in your commander's identity, it can't be activated") {
        val (d, me) = commanderGame()
        d.setLifeTotal(me, 1)
        val room = d.putLandOnBattlefield(me, WarRoom.name)
        d.giveColorlessMana(me, 3)

        d.canDraw(me, room) shouldBe false
        d.submit(ActivateAbility(me, room, drawAbility)).error shouldNotBe null
        d.getLifeTotal(me) shouldBe 1
        d.isTapped(room) shouldBe false
    }

    test("without a commander, the draw ability can't be activated at all") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + WarRoom)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.player1
        val room = d.putLandOnBattlefield(me, WarRoom.name)
        d.giveColorlessMana(me, 3)

        d.canDraw(me, room) shouldBe false
        d.submit(ActivateAbility(me, room, drawAbility)).error shouldNotBe null
        d.getLifeTotal(me) shouldBe 20
    }
})
