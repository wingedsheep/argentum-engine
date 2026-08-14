package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mjölnir, Hammer of Thor (MSH #146) — {3}{R} Legendary Artifact — Equipment.
 *
 * When Mjölnir enters, it deals 4 damage to up to one target creature.
 * Double all damage equipped creature would deal.
 * Equip worthy {1} (A creature is worthy if it's a legendary non-Villain that's red and/or white.)
 * {2}{R}, Discard this card: It deals 2 damage to each creature.
 *
 * The two things worth pinning here are the halves that needed new SDK surface:
 *
 *  - **Equip worthy** is an "Equip [quality]" variant (CR 702.6c) — legal targets are only
 *    creatures the activating player controls that have the quality. The load-bearing assertion is
 *    that the *legal-action enumerator* offers only worthy creatures, because that list
 *    (`LegalActionInfo.validTargets`) is verbatim what the web client highlights; a filter that is
 *    only enforced at validation time would still let the UI offer an illegal creature.
 *  - **The doubling** is a `DoubleDamage` replacement scoped to the damage *source* being the
 *    equipped creature, with `recipient` and `damageType` both left at `Any`. Combat and noncombat
 *    damage go through the same shared damage-source matcher, so both are covered here — combat
 *    damage to a player and an activated-ability ping at a permanent — each against an unequipped
 *    control that pins the undoubled number.
 */
class MjolnirHammerOfThorScenarioTest : ScenarioTestBase() {

    private val mjolnir get() = cardRegistry.getCard("Mjölnir, Hammer of Thor")!!
    private val equipAbilityId get() = mjolnir.script.activatedAbilities.single { it.isEquipAbility }.id
    private val discardAbilityId get() = mjolnir.script.activatedAbilities.single { !it.isEquipAbility }.id

    /** Prodigal Sorcerer — "{T}: This creature deals 1 damage to any target." */
    private val sorcererPingAbilityId
        get() = cardRegistry.getCard("Prodigal Sorcerer")!!.script.activatedAbilities.single().id

    /**
     * Vanilla bodies, defined here rather than picked out of the catalog so the four axes of
     * "worthy" (legendary / not a Villain / red / white) are each varied one at a time and nothing
     * else about the creature can influence the result. 2/2 unless a test needs a body that
     * survives the damage it is measuring.
     */
    private fun body(
        name: String,
        manaCost: String,
        subtype: Subtype,
        legendary: Boolean,
        power: Int = 2,
        toughness: Int = 2,
    ) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse(manaCost),
        subtypes = setOf(subtype),
        power = power,
        toughness = toughness,
        supertypes = if (legendary) setOf(Supertype.LEGENDARY) else emptySet(),
    )

    /** Marked damage on a permanent, so a test can pin the *amount* and not just "it died". */
    private fun TestGame.damageMarkedOn(id: EntityId): Int =
        state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    /**
     * Drive a freshly cast Mjölnir all the way through: pay for the spell, resolve it, answer the
     * enters trigger's target prompt with [chooseTarget], then let the trigger resolve and settle
     * state-based actions. Written as a loop because the mana prompt, the spell resolution and the
     * trigger's target prompt arrive in that order but with a variable number of priority passes
     * between them.
     */
    private fun TestGame.settleEntersTrigger(chooseTarget: () -> Unit) {
        var guard = 0
        var targetChosen = false
        while (guard++ < 12) {
            val decision = getPendingDecision()
            when {
                decision is SelectManaSourcesDecision -> submitManaSourcesAutoPay()
                decision != null && !targetChosen -> {
                    chooseTarget()
                    targetChosen = true
                }
                decision != null -> break
                state.stack.isNotEmpty() -> resolveStack()
                else -> break
            }
        }
        checkStateBasedActions()
    }

    init {
        // Worthy: legendary, not a Villain, red and/or white.
        cardRegistry.register(body("Thunder Prince", "{R}", Subtype.HERO, legendary = true))
        cardRegistry.register(body("Shield Paladin", "{W}", Subtype.HERO, legendary = true))
        cardRegistry.register(body("Dawnfire Captain", "{R}{W}", Subtype.HERO, legendary = true))
        // Not worthy, one axis at a time.
        cardRegistry.register(body("Masked Schemer", "{R}", Subtype.VILLAIN, legendary = true))
        cardRegistry.register(body("Thornvine Elder", "{G}", Subtype.HERO, legendary = true))
        cardRegistry.register(body("Rank-and-File Hero", "{R}", Subtype.HERO, legendary = false))
        // Worthy in every respect except that an opponent controls it (CR 702.6c).
        cardRegistry.register(body("Rival Champion", "{R}", Subtype.HERO, legendary = true))
        // A body big enough to survive the damage the amount-pinning tests measure, so the
        // assertion can read marked damage instead of inferring the number from a death.
        cardRegistry.register(
            body("Stalwart Bulwark", "{4}", Subtype.HERO, legendary = false, power = 2, toughness = 5)
        )

        context("Equip worthy {1} — CR 702.6c") {

            /** Mjölnir plus one creature of every worthiness shape, split across both players. */
            fun board() = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Mjölnir, Hammer of Thor")
                .withCardOnBattlefield(1, "Thunder Prince")
                .withCardOnBattlefield(1, "Shield Paladin")
                .withCardOnBattlefield(1, "Dawnfire Captain")
                .withCardOnBattlefield(1, "Masked Schemer")
                .withCardOnBattlefield(1, "Thornvine Elder")
                .withCardOnBattlefield(1, "Rank-and-File Hero")
                .withCardOnBattlefield(2, "Rival Champion")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("the enumerator offers only worthy creatures you control as equip targets") {
                val game = board()

                val equip = game.getLegalActions(1)
                    .singleOrNull { (it.action as? ActivateAbility)?.abilityId == equipAbilityId }
                withClue("the equip ability is offered at sorcery speed with {1} available") {
                    equip shouldNotBe null
                    equip!!.isAffordable shouldBe true
                }

                withClue("the target label is what the prompt shows the player") {
                    equip!!.targetDescription shouldBe "worthy creature you control"
                }

                withClue(
                    "validTargets is exactly what the client highlights — a legendary non-Villain " +
                        "that's red and/or white, that you control"
                ) {
                    equip!!.validTargets!! shouldContainExactlyInAnyOrder listOf(
                        game.findPermanent("Thunder Prince")!!,
                        game.findPermanent("Shield Paladin")!!,
                        game.findPermanent("Dawnfire Captain")!!,
                    )
                }
            }

            test("equipping a worthy creature attaches Mjölnir to it") {
                val game = board()
                val hammer = game.findPermanent("Mjölnir, Hammer of Thor")!!
                val prince = game.findPermanent("Thunder Prince")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = hammer,
                        abilityId = equipAbilityId,
                        targets = listOf(ChosenTarget.Permanent(prince)),
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.state.getEntity(hammer)?.get<AttachedToComponent>()?.targetId shouldBe prince
            }

            test("equipping an unworthy creature is rejected even when submitted directly") {
                val game = board()
                val hammer = game.findPermanent("Mjölnir, Hammer of Thor")!!
                val villain = game.findPermanent("Masked Schemer")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = hammer,
                        abilityId = equipAbilityId,
                        targets = listOf(ChosenTarget.Permanent(villain)),
                    )
                )
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("a legendary red Villain is not worthy: target validation rejects the activation") {
                    result.error shouldNotBe null
                }
                withClue("and nothing was attached") {
                    game.state.getEntity(hammer)?.get<AttachedToComponent>() shouldBe null
                }
            }

            test("the quality restricts targeting only — an equipped creature that isn't worthy stays equipped") {
                // CR 702.6c: "Additional restrictions for an equip ability don't restrict what the
                // Equipment may be attached to." An Equipment comes off only under CR 704.5n (the
                // host is an illegal permanent — not a creature at all).
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thornvine Elder")
                    .withCardAttachedTo(1, "Mjölnir, Hammer of Thor", "Thornvine Elder")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hammer = game.findPermanent("Mjölnir, Hammer of Thor")!!
                val elder = game.findPermanent("Thornvine Elder")!!

                game.checkStateBasedActions()

                withClue("a green legend is not worthy, but state-based actions leave it equipped") {
                    game.state.getEntity(hammer)?.get<AttachedToComponent>()?.targetId shouldBe elder
                }
            }
        }

        context("Double all damage equipped creature would deal") {

            fun equipped(hostName: String) = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, hostName, summoningSickness = false)
                .withCardAttachedTo(1, "Mjölnir, Hammer of Thor", hostName)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("combat damage from the equipped creature is doubled") {
                val game = equipped("Thunder Prince")
                val before = game.getLifeTotal(2)

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Thunder Prince" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.state.pendingDecision != null) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }

                withClue("a 2/2 equipped with Mjölnir deals 4, not 2") {
                    game.getLifeTotal(2) shouldBe before - 4
                }
            }

            test("an unequipped copy of the same creature deals its printed damage") {
                // Control for the test above: without the Equipment attached, nothing doubles.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thunder Prince", summoningSickness = false)
                    .withCardOnBattlefield(1, "Mjölnir, Hammer of Thor")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val before = game.getLifeTotal(2)

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Thunder Prince" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.state.pendingDecision != null) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }

                game.getLifeTotal(2) shouldBe before - 2
            }

            test("noncombat damage from the equipped creature is doubled, to a permanent too") {
                // The replacement is scoped to the damage *source*; `recipient` and `damageType`
                // both default to Any, so an activated-ability ping at a permanent doubles just
                // like combat damage to a player does. Prodigal Sorcerer is not worthy, but per
                // CR 702.6c the quality restricts targeting only — an effect (here the scenario
                // builder) may attach the Equipment to any creature.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Prodigal Sorcerer", summoningSickness = false)
                    .withCardAttachedTo(1, "Mjölnir, Hammer of Thor", "Prodigal Sorcerer")
                    .withCardOnBattlefield(2, "Stalwart Bulwark")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sorcerer = game.findPermanent("Prodigal Sorcerer")!!
                val bulwark = game.findPermanent("Stalwart Bulwark")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sorcerer,
                        abilityId = sorcererPingAbilityId,
                        targets = listOf(ChosenTarget.Permanent(bulwark)),
                    )
                ).error shouldBe null
                game.resolveStack()
                game.checkStateBasedActions()

                withClue("the Sorcerer's printed 1 damage lands as 2 on the 2/5, which survives it") {
                    game.damageMarkedOn(bulwark) shouldBe 2
                    game.isOnBattlefield("Stalwart Bulwark") shouldBe true
                }
            }

            test("noncombat damage from an unequipped creature is not doubled") {
                // Control for the case above: same ping, Mjölnir on the battlefield but attached
                // to nothing.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Prodigal Sorcerer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Mjölnir, Hammer of Thor")
                    .withCardOnBattlefield(2, "Stalwart Bulwark")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sorcerer = game.findPermanent("Prodigal Sorcerer")!!
                val bulwark = game.findPermanent("Stalwart Bulwark")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = sorcerer,
                        abilityId = sorcererPingAbilityId,
                        targets = listOf(ChosenTarget.Permanent(bulwark)),
                    )
                ).error shouldBe null
                game.resolveStack()
                game.checkStateBasedActions()

                game.damageMarkedOn(bulwark) shouldBe 1
            }
        }

        context("the enters trigger and the discard ability") {

            test("Mjölnir's own enters damage is exactly 4") {
                // Pinned by marked damage on a body that survives it, so the assertion fails on 2,
                // 3 or a doubled 8 alike. (The doubling can't apply here in any case: an Equipment
                // doesn't enter attached to anything — CR 301.5b — so as this trigger resolves
                // there is no equipped creature to be the source. The source-scoping of the
                // replacement is what the two unequipped control cases above pin.)
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Mjölnir, Hammer of Thor")
                    .withCardOnBattlefield(2, "Stalwart Bulwark")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val victim = game.findPermanent("Stalwart Bulwark")!!

                game.castSpell(1, "Mjölnir, Hammer of Thor").error shouldBe null
                game.settleEntersTrigger { game.selectTargets(listOf(victim)) }

                withClue("the 2/5 takes 4 — not 2, not 8 — and survives") {
                    game.damageMarkedOn(victim) shouldBe 4
                    game.isOnBattlefield("Stalwart Bulwark") shouldBe true
                }
            }

            test("the enters trigger is 'up to one', so it may be skipped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Mjölnir, Hammer of Thor")
                    // Two candidates, so the choice can't be short-circuited to a single option.
                    .withCardOnBattlefield(2, "Rival Champion")
                    .withCardOnBattlefield(2, "Rank-and-File Hero")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Mjölnir, Hammer of Thor").error shouldBe null
                game.settleEntersTrigger { game.skipTargets() }

                withClue("choosing no target leaves the board alone") {
                    game.isOnBattlefield("Rival Champion") shouldBe true
                    game.isOnBattlefield("Rank-and-File Hero") shouldBe true
                }
                game.isOnBattlefield("Mjölnir, Hammer of Thor") shouldBe true
            }

            test("{2}{R}, Discard this card: it deals 2 damage to each creature — from hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Mjölnir, Hammer of Thor")
                    .withCardOnBattlefield(1, "Thunder Prince")
                    .withCardOnBattlefield(2, "Rival Champion")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hammerInHand: EntityId =
                    game.findCardsInHand(1, "Mjölnir, Hammer of Thor").single()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = hammerInHand,
                        abilityId = discardAbilityId,
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.checkStateBasedActions()

                withClue("2 damage to each creature kills both 2/2s, on both sides") {
                    game.isOnBattlefield("Thunder Prince") shouldBe false
                    game.isOnBattlefield("Rival Champion") shouldBe false
                }
                withClue("the card was discarded as a cost") {
                    game.isInGraveyard(1, "Mjölnir, Hammer of Thor") shouldBe true
                }
            }
        }

        /**
         * The doubling is scoped to the damage *source* being the equipped creature, so the badge
         * that announces it belongs on that creature — not on a player.
         *
         * The player-badge path ([DamageUtils.damageDoublersAffectingPlayer]) evaluates only the
         * `RecipientFilter`. Mjölnir leaves `recipient` at `Any`, so before the fix an unattached
         * Mjölnir told *both* players "Damage dealt to you is doubled by Mjölnir, Hammer of Thor" —
         * false twice over: an unequipped Equipment doubles nothing, and even equipped it doubles
         * one creature's outgoing damage rather than everything aimed at a player.
         */
        context("the doubling is badged on the equipped creature, not on the players") {

            fun TestGame.doubledBadgesOnPlayers(): List<String?> =
                getClientState(1).players
                    .flatMap { it.activeEffects }
                    .filter { it.effectId.startsWith("damage_doubled") }
                    .map { it.description }

            fun TestGame.doubledBadgesOn(id: EntityId): List<String?> =
                getClientState(1).cards.getValue(id).activeEffects
                    .filter { it.effectId.startsWith("damage_doubled") }
                    .map { it.description }

            test("an unequipped Mjölnir badges nobody — not either player, not any creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thunder Prince")
                    .withCardOnBattlefield(1, "Mjölnir, Hammer of Thor")
                    .withCardOnBattlefield(2, "Rival Champion")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("an Equipment attached to nothing doubles nothing") {
                    game.doubledBadgesOnPlayers() shouldBe emptyList()
                    game.doubledBadgesOn(game.findPermanent("Thunder Prince")!!) shouldBe emptyList()
                    game.doubledBadgesOn(game.findPermanent("Rival Champion")!!) shouldBe emptyList()
                }
            }

            test("once equipped, the badge is on the equipped creature and still on no player") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thunder Prince", summoningSickness = false)
                    .withCardAttachedTo(1, "Mjölnir, Hammer of Thor", "Thunder Prince")
                    .withCardOnBattlefield(2, "Rival Champion")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("the creature whose outgoing damage is doubled carries the badge") {
                    game.doubledBadgesOn(game.findPermanent("Thunder Prince")!!) shouldBe
                        listOf("Damage this creature deals is doubled by Mjölnir, Hammer of Thor")
                }
                withClue("no other creature is affected, on either side") {
                    game.doubledBadgesOn(game.findPermanent("Rival Champion")!!) shouldBe emptyList()
                }
                withClue("the doubling is source-scoped, so no player is warned about it") {
                    game.doubledBadgesOnPlayers() shouldBe emptyList()
                }
            }

            test("the badge follows the Equipment when it moves to another creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thunder Prince", summoningSickness = false)
                    .withCardOnBattlefield(1, "Shield Paladin", summoningSickness = false)
                    .withCardAttachedTo(1, "Mjölnir, Hammer of Thor", "Thunder Prince")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hammer = game.findPermanent("Mjölnir, Hammer of Thor")!!
                val prince = game.findPermanent("Thunder Prince")!!
                val paladin = game.findPermanent("Shield Paladin")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = hammer,
                        abilityId = equipAbilityId,
                        targets = listOf(ChosenTarget.Permanent(paladin)),
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("re-equipping moves the badge along with the Equipment") {
                    game.doubledBadgesOn(paladin) shouldBe
                        listOf("Damage this creature deals is doubled by Mjölnir, Hammer of Thor")
                    game.doubledBadgesOn(prince) shouldBe emptyList()
                }
            }
        }
    }
}
