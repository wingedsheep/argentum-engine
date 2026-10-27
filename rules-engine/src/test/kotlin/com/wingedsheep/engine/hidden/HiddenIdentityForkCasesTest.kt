package com.wingedsheep.engine.hidden

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.HiddenIdentityForkChecker
import com.wingedsheep.engine.support.HiddenIdentityForkChecker.End
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.OnEnterRun
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Deterministic forks through [HiddenIdentityForkChecker], one scripted state per case, just
 * before the question a site may or may not ask.
 *
 * These are the enforcement of #3340's rule — whether a player is asked a question may depend only
 * on information every player has — and of #2780's. Each #3340 group adds its own case with its
 * fix: a fork that differs on main and agrees once the group is fixed. The cases here hold on main
 * and pin the checker itself. The leaky ones use test-only cards built to leak, so that each of the
 * checker's rules is needed by at least one case; the rest show it stays quiet where nothing leaks.
 * Every case asserts how its replay ended and how many actions it checked, so none passes by
 * stopping early.
 */
class HiddenIdentityForkCasesTest : ScenarioTestBase() {
    private val checker = HiddenIdentityForkChecker(cardRegistry)

    private val goblinInHand = Exists(Player.You, Zone.HAND, GameObjectFilter.Any.withSubtype("Goblin"))
    private fun peek() = GatherCardsEffect(source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)), storeAs = "peek")
    private val peekIsGoblin = Conditions.CollectionContainsMatch("peek", GameObjectFilter.Any.withSubtype("Goblin"))

    /** As it enters, with a Goblin in hand: reveal the hand, then you may gain 1 life. */
    private val herald = card("Leaky Test Herald") {
        typeLine = "Land"
        replacementEffect(
            OnEnterRun(Effects.If(goblinInHand, Effects.RevealHand(EffectTarget.Controller) then Effects.May(Effects.GainLife(1))))
        )
    }

    /** As it enters, with a Goblin in hand: gain 1 life — a public change its controller's knowledge explains. */
    private val signal = card("Leaky Test Signal") {
        typeLine = "Land"
        replacementEffect(OnEnterRun(Effects.If(goblinInHand, Effects.GainLife(1))))
    }

    /**
     * Scry 1 and draw, then with a Goblin in hand you may gain 1 life. The condition reads the hand,
     * which holds no Goblin until it resolves, so its stack text — which collapses conditions
     * against the current state — is the same in both worlds.
     */
    private val glimpse = card("Leaky Test Glimpse") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell { effect = Patterns.Library.scry(1) then Effects.DrawCards(1) then Effects.If(goblinInHand, Effects.May(Effects.GainLife(1))) }
    }

    /** Look at the top card privately; unless it's a Goblin, mill it. */
    private val tell = card("Leaky Test Tell") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell {
            effect = CompositeEffect(
                listOf(
                    peek(),
                    Effects.If(Conditions.Not(peekIsGoblin), MoveCollectionEffect(from = "peek", destination = CardDestination.ToZone(Zone.GRAVEYARD))),
                )
            )
        }
    }

    /** Gain 1 life if the top card is a Goblin, then show the top card to its owner in a selection. */
    private val peekCard = card("Leaky Test Peek") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell {
            effect = CompositeEffect(
                listOf(
                    peek(),
                    Effects.If(peekIsGoblin, Effects.GainLife(1)),
                    SelectFromCollectionEffect(from = "peek", selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)), storeSelected = "kept"),
                )
            )
        }
    }

    /** "You may discard a creature card. When you do, you gain 3 life." */
    private val offering = card("Hidden Test Offering") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.ReflexiveTrigger(
                action = Patterns.Hand.discardCards(1, filter = GameObjectFilter.Creature),
                optional = true,
                descriptionOverride = "You may discard a creature card. When you do, you gain 3 life.",
            ) { effect = Effects.GainLife(3) }
        }
    }

    /** "Sacrifice an artifact or discard a nonland card." */
    private val ultimatum = card("Hidden Test Ultimatum") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.ChooseAction(
                listOf(
                    EffectChoice(
                        "Sacrifice an artifact",
                        Effects.SacrificeOwn(filter = GameObjectFilter.Artifact),
                        FeasibilityCheck.ControlsPermanentMatching(GameObjectFilter.Artifact),
                    ),
                    EffectChoice(
                        "Discard a nonland card",
                        Patterns.Hand.discardCards(1, filter = GameObjectFilter.Nonland),
                        FeasibilityCheck.HasCardsInZone(Zone.HAND, GameObjectFilter.Nonland),
                    ),
                )
            )
        }
    }

    init {
        cardRegistry.register(listOf(herald, signal, glimpse, tell, peekCard, offering, ultimatum))
    }

    private fun cardIn(state: GameState, owner: EntityId, name: String, inHand: Boolean = true): EntityId =
        (if (inHand) state.getHand(owner) else state.getLibrary(owner))
            .first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    /** P2's turn, main phase, with [hand] in hand, [islands] Islands out and [library] top first. */
    private fun p2Turn(hand: List<String>, library: List<String> = emptyList(), islands: Int = 0): TestGame {
        var builder = scenario().withPlayers()
        hand.forEach { builder = builder.withCardInHand(2, it) }
        library.forEach { builder = builder.withCardInLibrary(2, it) }
        if (islands > 0) builder = builder.withLandsOnBattlefield(2, "Island", islands)
        return builder.withActivePlayer(2).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
    }

    /** Scripts the real game: does [action], then passes priority until a question is pending or the stack is empty. */
    private fun MutableList<GameAction>.play(game: TestGame, action: GameAction) {
        add(action)
        game.execute(action)
        while (game.state.pendingDecision == null && game.state.stack.isNotEmpty()) {
            add(PassPriority(game.state.priorityPlayerId!!))
            game.execute(last())
        }
    }

    /** Answers the pending selection — a scry, say — by choosing nothing. */
    private fun MutableList<GameAction>.chooseNothing(game: TestGame) =
        play(game, SubmitDecision(game.player2Id, CardsSelectedResponse(game.state.pendingDecision!!.id, emptyList())))

    init {
        test("a disclosure the same action makes can't excuse the question it asks") {
            // The Herald reveals the hand and asks only when it holds a Goblin.
            val game = p2Turn(listOf("Leaky Test Herald", "Boggart Mob"))
            val start = game.state
            val actions = mutableListOf<GameAction>().apply { play(game, PlayLand(game.player2Id, cardIn(start, game.player2Id, "Leaky Test Herald"))) }
            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()

            val outcome = checker.checkZoneFork(start, cardIn(start, game.player2Id, "Boggart Mob"), cardRegistry.requireCard("Mountain"), game.player1Id, actions)

            outcome.end shouldBe End.Leaked(1)
            outcome.checked shouldBe 0
            outcome.leak.orEmpty() shouldContain "question header differs"
        }

        test("a disclosure in the swapped world only doesn't end the header checks") {
            // Tell mills the top card unless it's a Goblin: only the swapped world shows it. The
            // Glimpse then asks only in the real world, which still holds the Goblin.
            val game = p2Turn(listOf("Leaky Test Tell", "Leaky Test Glimpse"), listOf("Boggart Mob", "Swamp", "Swamp"), islands = 2)
            val start = game.state
            val actions = mutableListOf<GameAction>().apply {
                play(game, CastSpell(game.player2Id, cardIn(start, game.player2Id, "Leaky Test Tell")))
                play(game, CastSpell(game.player2Id, cardIn(start, game.player2Id, "Leaky Test Glimpse")))
                chooseNothing(game)
            }
            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()

            val outcome = checker.checkZoneFork(start, start.getLibrary(game.player2Id).first(), cardRegistry.requireCard("Mountain"), game.player1Id, actions)

            outcome.end shouldBe End.Leaked(actions.size)
            outcome.checked shouldBe actions.size - 1
            outcome.leak.orEmpty() shouldContain "question header differs"
        }

        test("headers are still checked after an informed actor's choice stops the view comparison") {
            // The Signal changes a life total only with a Goblin in hand, which its controller
            // knows; the Glimpse then asks only in the real world.
            val game = p2Turn(listOf("Leaky Test Signal", "Leaky Test Glimpse", "Boggart Mob"), listOf("Swamp", "Swamp"), islands = 1)
            val start = game.state
            val actions = mutableListOf<GameAction>().apply {
                play(game, PlayLand(game.player2Id, cardIn(start, game.player2Id, "Leaky Test Signal")))
                play(game, CastSpell(game.player2Id, cardIn(start, game.player2Id, "Leaky Test Glimpse")))
                chooseNothing(game)
            }
            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()

            val outcome = checker.checkZoneFork(start, cardIn(start, game.player2Id, "Boggart Mob"), cardRegistry.requireCard("Mountain"), game.player1Id, actions)

            outcome.end shouldBe End.Leaked(actions.size)
            outcome.checked shouldBe actions.size - 1
            outcome.dataStopped.shouldNotBeNull()
            outcome.leak.orEmpty() shouldContain "question header differs"
        }

        test("a card shown to the player answering a question excuses nothing another player sees") {
            // The Peek gains life on a Goblin on top, then shows that card to its owner.
            val game = p2Turn(listOf("Leaky Test Peek"), listOf("Boggart Mob", "Swamp"), islands = 1)
            val start = game.state
            val actions = mutableListOf<GameAction>().apply { play(game, CastSpell(game.player2Id, cardIn(start, game.player2Id, "Leaky Test Peek"))) }
            game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()

            val outcome = checker.checkZoneFork(start, start.getLibrary(game.player2Id).first(), cardRegistry.requireCard("Mountain"), game.player1Id, actions)

            outcome.end shouldBe End.Leaked(actions.size)
            outcome.checked shouldBe actions.size
            outcome.leak.orEmpty() shouldContain "for ${game.player1Id}: state"
        }

        test("an action refused in one world only ends the replay as a refusal") {
            val game = p2Turn(listOf("Forest"))
            val start = game.state
            val forest = cardIn(start, game.player2Id, "Forest")

            val outcome = checker.checkZoneFork(start, forest, cardRegistry.requireCard("Grizzly Bears"), game.player1Id, listOf(PlayLand(game.player2Id, forest)))

            outcome.leak.shouldBeNull()
            outcome.end shouldBe End.Refused(1)
            outcome.checked shouldBe 0
        }

        test("a card played in both worlds is public, and what follows may depend on it") {
            // Auntie's Hovel asks about a Goblin in hand; the Mountain it is forked into doesn't.
            val game = p2Turn(listOf("Auntie's Hovel", "Boggart Mob"))
            val start = game.state
            val hovel = cardIn(start, game.player2Id, "Auntie's Hovel")

            val outcome = checker.checkZoneFork(start, hovel, cardRegistry.requireCard("Mountain"), game.player1Id, listOf(PlayLand(game.player2Id, hovel)))

            outcome.leak.shouldBeNull()
            outcome.end shouldBe End.PublicToAll(1)
            outcome.checked shouldBe 0
        }

        test("a skip on an empty hand is the same whatever the library holds") {
            // Auntie's Hovel reads the hand for a Goblin; with the Hovel played, the hand is empty,
            // which every player knows. The top of the library is forked into a Goblin.
            val game = p2Turn(listOf("Auntie's Hovel"), listOf("Mountain", "Swamp"))
            val start = game.state
            val hovel = cardIn(start, game.player2Id, "Auntie's Hovel")

            val outcome = checker.checkZoneFork(
                start, start.getLibrary(game.player2Id).first(), cardRegistry.requireCard("Boggart Mob"), game.player1Id,
                listOf(PlayLand(game.player2Id, hovel), PassPriority(game.player2Id)),
            )

            outcome.leak.shouldBeNull()
            outcome.end shouldBe End.Completed
            outcome.checked shouldBe 2
        }

        test("a question asked in both worlds agrees when the fork changes nothing it reads") {
            // A Goblin is in hand in both worlds; only another, non-Goblin card is forked.
            val game = p2Turn(listOf("Auntie's Hovel", "Boggart Mob", "Mountain"))
            val start = game.state
            val hovel = cardIn(start, game.player2Id, "Auntie's Hovel")
            game.execute(PlayLand(game.player2Id, hovel)).state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()

            val outcome = checker.checkZoneFork(start, cardIn(start, game.player2Id, "Mountain"), cardRegistry.requireCard("Island"), game.player1Id, listOf(PlayLand(game.player2Id, hovel)))

            outcome.leak.shouldBeNull()
            outcome.end shouldBe End.Completed
            outcome.checked shouldBe 1
        }

        test("an order fork of the owner's own library agrees when nothing reads the order") {
            val game = scenario()
                .withPlayers()
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Boggart Mob")
                .withCardInLibrary(1, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val start = game.state

            val outcome = checker.checkOrderFork(
                start,
                cardIn(start, game.player1Id, "Mountain", inHand = false),
                cardIn(start, game.player1Id, "Boggart Mob", inHand = false),
                game.player1Id,
                listOf(PassPriority(game.player1Id), PassPriority(game.player2Id)),
            )

            outcome.leak.shouldBeNull()
            outcome.end shouldBe End.Completed
            outcome.checked shouldBe 2
        }

        // #3340 group E: whether cards put back on a library ask for their order can't depend on
        // whether they are copies of one card.
        test("keeping two cards on top after a scry asks for their order whether or not they are copies") {
            // P1 keeps both scried cards on top. P2 can't tell two Forests from a Forest and an
            // Island, so the reorder question must be asked for both.
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Preordain")
                .withCardOnBattlefield(1, "Island")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val start = game.state
            val actions = mutableListOf<GameAction>().apply {
                play(game, CastSpell(game.player1Id, cardIn(start, game.player1Id, "Preordain")))
                game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                play(game, SubmitDecision(game.player1Id, CardsSelectedResponse(game.state.pendingDecision!!.id, emptyList())))
            }

            val outcome = checker.checkZoneFork(start, start.getLibrary(game.player1Id)[1], cardRegistry.requireCard("Island"), game.player2Id, actions)

            outcome.end shouldBe End.Completed
            outcome.checked shouldBe actions.size
            outcome.leak.shouldBeNull()
            game.state.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
        }

        // #3340 group D: a face-down permanent is read by its face-down characteristics, never by
        // the card underneath.
        test("a face-down permanent is asked about as a creature whatever card it is") {
            // P2's Imagecrafter targets P1's face-down permanent. It is a 2/2 creature whether the
            // card is a Grizzly Bears or a Forest, so the creature-type question is asked in both.
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(2, "Imagecrafter")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val faceDown = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(faceDown) { it.with(FaceDownComponent) }
            val start = game.state
            val imagecrafter = game.findPermanent("Imagecrafter")!!
            val ability = cardRegistry.requireCard("Imagecrafter").activatedAbilities.single().id
            val actions = mutableListOf<GameAction>().apply {
                play(game, ActivateAbility(game.player2Id, imagecrafter, ability, targets = listOf(ChosenTarget.Permanent(faceDown))))
            }
            game.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()

            val outcome = checker.checkFaceDownFork(start, faceDown, cardRegistry.requireCard("Forest"), game.player2Id, actions)

            outcome.leak.shouldBeNull()
            outcome.end shouldBe End.Completed
            outcome.checked shouldBe actions.size
        }

        // #3340 group B: an optional action or cost that needs a matching card in hand is asked
        // whether or not the hand holds one. Each case forks the one matching card into a card
        // that doesn't match, and expects the same question in both worlds.
        fun groupB(
            name: String,
            hand: List<String>,
            lands: Pair<String, Int>,
            forked: String,
            into: String,
            setUp: ScenarioBuilder.() -> ScenarioBuilder = { this },
            script: MutableList<GameAction>.(TestGame, GameState) -> Unit,
        ) = test(name) {
            var builder = scenario().withPlayers()
            hand.forEach { builder = builder.withCardInHand(2, it) }
            val game = builder.withLandsOnBattlefield(2, lands.first, lands.second).setUp()
                .withActivePlayer(2).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val start = game.state
            val actions = mutableListOf<GameAction>().apply { script(game, start) }
            game.state.pendingDecision.shouldNotBeNull().playerId shouldBe game.player2Id

            val outcome = checker.checkZoneFork(start, cardIn(start, game.player2Id, forked), cardRegistry.requireCard(into), game.player1Id, actions)

            outcome.leak.shouldBeNull()
            outcome.end shouldBe End.Completed
            outcome.checked shouldBe actions.size
        }

        groupB("a may-reveal from hand asks whether or not the hand holds a match",
            hand = listOf("Auntie's Hovel", "Boggart Mob"), lands = "Swamp" to 1, forked = "Boggart Mob", into = "Mountain",
        ) { game, start -> play(game, PlayLand(game.player2Id, cardIn(start, game.player2Id, "Auntie's Hovel"))) }

        groupB("behold asks whether or not the hand holds a match",
            hand = listOf("Sarkhan, Dragon Ascendant", "Shivan Dragon"), lands = "Mountain" to 2, forked = "Shivan Dragon", into = "Mountain",
        ) { game, start -> play(game, CastSpell(game.player2Id, cardIn(start, game.player2Id, "Sarkhan, Dragon Ascendant"))) }

        groupB("unless-you-discard a filtered card asks whether or not the hand holds a match",
            hand = listOf("Mercenary Knight", "Grizzly Bears"), lands = "Swamp" to 3, forked = "Grizzly Bears", into = "Swamp",
        ) { game, start -> play(game, CastSpell(game.player2Id, cardIn(start, game.player2Id, "Mercenary Knight"))) }

        groupB("amplify asks whether or not the hand holds a match",
            hand = listOf("Feral Throwback", "Krosan Tusker"), lands = "Forest" to 6, forked = "Krosan Tusker", into = "Forest",
        ) { game, start -> play(game, CastSpell(game.player2Id, cardIn(start, game.player2Id, "Feral Throwback"))) }

        groupB("a ward that discards a filtered card asks whether or not the hand holds a match",
            hand = listOf("Shock", "Divination"), lands = "Mountain" to 1, forked = "Divination", into = "Mountain",
            setUp = { withCardOnBattlefield(1, "Saruman of Many Colors") },
        ) { game, start ->
            val saruman = game.findPermanent("Saruman of Many Colors")!!
            play(game, CastSpell(game.player2Id, cardIn(start, game.player2Id, "Shock"), targets = listOf(ChosenTarget.Permanent(saruman))))
        }

        groupB("a may-discard with a reflexive payoff asks whether or not the hand holds a match",
            hand = listOf("Hidden Test Offering", "Grizzly Bears"), lands = "Island" to 1, forked = "Grizzly Bears", into = "Forest",
        ) { game, start -> play(game, CastSpell(game.player2Id, cardIn(start, game.player2Id, "Hidden Test Offering"))) }

        groupB("a may-discard with an if-you-do payoff asks whether or not the hand holds a match",
            hand = listOf("Pack Guardian", "Forest"), lands = "Forest" to 4, forked = "Forest", into = "Grizzly Bears",
        ) { game, start -> play(game, CastSpell(game.player2Id, cardIn(start, game.player2Id, "Pack Guardian"))) }

        groupB("a choose-one asks whether or not hidden cards rule an option out",
            hand = listOf("Hidden Test Ultimatum", "Grizzly Bears"), lands = "Island" to 1, forked = "Grizzly Bears", into = "Forest",
            setUp = { withCardOnBattlefield(2, "Ornithopter") },
        ) { game, start -> play(game, CastSpell(game.player2Id, cardIn(start, game.player2Id, "Hidden Test Ultimatum"))) }

        test("a may-discard with no match in hand offers only no, and a yes is refused") {
            val game = scenario().withPlayers()
                .withCardInHand(2, "Hidden Test Offering").withCardInHand(2, "Forest")
                .withLandsOnBattlefield(2, "Island", 1)
                .withActivePlayer(2).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            mutableListOf<GameAction>().play(game, CastSpell(game.player2Id, cardIn(game.state, game.player2Id, "Hidden Test Offering")))
            val decision = game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            decision.yesAvailable shouldBe false
            game.execute(SubmitDecision(game.player2Id, YesNoResponse(decision.id, true))).error.shouldNotBeNull()
        }
    }
}
