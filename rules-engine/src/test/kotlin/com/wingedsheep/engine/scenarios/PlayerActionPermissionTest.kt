package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TakePlayerAction
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.PlayerActionTiming
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class PlayerActionPermissionTest : ScenarioTestBase() {
    init {
        test("permission belongs to recipient, resolves immediately, and preserves priority") {
            val game = scenario().withPlayers().withActivePlayer(1).build()
            val grant = Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.GainLife(2), PlayerActionTiming.Instant, "Pay 1 life: gain 2 life")
            game.state = services.effectExecutorRegistry.execute(game.state, grant, EffectContext(null, game.player1Id)).state
            val id = game.state.playerActionPermissions.single().id
            game.execute(TakePlayerAction(game.player2Id, id)).error!! shouldContain "another player"
            game.getLifeTotal(2) shouldBe 20
            game.execute(TakePlayerAction(game.player1Id, id)).error shouldBe null
            game.getLifeTotal(1) shouldBe 21
            game.state.stack shouldBe emptyList()
            game.state.priorityPlayerId shouldBe game.player1Id
            game.execute(TakePlayerAction(game.player1Id, id)).error shouldBe null
            game.getLifeTotal(1) shouldBe 22
        }
        test("granting the same snapshot produces the same permission routing identity") {
            val game = scenario().withPlayers().withActivePlayer(1).build()
            val grant = Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.GainLife(2), PlayerActionTiming.Instant, "Life action")
            val context = EffectContext(null, game.player1Id)
            services.effectExecutorRegistry.execute(game.state, grant, context) shouldBe
                services.effectExecutorRegistry.execute(game.state, grant, context)
        }
        test("controlled turns retain the affected seat as the permission owner and payer") {
            val game = scenario().withPlayers().withActivePlayer(2).build()
            game.state = game.state.updateEntity(game.player2Id) {
                it.with(com.wingedsheep.engine.state.components.player.HotseatControlComponent(game.player1Id))
            }
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.GainLife(2), PlayerActionTiming.Instant, "Life action"),
                EffectContext(null, game.player2Id)).state.withPriority(game.player2Id)
            val action = TakePlayerAction(game.player2Id, game.state.playerActionPermissions.single().id)
            services.legalActionEnumerator.enumerate(game.state, game.player2Id).any { it.action == action } shouldBe true
            game.execute(action).error shouldBe null
            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 21
        }
        test("only the granted duration expires at cleanup") {
            val game = scenario().withPlayers().withActivePlayer(1).build()
            for (duration in listOf(Duration.EndOfTurn, Duration.Permanent)) {
                game.state = services.effectExecutorRegistry.execute(game.state,
                    Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.GainLife(2), PlayerActionTiming.Instant, "Life action", duration = duration),
                    EffectContext(null, game.player1Id)).state
            }
            val expiredId = game.state.playerActionPermissions.first().id
            game.state = services.turnManager.cleanupPhaseManager.cleanupEndOfTurn(game.state)
            game.state.playerActionPermissions.map { it.action.duration } shouldBe listOf(Duration.Permanent)
            game.execute(TakePlayerAction(game.player1Id, expiredId)).error!! shouldContain "no longer exists"
        }
        test("sorcery permissions are unavailable on opponents turns") {
            val game = scenario().withPlayers().withActivePlayer(2).build()
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.GainLife(2), PlayerActionTiming.Sorcery, "Sorcery action"),
                EffectContext(null, game.player1Id)).state.withPriority(game.player1Id)
            val action = TakePlayerAction(game.player1Id, game.state.playerActionPermissions.single().id)
            game.execute(action).error!! shouldContain "sorcery timing"
            services.legalActionEnumerator.enumerate(game.state, game.player1Id).none { it.action is TakePlayerAction } shouldBe true
        }
        test("split second permits the special action and does not report an ability activation") {
            cardRegistry.register(com.wingedsheep.sdk.dsl.card("Split-second test") {
                manaCost = "{0}"; typeLine = "Instant"
                keywords(com.wingedsheep.sdk.core.Keyword.SPLIT_SECOND)
                spell { effect = Effects.GainLife(1) }
            })
            val game = scenario().withPlayers().withCardInHand(2, "Split-second test").withActivePlayer(1).build()
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.GainLife(2), PlayerActionTiming.Instant, "Life action"), EffectContext(null, game.player1Id)).state
            val action = TakePlayerAction(game.player1Id, game.state.playerActionPermissions.single().id)
            game.state = game.state.withPriority(game.player2Id)
            game.castSpell(2, "Split-second test").error shouldBe null
            game.passPriority().error shouldBe null
            services.legalActionEnumerator.enumerate(game.state, game.player1Id).any { it.action == action } shouldBe true
            val result = game.execute(action)
            result.error shouldBe null
            result.events.none { it is com.wingedsheep.engine.core.AbilityActivatedEvent } shouldBe true
            game.state.stack.size shouldBe 1
            game.getLifeTotal(1) shouldBe 21
        }

        test("serialized permission retains targets and its captured context") {
            val game = scenario().withPlayers().withActivePlayer(1).build()
            val context = EffectContext(null, game.player1Id, targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(game.player2Id)), xValue = 7)
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.GainLife(2, com.wingedsheep.sdk.scripting.targets.EffectTarget.ContextTarget(0)), PlayerActionTiming.Instant, "Life action"), context).state
            val permission = game.state.playerActionPermissions.single()
            val restored = kotlinx.serialization.json.Json.decodeFromString<com.wingedsheep.engine.event.PlayerActionPermission>(
                kotlinx.serialization.json.Json.encodeToString(com.wingedsheep.engine.event.PlayerActionPermission.serializer(), permission))
            restored shouldBe permission
            game.state = game.state.copy(playerActionPermissions = listOf(restored))
            game.execute(TakePlayerAction(game.player1Id, restored.id)).error shouldBe null
            game.getLifeTotal(1) shouldBe 19
            game.getLifeTotal(2) shouldBe 22
        }
        test("instant permissions are withheld during a resolution mana payment") {
            val game = scenario().withPlayers().withLandsOnBattlefield(1, "Mountain", 1).withActivePlayer(1).build()
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.GainLife(2), PlayerActionTiming.Instant, "Life action"), EffectContext(null, game.player1Id)).state
            val permission = game.state.playerActionPermissions.single()
            val payment = services.costPaymentService.pay(game.state, game.player1Id, Costs.pay.Mana("{1}"), game.player1Id)
            game.state = payment.state
            game.answerYesNo(true).error shouldBe null
            (game.state.pendingDecision is com.wingedsheep.engine.core.SelectManaSourcesDecision) shouldBe true
            game.execute(TakePlayerAction(game.player1Id, permission.id)).error!! shouldContain "decision"
            services.legalActionEnumerator.enumerateManaAbilities(game.state, game.player1Id).none { it.action is TakePlayerAction } shouldBe true
        }
        test("no special action interrupts declaring attackers") {
            val game = scenario().withPlayers().withActivePlayer(1).build()
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.GainLife(2), PlayerActionTiming.Instant, "Life action"), EffectContext(null, game.player1Id)).state
            game.advanceToPhase(com.wingedsheep.sdk.core.Phase.COMBAT, com.wingedsheep.sdk.core.Step.DECLARE_ATTACKERS)
            val permission = game.state.playerActionPermissions.single()
            game.execute(TakePlayerAction(game.player1Id, permission.id)).error!! shouldContain "priority"
            services.legalActionEnumerator.enumerate(game.state, game.player1Id).none { it.action is TakePlayerAction } shouldBe true
        }

        test("life locked players cannot fund mana affordability") {
            val game = scenario().withPlayers().withActivePlayer(1).build()
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.AddColorlessMana(1), PlayerActionTiming.ManaAbility, "Life mana"),
                EffectContext(null, game.player1Id)).state
            services.manaSolver.canPay(game.state, game.player1Id, com.wingedsheep.sdk.core.ManaCost.parse("{1}")) shouldBe true
            game.state = game.state.updateEntity(game.player1Id) { it.with(com.wingedsheep.engine.state.components.player.CantLoseLifeComponent()) }
            services.manaSolver.canPay(game.state, game.player1Id, com.wingedsheep.sdk.core.ManaCost.parse("{1}")) shouldBe false
            services.manaSolver.getAvailableManaCount(game.state, game.player1Id) shouldBe 0
        }
        test("activation mana window reserves the source tap cost") {
            val card = com.wingedsheep.sdk.dsl.card("Reserved tap source") {
                typeLine = "Artifact"
                activatedAbility { cost = Costs.Tap; effect = Effects.AddColorlessMana(1); manaAbility = true; timing = com.wingedsheep.sdk.scripting.TimingRule.ManaAbility }
                activatedAbility { cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap); effect = Effects.GainLife(3) }
            }
            cardRegistry.register(card)
            val game = scenario().withPlayers().withCardOnBattlefield(1, card.name).withActivePlayer(1).build()
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.AddColorlessMana(1), PlayerActionTiming.ManaAbility, "Life mana"), EffectContext(null, game.player1Id)).state
            val source = game.findPermanent(card.name)!!
            val id = game.state.playerActionPermissions.single().id
            game.execute(com.wingedsheep.engine.core.ActivateAbility(game.player1Id, source, card.script.activatedAbilities.last().id)).error shouldBe null
            val window = game.state.pendingDecision as com.wingedsheep.engine.core.SelectManaSourcesDecision
            window.availableSources.none { it.entityId == source } shouldBe true
            game.execute(TakePlayerAction(game.player1Id, id)).error shouldBe null
            game.submitDecision(com.wingedsheep.engine.core.ManaSourcesSelectedResponse(window.id, listOf(source), autoPay = false)).error!! shouldContain "Selected sources"
            game.execute(TakePlayerAction(game.player1Id, id)).error shouldBe null
            game.submitDecision(com.wingedsheep.engine.core.ManaSourcesSelectedResponse(window.id, autoPay = true)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 21
        }
        test("manually tapping an announced activation source cannot pay the tap cost twice") {
            val card = com.wingedsheep.sdk.dsl.card("Manual reserved tap source") {
                typeLine = "Artifact"
                activatedAbility { cost = Costs.Tap; effect = Effects.AddColorlessMana(1); manaAbility = true; timing = com.wingedsheep.sdk.scripting.TimingRule.ManaAbility }
                activatedAbility { cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap); effect = Effects.GainLife(3) }
            }
            cardRegistry.register(card)
            val game = scenario().withPlayers().withCardOnBattlefield(1, card.name).withActivePlayer(1).build()
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.AddColorlessMana(1), PlayerActionTiming.ManaAbility, "Life mana"), EffectContext(null, game.player1Id)).state
            val source = game.findPermanent(card.name)!!
            val id = game.state.playerActionPermissions.single().id
            game.execute(com.wingedsheep.engine.core.ActivateAbility(game.player1Id, source, card.script.activatedAbilities.last().id)).error shouldBe null
            val window = game.state.pendingDecision as com.wingedsheep.engine.core.SelectManaSourcesDecision
            game.execute(com.wingedsheep.engine.core.ActivateAbility(game.player1Id, source, card.script.activatedAbilities.first().id)).error shouldBe null
            game.execute(TakePlayerAction(game.player1Id, id)).error shouldBe null
            game.submitDecision(com.wingedsheep.engine.core.ManaSourcesSelectedResponse(window.id)).error!! shouldContain "tap cost"
            game.state.stack shouldBe emptyList()
            game.getLifeTotal(1) shouldBe 19
        }
        test("casting window does not turn creature-only mana into unrestricted mana") {
            val sourceCard = com.wingedsheep.sdk.dsl.card("Restricted source") {
                typeLine = "Artifact"
                activatedAbility { cost = Costs.Tap; effect = Effects.AddColorlessMana(1, restriction = com.wingedsheep.sdk.scripting.effects.ManaRestriction.CreatureSpellsOnly); manaAbility = true; timing = com.wingedsheep.sdk.scripting.TimingRule.ManaAbility }
            }
            val spell = com.wingedsheep.sdk.dsl.card("Restricted payment spell") { typeLine = "Artifact"; manaCost = "{2}" }
            cardRegistry.register(sourceCard); cardRegistry.register(spell)
            val game = scenario().withPlayers().withCardOnBattlefield(1, sourceCard.name).withCardInHand(1, spell.name).withActivePlayer(1).build()
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.AddColorlessMana(1), PlayerActionTiming.ManaAbility, "Life mana"), EffectContext(null, game.player1Id)).state
            game.castSpell(1, spell.name).error shouldBe null
            val window = game.state.pendingDecision as com.wingedsheep.engine.core.SelectManaSourcesDecision
            window.availableSources.none { it.entityId == game.findPermanent(sourceCard.name) } shouldBe true
            val id = game.state.playerActionPermissions.single().id
            repeat(2) { game.execute(TakePlayerAction(game.player1Id, id)).error shouldBe null }
            game.submitDecision(com.wingedsheep.engine.core.ManaSourcesSelectedResponse(window.id)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 18
        }

        test("casting window floats compatible mana with its restriction before payment") {
            val sourceCard = com.wingedsheep.sdk.dsl.card("Surplus restricted source") {
                typeLine = "Artifact"
                activatedAbility {
                    cost = Costs.Tap
                    effect = Effects.AddColorlessMana(2, restriction = com.wingedsheep.sdk.scripting.effects.ManaRestriction.CreatureSpellsOnly)
                    manaAbility = true
                    timing = com.wingedsheep.sdk.scripting.TimingRule.ManaAbility
                }
            }
            val spell = com.wingedsheep.sdk.dsl.card("Restricted surplus creature") {
                typeLine = "Creature — Bear"
                manaCost = "{3}"
                power = 2
                toughness = 2
            }
            cardRegistry.register(sourceCard)
            cardRegistry.register(spell)
            val game = scenario().withPlayers().withCardOnBattlefield(1, sourceCard.name)
                .withCardInHand(1, spell.name).withActivePlayer(1).build()
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.GrantPlayerAction(Costs.pay.PayLife(1), Effects.AddColorlessMana(1), PlayerActionTiming.ManaAbility, "Life mana"),
                EffectContext(null, game.player1Id)).state
            game.castSpell(1, spell.name).error shouldBe null
            val window = game.state.pendingDecision as com.wingedsheep.engine.core.SelectManaSourcesDecision
            val id = game.state.playerActionPermissions.single().id
            repeat(2) { game.execute(TakePlayerAction(game.player1Id, id)).error shouldBe null }
            val answer = com.wingedsheep.engine.core.ManaSourcesSelectedResponse(window.id, autoPay = true)
            val floated = com.wingedsheep.engine.mechanics.mana.ManaPaymentWindow.floatSelectedMana(
                services.zones, game.state, game.player1Id, com.wingedsheep.sdk.core.ManaCost.parse("{3}"), answer,
                window.availableSources, services,
                spellContext = com.wingedsheep.engine.mechanics.mana.SpellPaymentContext(isCreature = true),
            )
            floated.paid shouldBe true
            val producedPool = floated.state.getEntity(game.player1Id)!!.get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()!!
            producedPool.colorless shouldBe 2
            producedPool.restrictedMana.size shouldBe 2
            producedPool.restrictedMana.all { it.restriction == com.wingedsheep.sdk.scripting.effects.ManaRestriction.CreatureSpellsOnly } shouldBe true
            com.wingedsheep.engine.mechanics.mana.ManaPool(restrictedMana = producedPool.restrictedMana)
                .canPay(com.wingedsheep.sdk.core.ManaCost.parse("{1}"), com.wingedsheep.engine.mechanics.mana.SpellPaymentContext()) shouldBe false
            game.submitDecision(answer).error shouldBe null
            game.resolveStack()
            val pool = game.state.getEntity(game.player1Id)!!.get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()!!
            // Eligible restricted mana is spent first; the surplus is Channel's unrestricted mana.
            pool.colorless shouldBe 1
            pool.restrictedMana.size shouldBe 0
            game.getLifeTotal(1) shouldBe 18
        }

        test("unaffordable life payments reject atomically") {
            val game = scenario().withPlayers().withLifeTotal(1, 1).withActivePlayer(1).build()
            game.state = services.effectExecutorRegistry.execute(game.state,
                Effects.GrantPlayerAction(Costs.pay.PayLife(2), Effects.GainLife(5), PlayerActionTiming.Instant, "Pay 2 life"),
                EffectContext(null, game.player1Id)).state
            val before = game.state
            val result = game.execute(TakePlayerAction(game.player1Id, game.state.playerActionPermissions.single().id))
            result.error!! shouldContain "Cannot pay"
            result.state shouldBe before
            result.events shouldBe emptyList()
        }
    }
}
