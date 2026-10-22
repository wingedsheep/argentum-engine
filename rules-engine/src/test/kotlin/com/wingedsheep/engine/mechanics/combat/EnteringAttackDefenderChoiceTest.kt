package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.ProtectorComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BeingAttackedComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * CR 508.4: "If a creature is put onto the battlefield attacking, its controller chooses which
 * defending player, planeswalker a defending player controls, or battle a defending player protects
 * it's attacking as it enters the battlefield (unless the effect that put it onto the battlefield
 * specifies what it's attacking)." — [EnteringAttackDefender.CONTROLLER_CHOOSES] on a token maker.
 *
 * Also CR 506.3b (a creature put onto the battlefield attacking under a player who isn't attacking
 * never attacks) and that the default [EnteringAttackDefender.JOIN_SOURCE] is untouched.
 *
 * A named `attackedPlayer` with CONTROLLER_CHOOSES is "attacking that player or a planeswalker they
 * control" — the choice narrows to that player and their planeswalkers (Adeline, Resplendent Cathar).
 */
class EnteringAttackDefenderChoiceTest : FunSpec({

    fun soldiers(choice: EnteringAttackDefender) = Effects.CreateToken(
        power = 1, toughness = 1, colors = setOf(Color.RED, Color.WHITE),
        creatureTypes = setOf("Soldier"), count = 2,
        tapped = true, attacking = true, attackDefender = choice,
    )

    val captain = card("Test Choosing Captain") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Human Soldier"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.self.attacks()
            effect = soldiers(EnteringAttackDefender.CONTROLLER_CHOOSES)
        }
    }

    val legacyCaptain = card("Test Joining Captain") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Human Soldier"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.self.attacks()
            effect = soldiers(EnteringAttackDefender.JOIN_SOURCE)
        }
    }

    // "Create two tapped and attacking tokens that are copies of this creature" — the copy maker.
    val copyingCaptain = card("Test Copying Captain") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Human Soldier"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.self.attacks()
            effect = Effects.CreateTokenCopyOfTarget(
                target = EffectTarget.Self, count = 2, tapped = true, attacking = true,
                attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
            )
        }
    }

    // "Whenever you attack, for each opponent, create a 1/1 token that's tapped and attacking that
    // player or a planeswalker they control." — the loop rebinds Controller to the visited opponent.
    val rallyingCathar = card("Test Rallying Cathar") {
        manaCost = "{1}{W}{W}"
        typeLine = "Creature — Human Knight"
        power = 2
        toughness = 2
        triggeredAbility {
            trigger = Triggers.you.attacks()
            effect = Effects.ForEachPlayer(
                Player.EachOpponent,
                Effects.CreateToken(
                    power = 1, toughness = 1, colors = setOf(Color.WHITE), creatureTypes = setOf("Human"),
                    controller = EffectTarget.PlayerRef(Player.ControllerOfSource),
                    tapped = true, attacking = true,
                    attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
                    attackedPlayer = EffectTarget.Controller,
                ),
            )
        }
    }

    val walker = card("Test Defending Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = ""
    }

    val bulwark = card("Test Defending Bulwark") {
        manaCost = "{3}"
        typeLine = "Battle"
        startingDefense = 3
        oracleText = ""
    }

    fun newDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(captain, legacyCaptain, copyingCaptain, rallyingCathar, walker, bulwark))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.ready(name: String): EntityId =
        putCreatureOnBattlefield(player1, name).also { removeSummoningSickness(it) }

    fun GameTestDriver.walkerFor(player: EntityId, loyalty: Int = 3): EntityId =
        putPermanentOnBattlefield(player, "Test Defending Walker").also { pw ->
            replaceState(state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, loyalty)) })
        }

    fun GameTestDriver.battleProtectedBy(player: EntityId): EntityId =
        putPermanentOnBattlefield(player, "Test Defending Bulwark").also { battle ->
            replaceState(state.updateEntity(battle) {
                it.with(CountersComponent().withAdded(CounterType.DEFENSE, 3)).with(ProtectorComponent(player))
            })
        }

    fun GameTestDriver.resolveUntilDecision() {
        var guard = 0
        while (pendingDecision == null && stackSize > 0 && guard++ < 50) passPriority(priorityPlayer!!)
    }

    fun GameTestDriver.attackWith(attacker: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, listOf(attacker), player2).error shouldBe null
        resolveUntilDecision()
    }

    fun GameTestDriver.soldierTokens(): List<EntityId> =
        getCreatures(player1).filter { state.getEntity(it)?.has<TokenComponent>() == true }

    fun GameTestDriver.attackedBy(id: EntityId): EntityId? = state.getEntity(id)?.get<AttackingComponent>()?.defenderId

    test("only the defending player is legal: no question, both tokens attack that player") {
        val d = newDriver()
        d.attackWith(d.ready("Test Choosing Captain"))

        d.pendingDecision shouldBe null
        val tokens = d.soldierTokens()
        tokens shouldHaveSize 2
        tokens.forEach {
            d.attackedBy(it) shouldBe d.player2
            d.state.getEntity(it)!!.has<TappedComponent>() shouldBe true
        }
    }

    test("one choice per token, before any token exists; two tokens may attack different defenders") {
        val d = newDriver()
        val ownWalker = d.walkerFor(d.player1)
        val theirWalker = d.walkerFor(d.player2)
        d.attackWith(d.ready("Test Choosing Captain"))

        val first = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        first.playerId shouldBe d.player1
        withClue("the opponent and their planeswalker — never yourself or your own planeswalker") {
            first.legalTargets[0]!! shouldContainExactly listOf(d.player2, theirWalker)
        }
        withClue("chosen as it enters: nothing is on the battlefield yet") { d.soldierTokens() shouldHaveSize 0 }

        d.submitTargetSelection(d.player1, listOf(theirWalker)).error shouldBe null
        d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.soldierTokens() shouldHaveSize 0
        d.submitTargetSelection(d.player1, listOf(d.player2)).error shouldBe null

        d.pendingDecision shouldBe null
        val tokens = d.soldierTokens()
        tokens shouldHaveSize 2
        tokens.map { d.attackedBy(it) } shouldContainExactly listOf(theirWalker, d.player2)
        withClue("the attacked planeswalker is stamped as being attacked (CR 506.4)") {
            d.state.getEntity(theirWalker)!!.has<BeingAttackedComponent>() shouldBe true
        }
        tokens.forEach {
            withClue("a planeswalker's controller is the defending player") {
                d.state.getEntity(it)!!.get<AttackingComponent>()!!.defendingPlayerId shouldBe d.player2
            }
        }
    }

    test("an illegal answer is rejected and the question stays open") {
        val d = newDriver()
        val ownWalker = d.walkerFor(d.player1)
        d.walkerFor(d.player2)
        d.attackWith(d.ready("Test Choosing Captain"))

        d.submitTargetSelection(d.player1, listOf(ownWalker)).error shouldNotBe null
        d.submitTargetSelection(d.player1, listOf(d.player1)).error shouldNotBe null
        d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
    }

    test("a battle the defending player protects can be chosen") {
        val d = newDriver()
        val battle = d.battleProtectedBy(d.player2)
        d.attackWith(d.ready("Test Choosing Captain"))

        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        decision.legalTargets[0]!! shouldContainExactly listOf(d.player2, battle)
        d.submitTargetSelection(d.player1, listOf(battle)).error shouldBe null
        d.submitTargetSelection(d.player1, listOf(battle)).error shouldBe null

        d.soldierTokens().forEach {
            d.attackedBy(it) shouldBe battle
            withClue("a battle defends on behalf of its protector (CR 310.9d)") {
                d.state.getEntity(it)!!.get<AttackingComponent>()!!.defendingPlayerId shouldBe d.player2
            }
        }
    }

    test("combat damage goes to the chosen defender") {
        val d = newDriver()
        val theirWalker = d.walkerFor(d.player2, loyalty = 3)
        d.attackWith(d.ready("Test Choosing Captain"))
        d.submitTargetSelection(d.player1, listOf(theirWalker))
        d.submitTargetSelection(d.player1, listOf(d.player2))

        d.passPriorityUntil(Step.END_COMBAT)

        withClue("one 1/1 Soldier hit the planeswalker") {
            d.state.getEntity(theirWalker)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 2
        }
        withClue("the 2/2 captain and the other Soldier hit the player") { d.getLifeTotal(d.player2) shouldBe 17 }
    }

    test("token copies: one choice per copy before any copy exists, each attacking its own defender") {
        val d = newDriver()
        val theirWalker = d.walkerFor(d.player2)
        d.attackWith(d.ready("Test Copying Captain"))

        val first = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        first.legalTargets[0]!! shouldContainExactly listOf(d.player2, theirWalker)
        withClue("chosen as it enters: no copy exists yet") { d.soldierTokens() shouldHaveSize 0 }
        d.submitTargetSelection(d.player1, listOf(theirWalker)).error shouldBe null
        d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submitTargetSelection(d.player1, listOf(d.player2)).error shouldBe null

        d.pendingDecision shouldBe null
        val copies = d.soldierTokens()
        copies shouldHaveSize 2
        copies.map { d.attackedBy(it) } shouldContainExactly listOf(theirWalker, d.player2)
        copies.forEach {
            d.state.getEntity(it)!!.has<TappedComponent>() shouldBe true
            d.state.getEntity(it)!!.get<AttackingComponent>()!!.defendingPlayerId shouldBe d.player2
        }
    }

    test("token copies with only the defending player legal: no question") {
        val d = newDriver()
        d.attackWith(d.ready("Test Copying Captain"))

        d.pendingDecision shouldBe null
        d.soldierTokens().map { d.attackedBy(it) } shouldContainExactly listOf(d.player2, d.player2)
    }

    test("CONTROLLER_CHOOSES and a named defender need attacking") {
        shouldThrow<IllegalArgumentException> {
            CreateTokenCopyOfTargetEffect(target = EffectTarget.Self, attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES)
        }
        shouldThrow<IllegalArgumentException> {
            CreateTokenEffect(
                power = 1, toughness = 1, colors = emptySet(), creatureTypes = setOf("Human"),
                attackedPlayer = EffectTarget.Controller,
            )
        }
    }

    fun multiplayer(): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(captain, legacyCaptain, copyingCaptain, rallyingCathar, walker, bulwark))
        val ids = d.initMultiplayer(List(3) { Deck.of("Forest" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d to ids
    }

    test("for each opponent: one token attacking that opponent, under the source's controller, no question") {
        val (d, p) = multiplayer()
        val (p1, p2, p3) = p
        val cathar = d.putCreatureOnBattlefield(p1, "Test Rallying Cathar").also { d.removeSummoningSickness(it) }
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(cathar), p2).error shouldBe null
        d.resolveUntilDecision()

        d.pendingDecision shouldBe null
        val tokens = d.getCreatures(p1).filter { d.state.getEntity(it)?.has<TokenComponent>() == true }
        withClue("both tokens are the attacking player's, not the visited opponents'") { tokens shouldHaveSize 2 }
        tokens.map { d.attackedBy(it) } shouldContainExactlyInAnyOrder listOf(p2, p3)
        withClue("an opponent the source didn't attack is attacked too") { d.attackedBy(cathar) shouldBe p2 }
    }

    test("the choice narrows to that opponent and their planeswalkers — never another's, never a battle") {
        val (d, p) = multiplayer()
        val (p1, p2, p3) = p
        val p2Walker = d.walkerFor(p2)
        d.walkerFor(p3)
        d.battleProtectedBy(p3)
        val cathar = d.putCreatureOnBattlefield(p1, "Test Rallying Cathar").also { d.removeSummoningSickness(it) }
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(cathar), p2).error shouldBe null
        d.resolveUntilDecision()

        val first = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        first.playerId shouldBe p1
        withClue("APNAP visits p2 first: p2 or p2's planeswalker") {
            first.legalTargets[0]!! shouldContainExactly listOf(p2, p2Walker)
        }
        withClue("the prompt doesn't offer battles") { first.prompt.contains("battle") shouldBe false }
        d.submitTargetSelection(p1, listOf(p2Walker)).error shouldBe null

        val second = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        withClue("p3's battle is not 'a planeswalker they control'") {
            second.legalTargets[0]!!.filter { it == p3 } shouldHaveSize 1
            second.legalTargets[0]!! shouldHaveSize 2
        }
        withClue("another opponent's planeswalker is not a legal answer") {
            d.submitTargetSelection(p1, listOf(p2Walker)).error shouldNotBe null
        }
        d.submitTargetSelection(p1, listOf(p3)).error shouldBe null

        d.pendingDecision shouldBe null
        val tokens = d.getCreatures(p1).filter { d.state.getEntity(it)?.has<TokenComponent>() == true }
        tokens.map { d.attackedBy(it) } shouldContainExactlyInAnyOrder listOf(p2Walker, p3)
    }

    test("each opponent's question starts fresh: answers for one never carry to the next") {
        val (d, p) = multiplayer()
        val (p1, p2, p3) = p
        val p2Walker = d.walkerFor(p2)
        val p3Walker = d.walkerFor(p3)
        val cathar = d.putCreatureOnBattlefield(p1, "Test Rallying Cathar").also { d.removeSummoningSickness(it) }
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(cathar), p2).error shouldBe null
        d.resolveUntilDecision()

        d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>().legalTargets[0]!! shouldContainExactly listOf(p2, p2Walker)
        d.submitTargetSelection(p1, listOf(p2Walker)).error shouldBe null
        withClue("p3 is asked too, among p3 and p3's planeswalker") {
            d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>().legalTargets[0]!! shouldContainExactly listOf(p3, p3Walker)
        }
        d.submitTargetSelection(p1, listOf(p3)).error shouldBe null

        d.pendingDecision shouldBe null
        val tokens = d.getCreatures(p1).filter { d.state.getEntity(it)?.has<TokenComponent>() == true }
        withClue("exactly one token per opponent — none created twice across the pause") {
            tokens.map { d.attackedBy(it) } shouldContainExactlyInAnyOrder listOf(p2Walker, p3)
        }
    }

    test("a named player alone (JOIN_SOURCE) is attacked exactly — no question, never their planeswalker") {
        val (d, p) = multiplayer()
        val (p1, _, p3) = p
        d.walkerFor(p3)
        d.passPriorityUntil(Step.BEGIN_COMBAT)
        val result = d.services.effectExecutorRegistry.execute(
            d.state,
            Effects.CreateToken(
                power = 1, toughness = 1, colors = setOf(Color.WHITE), creatureTypes = setOf("Human"),
                tapped = true, attacking = true, attackedPlayer = EffectTarget.ContextTarget(0),
            ),
            EffectContext(sourceId = null, controllerId = p1, targets = listOf(ChosenTarget.Player(p3)))
        )
        d.replaceState(result.state)
        d.pendingDecision shouldBe null
        d.getCreatures(p1).filter { d.state.getEntity(it)?.has<TokenComponent>() == true }
            .map { d.attackedBy(it) } shouldContainExactly listOf(p3)
    }

    test("the default (JOIN_SOURCE) is unchanged: no question, the tokens join the source's attack") {
        val d = newDriver()
        d.walkerFor(d.player2)
        d.attackWith(d.ready("Test Joining Captain"))

        d.pendingDecision shouldBe null
        d.soldierTokens().map { d.attackedBy(it) } shouldContainExactly listOf(d.player2, d.player2)
    }

    test("Two-Headed Giant: the active player's teammate is an attacking player too (CR 805.10a)") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(captain, legacyCaptain, copyingCaptain, rallyingCathar, walker, bulwark))
        val p = d.initMultiplayer(
            List(4) { Deck.of("Forest" to 40) },
            format = com.wingedsheep.sdk.core.Format.TwoHeadedGiant(),
            teams = listOf(listOf(0, 1), listOf(2, 3)),
        )
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = d.activePlayer!!
        val teammate = d.state.sharedTurnTeam(active).single { it != active }
        val opponents = d.state.getOpponents(teammate)
        val theirWalker = d.walkerFor(opponents.first())
        d.passPriorityUntil(Step.BEGIN_COMBAT)
        p shouldHaveSize 4

        val result = d.services.effectExecutorRegistry.execute(
            d.state, soldiers(EnteringAttackDefender.CONTROLLER_CHOOSES),
            EffectContext(sourceId = null, controllerId = teammate)
        )
        d.replaceState(result.state)

        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        decision.playerId shouldBe teammate
        decision.legalTargets[0]!! shouldContainExactly d.state.turnOrder.filter { it in opponents } + theirWalker
    }

    test("outside combat nothing can be attacked: the tokens enter but aren't attacking (CR 506.3b)") {
        val d = newDriver()
        d.walkerFor(d.player2)
        val result = d.services.effectExecutorRegistry.execute(
            d.state, soldiers(EnteringAttackDefender.CONTROLLER_CHOOSES),
            EffectContext(sourceId = null, controllerId = d.player1)
        )
        d.replaceState(result.state)

        d.pendingDecision shouldBe null
        val tokens = d.soldierTokens()
        tokens shouldHaveSize 2
        tokens.forEach { d.attackedBy(it) shouldBe null }
    }
})
