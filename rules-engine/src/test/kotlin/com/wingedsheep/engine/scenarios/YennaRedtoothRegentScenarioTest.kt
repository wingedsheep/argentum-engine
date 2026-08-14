package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.woe.cards.YennaRedtoothRegent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Yenna, Redtooth Regent — "{2}, {T}: Choose target enchantment you control that doesn't have the
 * same name as another permanent you control. Create a token that's a copy of it, except it isn't
 * legendary. If the token is an Aura, untap Yenna, then scry 2. Activate only as a sorcery."
 *
 * Covers the two capabilities this card introduced:
 *  - `CardPredicate.NameNotSharedWithAnotherControlledPermanent` — the target restriction, which is
 *    self-limiting: copying an enchantment makes it an illegal target from then on.
 *  - The Aura branch of `CreateTokenCopyOfTargetExecutor` — a token copy of an Aura is created, not
 *    cast, so its controller chooses what it enchants as it enters (CR 303.4h).
 */
class YennaRedtoothRegentScenarioTest : ScenarioTestBase() {

    private val copyAbilityId = YennaRedtoothRegent.activatedAbilities.first { !it.isManaAbility }.id

    init {

        test("copies a non-Aura enchantment; Yenna stays tapped and there is no scry") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Yenna, Redtooth Regent")
                .withCardOnBattlefield(1, "A Tale for the Ages")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val yenna = game.findPermanent("Yenna, Redtooth Regent")!!
            val tale = game.findPermanent("A Tale for the Ages")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = yenna,
                    abilityId = copyAbilityId,
                    targets = listOf(ChosenTarget.Permanent(tale)),
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("a token copy of the enchantment now shares the battlefield with the original") {
                game.findAllPermanents("A Tale for the Ages").size shouldBe 2
            }
            withClue("the copy is a token; the original is not") {
                val token = game.findAllPermanents("A Tale for the Ages").single { it != tale }
                game.state.getEntity(token)?.has<TokenComponent>() shouldBe true
            }
            withClue("the copied enchantment isn't an Aura, so Yenna is not untapped") {
                game.state.getEntity(yenna)?.has<TappedComponent>() shouldBe true
            }
            withClue("and no scry is offered") {
                game.hasPendingDecision() shouldBe false
            }
        }

        test("the token copy of a legendary enchantment isn't legendary, so both survive") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Yenna, Redtooth Regent")
                .withCardOnBattlefield(1, "Mornsong Aria")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val yenna = game.findPermanent("Yenna, Redtooth Regent")!!
            val aria = game.findPermanent("Mornsong Aria")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = yenna,
                    abilityId = copyAbilityId,
                    targets = listOf(ChosenTarget.Permanent(aria)),
                )
            ).error shouldBe null
            game.resolveStack()

            val copies = game.findAllPermanents("Mornsong Aria")
            withClue("the legend rule (CR 704.5j) doesn't fire — the token isn't legendary") {
                copies.size shouldBe 2
            }
            val token = copies.single { it != aria }
            withClue("the token's projected types omit LEGENDARY") {
                game.state.projectedState.getProjectedValues(token)?.types shouldNotBe null
                game.state.projectedState.getProjectedValues(token)!!.types.contains("LEGENDARY") shouldBe false
            }
            withClue("the original keeps its own legendary supertype") {
                game.state.projectedState.getProjectedValues(aria)!!.types shouldContain "LEGENDARY"
            }
        }

        test("an enchantment sharing a name with another permanent you control isn't a legal target") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Yenna, Redtooth Regent")
                .withCardOnBattlefield(1, "A Tale for the Ages")
                .withCardOnBattlefield(1, "A Tale for the Ages")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val yenna = game.findPermanent("Yenna, Redtooth Regent")!!
            val tales = game.findAllPermanents("A Tale for the Ages")
            tales.size shouldBe 2

            withClue("both copies disqualify each other — 'another permanent you control'") {
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = yenna,
                        abilityId = copyAbilityId,
                        targets = listOf(ChosenTarget.Permanent(tales.first())),
                    )
                ).error shouldNotBe null
            }
        }

        test("copying an enchantment makes it an illegal target for the next activation") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Yenna, Redtooth Regent")
                .withCardOnBattlefield(1, "A Tale for the Ages")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val yenna = game.findPermanent("Yenna, Redtooth Regent")!!
            val tale = game.findPermanent("A Tale for the Ages")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = yenna,
                    abilityId = copyAbilityId,
                    targets = listOf(ChosenTarget.Permanent(tale)),
                )
            ).error shouldBe null
            game.resolveStack()
            game.findAllPermanents("A Tale for the Ages").size shouldBe 2

            // Untap Yenna by hand so the second attempt fails on the target restriction rather
            // than on the {T} cost.
            game.state = game.state.updateEntity(yenna) { it.without<TappedComponent>() }

            withClue("the original now shares its name with the token it produced") {
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = yenna,
                        abilityId = copyAbilityId,
                        targets = listOf(ChosenTarget.Permanent(tale)),
                    )
                ).error shouldNotBe null
            }
        }

        test("copying an Aura asks what the token enchants, then untaps Yenna and scries") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Yenna, Redtooth Regent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Charmed Sleep", "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val yenna = game.findPermanent("Yenna, Redtooth Regent")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val sleep = game.findPermanent("Charmed Sleep")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = yenna,
                    abilityId = copyAbilityId,
                    targets = listOf(ChosenTarget.Permanent(sleep)),
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("CR 303.4h — the controller chooses what the Aura token enchants") {
                val decision = game.getPendingDecision()
                (decision is ChooseTargetsDecision) shouldBe true
                (decision as ChooseTargetsDecision).legalTargets[0]!! shouldContain bears
            }
            game.selectTargets(listOf(bears))

            val copies = game.findAllPermanents("Charmed Sleep")
            withClue("the Aura token was created") {
                copies.size shouldBe 2
            }
            val token: EntityId = copies.single { it != sleep }
            withClue("and it entered already attached to the chosen host") {
                game.state.getEntity(token)?.get<AttachedToComponent>()?.targetId shouldBe bears
            }
            withClue("the token is an Aura, so Yenna untaps") {
                game.state.getEntity(yenna)?.has<TappedComponent>() shouldBe false
            }
            withClue("and the controller scries 2") {
                game.hasPendingDecision() shouldBe true
            }
        }
    }
}
