package com.wingedsheep.sdk.chaos

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TurnPart
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.ModeBuilder
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.div
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.scripting.AbilityIdScope
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * One rule of the **Chaos** house variant: a short, card-like instruction the active player rolls
 * at the beginning of their upkeep (see [ChaosRules]).
 *
 * @property id The rule's id in the original chaos-rule table (fcjensen.github.io `data.json`), so a
 *   rule can be traced back to the list players already know.
 * @property name The rule's name — usually the card it parodies ("Lightning Helix", "Wheel of Fortune").
 * @property text What the rule does, worded as the engine performs it.
 * @property mode The rule as a mode of the chaos roll: its effect and the targets the roller chooses
 *   as the rolled ability goes on the stack. Its description is "[name] — [text]", which is what the
 *   stack and the game log show for the rolled rule.
 */
data class ChaosRule(
    val id: Int,
    val name: String,
    val text: String,
    val mode: Mode,
)

/**
 * The **Chaos** house variant: at the beginning of each player's upkeep, that player rolls a random
 * chaos rule and it happens — "Day of Judgment: destroy all creatures", "Time Walk: take an extra
 * turn", "Prosperity: roll a d6, each player draws that many cards", and so on.
 *
 * The variant is pure composition of existing SDK vocabulary, the same way [com.wingedsheep.sdk.dungeons.Dungeons]
 * is: each rule is an ordinary [Mode] built from the facades cards use, and the roll is one
 * "choose one at random" modal triggered ability ([ModalEffect.chooseAtRandom]) on a Chaos emblem
 * every player gets as the game begins. That gives the variant the engine's own semantics for free:
 *
 * - the engine draws the rule from the game's seeded RNG as the trigger goes on the stack, before
 *   targets are chosen, so every player sees the rolled rule and can respond to it (CR 603.3c);
 * - a rule that couldn't legally be chosen — no legal target, say — is left out of that roll, which is
 *   the original table's "if not possible, reroll";
 * - [ModalEffect.excludePreviouslyChosenModes] keeps a rule from coming up twice for the same player,
 *   the original randomizer's no-repeat history;
 * - "you" in a rule is the player who rolled it, and every choice it asks for is theirs.
 *
 * Rules are worded for what the engine does. A few rules of the original table aren't here because
 * they change the chaos roll itself (Calm, Storm, Master of Chaos) or have no faithful engine reading
 * yet; see [notAutomated].
 */
object ChaosRules {

    /** Name of the Chaos emblem each player gets — the source of every rolled rule. */
    const val SOURCE_NAME: String = "Chaos"

    /** The emblem's text, shown on its badge and as the trigger's description. */
    const val EMBLEM_TEXT: String = "At the beginning of your upkeep, roll a random chaos rule. It happens to you."

    private val yourCreatures = GroupFilter.AllCreaturesYouControl
    private val graveyardOfYours = { filter: GameObjectFilter -> TargetFilter(filter.ownedByYou(), zone = Zone.GRAVEYARD) }

    /**
     * A rule as a mode whose effect reads "[name] — [text]" wherever the engine describes it. The
     * stack shows a chosen mode by its effect's description, so the effect itself carries the rule's
     * name: players see "Lightning Helix — Deal 3 damage to any target. You gain 3 life." rather than
     * the bare instruction.
     */
    private fun rule(id: Int, name: String, text: String, init: ModeBuilder.() -> Unit): ChaosRule {
        val label = "$name — $text"
        val built = mode(label, init)
        val labelled = built.copy(effect = Effects.Composite(listOf(built.effect), descriptionOverride = label))
        return ChaosRule(id, name, text, labelled)
    }

    /** "Each player who controls a [landType] …" — the land-type rules of the table's last page. */
    private fun eachPlayerWhoControls(landType: String, effect: com.wingedsheep.sdk.scripting.effects.Effect) =
        Effects.ForEachPlayer(
            Player.EachStartingWithYou,
            Effects.If(Conditions.YouControl(GameObjectFilter.Land.withSubtype(landType)), effect)
        )

    /**
     * Every chaos rule the engine plays automatically, in the order of the original table. Built in
     * one [AbilityIdScope] so the abilities and targets the rules declare get stable, distinct ids,
     * exactly as a card's do.
     */
    val all: List<ChaosRule> = AbilityIdScope.within(SOURCE_NAME) { listOf(
        rule(1, "Day of Judgment", "Destroy all creatures.") {
            effect = Effects.DestroyAll(GameObjectFilter.Creature)
        },
        rule(3, "Ancestral Recall", "Draw three cards. Skip your next draw step.") {
            effect = Effects.DrawCards(3) then Effects.SkipNextStepOrPhase(TurnPart.DRAW_STEP)
        },
        rule(2, "Reminisce", "Shuffle your graveyard into your library.") {
            effect = Patterns.Library.shuffleGraveyardIntoLibrary(EffectTarget.Controller)
        },
        rule(4, "Prosperity", "Roll a six-sided die. Each player draws cards equal to the result.") {
            effect = Effects.Pipeline {
                val roll = rollDiceResult(sides = 6)
                run(Effects.DrawCards(roll.amount, EffectTarget.PlayerRef(Player.Each)))
            }
        },
        rule(5, "Demonic Tutor", "Search your library for a card, put it into your hand, then shuffle.") {
            effect = Patterns.Library.searchLibrary(filter = GameObjectFilter.Any, destination = SearchDestination.HAND)
        },
        rule(
            6, "Eureka",
            "Starting with you, each player may put a permanent card from their hand onto the battlefield. " +
                "Each player who does loses life equal to that card's mana value."
        ) {
            effect = Effects.ForEachPlayer(
                Player.EachStartingWithYou,
                Effects.Pipeline {
                    val permanents = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Permanent))
                    val chosen = chooseUpTo(
                        1, from = permanents, chooser = Chooser.Controller,
                        prompt = "You may put a permanent card from your hand onto the battlefield"
                    )
                    move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
                    run(Effects.LoseLife(DynamicAmounts.manaValueSumOf(chosen), EffectTarget.Controller))
                }
            )
        },
        rule(7, "Exhume", "Starting with you, each player puts a creature card from their graveyard onto the battlefield.") {
            effect = Effects.ForEachPlayer(
                Player.EachStartingWithYou,
                Effects.Pipeline {
                    val creatures = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature))
                    val chosen = chooseExactly(
                        1, from = creatures, chooser = Chooser.Controller,
                        prompt = "Put a creature card from your graveyard onto the battlefield"
                    )
                    move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
                }
            )
        },
        rule(
            8, "tnahC smirO",
            "You can't cast spells this turn. Then any player may pay 5 life. If a player does, " +
                "creatures you control can't attack this turn."
        ) {
            effect = Effects.CantCastSpells(EffectTarget.Controller) then
                Effects.AnyPlayerMayPay(
                    Costs.pay.PayLife(5),
                    Effects.CantAttackGroup(yourCreatures)
                )
        },
        rule(9, "Better Off Dead", "Put target creature card from your graveyard onto the battlefield under target opponent's control.") {
            val card = target(graveyardOfYours(GameObjectFilter.Creature))
            val opponent = target(Targets.Opponent)
            effect = Effects.Move(card, Zone.BATTLEFIELD, controllerOverride = opponent, fromZone = Zone.GRAVEYARD)
        },
        rule(12, "Crimson Hellflame", "You may pay any amount of life. Deal that much damage to target creature.") {
            val creature = target(TargetFilter.Creature)
            effect = Effects.MayPayAnyAmountOfLife(Effects.DealDamage(DynamicAmount.XValue, creature))
        },
        rule(13, "Syphon Soul", "Deal 2 damage to each opponent. You gain life equal to the damage dealt this way.") {
            effect = Effects.Pipeline {
                val dealt = runStoringNumber {
                    Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent), damageDealtVariable = it)
                }
                run(Effects.GainLife(dealt.amount))
            }
        },
        rule(14, "Shatter", "Destroy target artifact.") {
            val artifact = target(TargetFilter.Artifact)
            effect = Effects.Destroy(artifact)
        },
        rule(15, "Collective Unconscious", "Draw a card for each creature you control.") {
            effect = Effects.DrawCards(DynamicAmounts.creaturesYouControl())
        },
        rule(16, "Lightning Helix", "Deal 3 damage to any target. You gain 3 life.") {
            val any = target(Targets.Any)
            effect = Effects.DealDamage(3, any) then Effects.GainLife(3)
        },
        rule(17, "Necrologia", "Choose a number. Pay twice that much life and draw that many cards. Skip your next draw step.") {
            effect = Effects.ChooseNumberThen(
                then = Effects.PayDynamicLife(DynamicAmount.XValue * 2) then Effects.DrawCards(DynamicAmount.XValue),
                maxValue = DynamicAmounts.yourLifeTotal() / 2,
                prompt = "Pay 2 life and draw a card how many times?"
            ) then Effects.SkipNextStepOrPhase(TurnPart.DRAW_STEP)
        },
        rule(19, "Pity", "If a player has less life than each other player, that player gains 5 life.") {
            effect = Effects.ForEachPlayer(
                Player.Each,
                Effects.If(
                    Conditions.PlayerHasMoreThanEachOtherPlayer(Player.You, -DynamicAmounts.lifeTotal(Player.You)),
                    Effects.GainLife(5)
                )
            )
        },
        rule(20, "Chain Lightning", "Deal 3 damage to any target.") {
            val any = target(Targets.Any)
            effect = Effects.DealDamage(3, any)
        },
        rule(22, "Mind Control", "Gain control of target creature.") {
            val creature = target(TargetFilter.Creature)
            effect = Effects.GainControl(creature)
        },
        rule(23, "Life Exchange", "Exchange life totals with an opponent chosen at random.") {
            effect = Effects.Pipeline {
                val opponent = selectTarget(Targets.Opponent, nonTargeting = true, atRandom = true)
                run(Effects.ExchangeLifeTotals(opponent.asTarget(0)))
            }
        },
        rule(25, "Sunblast Angel", "Destroy all tapped creatures.") {
            effect = Effects.DestroyAll(GameObjectFilter.Creature.tapped())
        },
        rule(26, "Hymn to Tourach", "Target player discards two cards at random.") {
            val player = target(Targets.Player)
            effect = Patterns.Hand.discardRandom(2, player)
        },
        rule(27, "Exchange", "Exchange life totals with target opponent.") {
            val opponent = target(Targets.Opponent)
            effect = Effects.ExchangeLifeTotals(opponent)
        },
        rule(28, "Rick", "You got Rick rolled! You lose 5 life and sacrifice a creature.") {
            effect = Effects.LoseLife(5, EffectTarget.Controller) then
                Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.Controller)
        },
        rule(29, "Cleanup", "Exile all cards from all graveyards.") {
            effect = Effects.Pipeline {
                val graveyards = gather(CardSource.FromZone(zone = Zone.GRAVEYARD, player = Player.Each, filter = GameObjectFilter.Any))
                exile(graveyards)
            }
        },
        rule(30, "Ancest-roll Recall", "Target opponent creates a 3/3 green Wolf creature token.") {
            val opponent = target(Targets.Opponent)
            effect = Effects.CreateToken(
                power = 3, toughness = 3, colors = setOf(Color.GREEN), creatureTypes = setOf("Wolf"),
                controller = opponent
            )
        },
        rule(31, "Tremble", "Each player sacrifices a land.") {
            effect = Effects.Sacrifice(GameObjectFilter.Land, 1, EffectTarget.PlayerRef(Player.Each))
        },
        rule(32, "Confiscate", "Gain control of target permanent.") {
            val permanent = target(TargetFilter.Permanent)
            effect = Effects.GainControl(permanent)
        },
        rule(33, "Reset", "Each player's life total becomes 10.") {
            effect = Effects.ForEachPlayer(Player.Each, Effects.SetLifeTotal(10))
        },
        rule(34, "Price of Immortality", "Discard a card. You gain 6 life.") {
            effect = Effects.Discard(1) then Effects.GainLife(6)
        },
        rule(35, "Giant Growth", "Roll a six-sided die. Target creature gets +X/+X until end of turn, where X is the result.") {
            val creature = target(TargetFilter.Creature)
            effect = Effects.Pipeline {
                val roll = rollDiceResult(sides = 6)
                run(Effects.ModifyStats(roll.amount, roll.amount, creature))
            }
        },
        rule(36, "Mindslaver", "You control target opponent during their next turn, chaos roll included.") {
            val opponent = target(Targets.Opponent)
            effect = Effects.HijackNextTurn(opponent)
        },
        rule(37, "Regrowth", "Return target card from your graveyard to your hand.") {
            val card = target(graveyardOfYours(GameObjectFilter.Any))
            effect = Effects.Move(card, Zone.HAND, fromZone = Zone.GRAVEYARD)
        },
        rule(38, "rekeeS mrotS", "Deal damage to yourself equal to the number of cards in your hand.") {
            effect = Effects.DealDamage(DynamicAmounts.cardsInYourHand(), EffectTarget.Controller)
        },
        rule(39, "Price of Strength", "Put two +1/+1 counters on each creature you control. You lose 4 life.") {
            effect = Effects.ForEachInGroup(
                yourCreatures,
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.IterationEntity)
            ) then Effects.LoseLife(4, EffectTarget.Controller)
        },
        rule(41, "Price of Power", "Sacrifice a land. Deal 4 damage to any target.") {
            val any = target(Targets.Any)
            effect = Effects.Sacrifice(GameObjectFilter.Land, 1, EffectTarget.Controller) then Effects.DealDamage(4, any)
        },
        rule(42, "Backfire", "Target opponent draws a card. You lose 2 life.") {
            val opponent = target(Targets.Opponent)
            effect = Effects.DrawCards(1, opponent) then Effects.LoseLife(2, EffectTarget.Controller)
        },
        rule(43, "Stronghold Discipline", "Each player loses 1 life for each creature they control.") {
            effect = Effects.ForEachPlayer(
                Player.Each,
                Effects.LoseLife(DynamicAmounts.creaturesYouControl(), EffectTarget.Controller)
            )
        },
        rule(45, "Scar", "Roll a six-sided die. Put that many -1/-1 counters on target creature.") {
            val creature = target(TargetFilter.Creature)
            effect = Effects.Pipeline {
                val roll = rollDiceResult(sides = 6)
                run(Effects.AddDynamicCounters(CounterType.MINUS_ONE_MINUS_ONE, roll.amount, creature))
            }
        },
        rule(
            46, "Meet in the Middle",
            "If you have more than four cards in hand, discard down to four. If you have fewer than four, draw up to four."
        ) {
            effect = Effects.If(
                Conditions.CardsInHandAtLeast(5),
                Effects.Discard(DynamicAmounts.cardsInYourHand() - 4)
            ) then Effects.If(
                Conditions.CardsInHandAtMost(3),
                Effects.DrawCards(4 - DynamicAmounts.cardsInYourHand())
            )
        },
        rule(47, "Unearth", "Return target creature card with mana value 3 or less from your graveyard to the battlefield.") {
            val card = target(graveyardOfYours(GameObjectFilter.Creature.manaValueAtMost(3)))
            effect = Effects.Move(card, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
        },
        rule(48, "Blue", "When you next cast a spell this turn, counter that spell.") {
            effect = Effects.CreateDelayedTrigger(
                effect = Effects.CounterTriggeringSpell(),
                trigger = Triggers.you.casts(),
                fireOnce = true
            )
        },
        rule(49, "Peach Garden Oath Reversed", "You lose 2 life for each creature you control.") {
            effect = Effects.LoseLife(DynamicAmounts.creaturesYouControl() * 2, EffectTarget.Controller)
        },
        rule(50, "Unhinge", "Target player discards a card. Draw a card.") {
            val player = target(Targets.Player)
            effect = Patterns.Hand.discardCards(1, player) then Effects.DrawCards(1)
        },
        rule(51, "Prey Upon", "Target creature you control fights target creature you don't control.") {
            val mine = target(TargetFilter.CreatureYouControl)
            val theirs = target(TargetFilter(GameObjectFilter.Creature.opponentControls()))
            effect = Effects.Fight(mine, theirs)
        },
        rule(52, "Price of Support", "Sacrifice a creature, then create four 1/1 Spirit creature tokens that are all colors.") {
            effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.Controller) then
                Effects.CreateToken(
                    power = 1, toughness = 1, colors = Color.entries.toSet(), creatureTypes = setOf("Spirit"), count = 4
                )
        },
        rule(53, "Balloon", "Until end of turn, whenever you tap a land for mana, you lose 1 life.") {
            effect = Effects.CreateGlobalTriggeredAbility(
                grantedTriggeredAbility {
                    trigger = Triggers.you.tapsLandForMana()
                    effect = Effects.LoseLife(1, EffectTarget.Controller)
                    description = "Whenever you tap a land for mana, you lose 1 life."
                },
                duration = Duration.EndOfTurn
            )
        },
        rule(54, "Tap", "Tap target permanent.") {
            val permanent = target(TargetFilter.Permanent)
            effect = Effects.Tap(permanent)
        },
        rule(55, "Kill Walker", "Destroy target planeswalker.") {
            val walker = target(TargetFilter.Planeswalker)
            effect = Effects.Destroy(walker)
        },
        rule(56, "Power Surge", "Deal damage to yourself equal to the number of basic lands you control.") {
            effect = Effects.DealDamage(
                DynamicAmounts.battlefield(Player.You, GameObjectFilter.BasicLand).count(),
                EffectTarget.Controller
            )
        },
        rule(57, "Lurking Evil", "Create a 4/4 black Horror creature token with flying. You lose 4 life.") {
            effect = Effects.CreateToken(
                power = 4, toughness = 4, colors = setOf(Color.BLACK), creatureTypes = setOf("Horror"),
                keywords = setOf(Keyword.FLYING)
            ) then Effects.LoseLife(4, EffectTarget.Controller)
        },
        rule(58, "Blessed Wine", "You gain 1 life. Draw a card.") {
            effect = Effects.GainLife(1) then Effects.DrawCards(1)
        },
        rule(59, "Cloak of Mists", "Target creature can't be blocked for the rest of the game.") {
            val creature = target(TargetFilter.Creature)
            effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature, Duration.Permanent)
        },
        rule(60, "Walker Control", "Gain control of target planeswalker.") {
            val walker = target(TargetFilter.Planeswalker)
            effect = Effects.GainControl(walker)
        },
        rule(61, "Time Walk", "Take an extra turn after this one.") {
            effect = Effects.TakeExtraTurn()
        },
        rule(
            62, "Wanna Bet?",
            "Choose target permanent an opponent controls and target permanent you control. Flip a coin. " +
                "If you win, gain control of the first. If you lose, that opponent gains control of the second."
        ) {
            val theirs = target(TargetFilter(GameObjectFilter.Permanent.opponentControls()))
            val yours = target(TargetFilter.PermanentYouControl)
            effect = Effects.FlipCoin(
                wonEffect = Effects.GainControl(theirs),
                lostEffect = Effects.GiveControl(yours, EffectTarget.TargetController)
            )
        },
        rule(63, "Lat-Nam's Legacy", "Shuffle a card from your hand into your library. Draw a card.") {
            effect = Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Any))
                val chosen = chooseExactly(1, from = hand, chooser = Chooser.Controller, prompt = "Shuffle a card from your hand into your library")
                move(chosen, CardDestination.ToZone(Zone.LIBRARY, Player.You))
                run(Effects.ShuffleLibrary())
                run(Effects.DrawCards(1))
            }
        },
        rule(64, "Aether Snap", "Remove all counters from all creatures.") {
            effect = Effects.ForEachInGroup(GroupFilter.AllCreatures, Effects.RemoveAllCounters(EffectTarget.IterationEntity))
        },
        rule(65, "Steal Artifact", "Gain control of target artifact.") {
            val artifact = target(TargetFilter.Artifact)
            effect = Effects.GainControl(artifact)
        },
        rule(67, "Whale Power", "If you control three or fewer lands, create a 5/5 blue Whale creature token.") {
            effect = Effects.If(
                Conditions.Not(Conditions.ControlLandsAtLeast(4)),
                Effects.CreateToken(power = 5, toughness = 5, colors = setOf(Color.BLUE), creatureTypes = setOf("Whale"))
            )
        },
        rule(68, "Strongarm Tactics", "Each player discards a card. Then each player who didn't discard a creature card this way loses 4 life.") {
            effect = Effects.EachPlayerDiscardsOrLosesLife(lifeLoss = 4)
        },
        rule(69, "Leonin Sun Standard", "Creatures you control get +1/+1 until end of turn.") {
            effect = Effects.ForEachInGroup(yourCreatures, Effects.ModifyStats(1, 1, EffectTarget.IterationEntity))
        },
        rule(70, "Psionic Blast", "Deal 4 damage to any target and 2 damage to you.") {
            val any = target(Targets.Any)
            effect = Effects.DealDamage(4, any) then Effects.DealDamage(2, EffectTarget.Controller)
        },
        rule(72, "Defender", "Creatures you control gain defender until end of turn.") {
            effect = Effects.ForEachInGroup(yourCreatures, Effects.GrantKeyword(Keyword.DEFENDER, EffectTarget.IterationEntity))
        },
        rule(73, "Charge", "Creatures you control attack this turn if able.") {
            effect = Effects.MustAttackGroup(yourCreatures)
        },
        rule(74, "Ponder", "Look at the top three cards of your library, then put them back in any order. You may shuffle.") {
            effect = Patterns.Library.lookAtTopAndReorder(count = 3) then Effects.May(Effects.ShuffleLibrary())
        },
        rule(75, "Footbottom Feast", "Put any number of target creature cards from your graveyard on top of your library. Draw a card.") {
            targets(TargetFilter.CreatureInYourGraveyard, unlimited = true)
            effect = Effects.Pipeline {
                val cards = gather(CardSource.ChosenTargets)
                toLibraryTop(cards)
                run(Effects.DrawCards(1))
            }
        },
        rule(77, "Krosan Forest", "Each player mills five cards.") {
            effect = Effects.ForEachPlayer(Player.Each, Patterns.Library.mill(5))
        },
        rule(78, "Terminate", "Destroy target creature. It can't be regenerated.") {
            val creature = target(TargetFilter.Creature)
            effect = Effects.Destroy(creature, noRegenerate = true)
        },
        rule(
            79, "Direct Damage",
            "Tap any number of untapped lands you control. Deal X damage to each creature, where X is the number of lands tapped this way."
        ) {
            effect = Effects.Pipeline {
                val lands = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Land.untapped()))
                val tapped = chooseAnyNumber(from = lands, chooser = Chooser.Controller, prompt = "Tap any number of untapped lands you control")
                run(Effects.TapCollection(tapped))
                run(
                    Effects.ForEachInGroup(
                        GroupFilter.AllCreatures,
                        Effects.DealDamage(DynamicAmounts.distinctEntitiesIn(tapped), EffectTarget.IterationEntity)
                    )
                )
            }
        },
        rule(80, "Cloudshift", "Exile target creature you control, then return that card to the battlefield under your control.") {
            val creature = target(TargetFilter.CreatureYouControl)
            effect = Effects.Exile(creature) then Effects.PutOntoBattlefieldUnderYourControl(creature)
        },
        rule(81, "Gale Force", "Deal 5 damage to each creature with flying.") {
            effect = Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING)),
                Effects.DealDamage(5, EffectTarget.IterationEntity)
            )
        },
        rule(82, "Storm Seeker", "Deal damage to target player equal to the number of cards in that player's hand.") {
            val player = target(Targets.Player)
            effect = Effects.DealDamage(DynamicAmounts.zone(player.asPlayer, Zone.HAND).count(), player)
        },
        rule(83, "Mana Short", "Tap all lands you control.") {
            effect = Patterns.Group.tapAll(GroupFilter(GameObjectFilter.Land.youControl()))
        },
        rule(
            84, "Wanna-be Black Lotus",
            "Add {C}{C}{C}. Until end of turn, you don't lose this mana as steps and phases end. Spend it only to cast creature spells."
        ) {
            effect = Effects.AddColorlessMana(3, restriction = ManaRestriction.CreatureSpellsOnly)
        },
        rule(85, "Flying", "Creatures you control gain flying until end of turn.") {
            effect = Effects.ForEachInGroup(yourCreatures, Effects.GrantKeyword(Keyword.FLYING, EffectTarget.IterationEntity))
        },
        rule(86, "Time Crawl", "Pass the dice — end the turn.") {
            effect = Effects.EndTheTurn
        },
        rule(87, "First", "Target creature you control gains lifelink until end of turn.") {
            val creature = target(TargetFilter.CreatureYouControl)
            effect = Effects.GrantKeyword(Keyword.LIFELINK, creature)
        },
        rule(88, "Temporal Spring", "Put target permanent on top of its owner's library.") {
            val permanent = target(TargetFilter.Permanent)
            effect = Effects.PutOnTopOfLibrary(permanent)
        },
        rule(89, "Gamble", "Search your library for a card, put it into your hand, discard a card at random, then shuffle.") {
            effect = Patterns.Library.searchLibrary(filter = GameObjectFilter.Any, destination = SearchDestination.HAND) then
                Patterns.Hand.discardRandom(1)
        },
        rule(90, "Vindicate", "Destroy target permanent.") {
            val permanent = target(TargetFilter.Permanent)
            effect = Effects.Destroy(permanent)
        },
        rule(91, "Forget", "Target player discards two cards, then draws two cards.") {
            val player = target(Targets.Player)
            effect = Patterns.Hand.discardCards(2, player) then Effects.DrawCards(2, player)
        },
        rule(92, "Sleep", "Tap all creatures target player controls. Those creatures don't untap during that player's next untap step.") {
            val player = target(Targets.Player)
            effect = Patterns.Group.tapAll(GroupFilter(GameObjectFilter.Creature.targetPlayerControls(player))) then
                Effects.SkipUntap(player, GameObjectFilter.Creature, lockedAtResolution = true)
        },
        rule(93, "Wheel of Fortune", "Each player discards their hand, then draws seven cards.") {
            effect = Effects.ForEachPlayer(Player.Each, Patterns.Hand.discardHand()) then
                Effects.ForEachPlayer(Player.Each, Effects.DrawCards(7))
        },
        rule(94, "Second Roll", "Roll a six-sided die. On 1–3, draw a card. On 4–6, discard a card.") {
            effect = Effects.Pipeline {
                val roll = rollDiceResult(sides = 6)
                ifThen(Conditions.CompareAmounts(roll.amount, ComparisonOperator.LTE, 3)) {
                    run(Effects.DrawCards(1))
                }
                ifThen(Conditions.CompareAmounts(roll.amount, ComparisonOperator.GTE, 4)) {
                    run(Effects.Discard(1))
                }
            }
        },
        rule(95, "Manabarbs", "Until end of turn, whenever a player taps a land for mana, Chaos deals 1 damage to that player.") {
            effect = Effects.CreateGlobalTriggeredAbility(
                grantedTriggeredAbility {
                    trigger = Triggers.anyPlayer.tapsLandForMana()
                    effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
                    description = "Whenever a player taps a land for mana, Chaos deals 1 damage to that player."
                },
                duration = Duration.EndOfTurn
            )
        },
        rule(96, "Ill-Gotten Gains", "Each player discards their hand, then returns up to three cards from their graveyard to their hand.") {
            effect = Effects.ForEachPlayer(Player.Each, Patterns.Hand.discardHand()) then
                Effects.ForEachPlayer(
                    Player.EachStartingWithYou,
                    Effects.Pipeline {
                        val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Any))
                        val chosen = chooseUpTo(
                            3, from = graveyard, chooser = Chooser.Controller,
                            prompt = "Return up to three cards from your graveyard to your hand"
                        )
                        toHand(chosen)
                    }
                )
        },
        rule(97, "Flashback", "Target instant or sorcery card in your graveyard gains flashback until end of turn. Its flashback cost is its mana cost.") {
            val card = target(TargetFilter.InstantOrSorceryInYourGraveyard)
            effect = Effects.GrantFlashback(card)
        },
        rule(99, "Boomerang", "Return target permanent to its owner's hand.") {
            val permanent = target(TargetFilter.Permanent)
            effect = Effects.ReturnToHand(permanent)
        },
        rule(100, "Retribution of the Meek", "Destroy all creatures with power 4 or greater.") {
            effect = Effects.DestroyAll(GameObjectFilter.Creature.powerAtLeast(4))
        },
        rule(101, "Next", "Target opponent skips their next turn.") {
            val opponent = target(Targets.Opponent)
            effect = Effects.SkipNextTurn(opponent)
        },
        rule(102, "Graveyard", "You lose 1 life for each card in your graveyard, up to 5.") {
            effect = Effects.LoseLife(
                DynamicAmounts.min(DynamicAmounts.cardsInYourGraveyard(), DynamicAmounts.fixed(5)),
                EffectTarget.Controller
            )
        },
        rule(103, "New Hand", "Discard your hand, then draw seven cards. Skip your next draw step.") {
            effect = Patterns.Hand.discardHand() then Effects.DrawCards(7) then
                Effects.SkipNextStepOrPhase(TurnPart.DRAW_STEP)
        },
        rule(104, "Hideous End", "Destroy target nonblack creature. Its controller loses 2 life.") {
            val creature = target(TargetFilter(GameObjectFilter.Creature.notColor(Color.BLACK)))
            effect = Effects.Destroy(creature) then Effects.LoseLife(2, EffectTarget.TargetController)
        },
        rule(105, "Haste", "Until end of turn, whenever a creature enters, it gains haste until end of turn.") {
            effect = Effects.CreateGlobalTriggeredAbility(
                grantedTriggeredAbility {
                    trigger = Triggers.a(GameObjectFilter.Creature).enters()
                    effect = Effects.GrantKeyword(Keyword.HASTE, EffectTarget.TriggeringEntity)
                    description = "Whenever a creature enters, it gains haste until end of turn."
                },
                duration = Duration.EndOfTurn
            )
        },
        rule(106, "Thalia", "Noncreature spells cost {1} more to cast this turn.") {
            effect = Effects.IncreaseSpellCosts(
                amount = DynamicAmounts.fixed(1),
                player = Player.Each,
                spellFilter = GameObjectFilter.Noncreature
            )
        },
        rule(107, "Extra Lands", "You may play an additional land this turn.") {
            effect = Effects.PlayAdditionalLands(1)
        },
        rule(109, "Clone", "Create a token that's a copy of target creature.") {
            val creature = target(TargetFilter.Creature)
            effect = Effects.CreateTokenCopyOfTarget(creature)
        },
        rule(110, "Looting", "Draw a card, then discard a card.") {
            effect = Effects.DrawCards(1) then Effects.Discard(1)
        },
        rule(111, "Probe", "Look at target opponent's hand.") {
            val opponent = target(Targets.Opponent)
            effect = Effects.LookAtHand(opponent)
        },
        rule(112, "Fog", "Prevent all combat damage that would be dealt this turn.") {
            effect = Effects.PreventAllCombatDamage()
        },
        rule(114, "Plummet", "Destroy target creature with flying.") {
            val creature = target(TargetFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING)))
            effect = Effects.Destroy(creature)
        },
        rule(116, "Zombify", "Return target creature card from your graveyard to the battlefield.") {
            val card = target(TargetFilter.CreatureInYourGraveyard)
            effect = Effects.Move(card, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
        },
        rule(117, "Singe-Mind Ogre", "Target player reveals a card at random from their hand, then loses life equal to its mana value.") {
            val player = target(Targets.Player)
            effect = Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, player.asPlayer, GameObjectFilter.Any))
                val revealed = chooseRandom(1, from = hand)
                reveal(revealed)
                run(Effects.LoseLife(DynamicAmounts.manaValueSumOf(revealed), player))
            }
        },
        rule(118, "Gain", "Sacrifice a land, creature, or artifact, then draw a card.") {
            effect = Effects.Sacrifice(
                GameObjectFilter.Land.or(GameObjectFilter.Creature).or(GameObjectFilter.Artifact), 1, EffectTarget.Controller
            ) then Effects.DrawCards(1)
        },
        rule(
            120, "No Spells +1",
            "At the beginning of your end step this turn, if you cast no spells this turn, put a +1/+1 counter on each creature you control."
        ) {
            effect = Effects.CreateDelayedTrigger(
                effect = Effects.If(
                    Conditions.Not(Conditions.YouCastSpellsThisTurn(atLeast = 1)),
                    Effects.ForEachInGroup(
                        yourCreatures,
                        Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
                    )
                ),
                step = Step.END
            )
        },
        rule(122, "Serum Visions", "Scry 2.") {
            effect = Effects.Scry(2)
        },
        rule(
            123, "Squirrel Nest",
            "Tap all lands you control. Create a 1/1 green Squirrel creature token for each land you control."
        ) {
            effect = Patterns.Group.tapAll(GroupFilter(GameObjectFilter.Land.youControl())) then
                Effects.CreateToken(
                    count = DynamicAmounts.landsYouControl(),
                    power = 1, toughness = 1, colors = setOf(Color.GREEN), creatureTypes = setOf("Squirrel")
                )
        },
        rule(124, "Meteor Storm", "Deal 2 damage to each creature and each player.") {
            effect = Effects.ForEachInGroup(GroupFilter.AllCreatures, Effects.DealDamage(2, EffectTarget.IterationEntity)) then
                Effects.DealDamage(2, EffectTarget.PlayerRef(Player.Each))
        },
        rule(125, "Monstrous Growth", "Put a +2/+2 counter on target creature.") {
            val creature = target(TargetFilter.Creature)
            effect = Effects.AddCounters(CounterType.PLUS_TWO_PLUS_TWO, 1, creature)
        },
        rule(126, "Jokulhaups", "Destroy all artifacts, creatures, and lands.") {
            effect = Effects.DestroyAll(GameObjectFilter.Artifact.or(GameObjectFilter.Creature).or(GameObjectFilter.Land))
        },
        rule(127, "Solid Fog", "Until your next turn, no combat phases happen: you skip combat this turn and each opponent skips combat during their next turn.") {
            effect = Effects.SkipStepOrPhaseThisTurn(TurnPart.COMBAT_PHASE, EffectTarget.Controller) then
                Effects.ForEachPlayer(Player.EachOpponent, Effects.SkipCombatPhases(EffectTarget.Controller))
        },
        rule(128, "Library of Tolaria", "Starting with you, each player may pay 4 life. Each player who does draws a card.") {
            effect = Effects.ForEachPlayer(
                Player.EachStartingWithYou,
                Effects.MayPay(Effects.PayLife(4), Effects.DrawCards(1))
            )
        },
        rule(129, "Mine Field", "Starting with you, each player may pay 6 life. Each player who does destroys a creature of their choice.") {
            effect = Effects.ForEachPlayer(
                Player.EachStartingWithYou,
                Effects.MayPay(
                    Effects.PayLife(6),
                    Effects.Pipeline {
                        val creatures = gather(CardSource.BattlefieldMatching(GameObjectFilter.Creature, Player.Each))
                        val chosen = chooseExactly(1, from = creatures, chooser = Chooser.Controller, prompt = "Destroy a creature")
                        destroy(chosen)
                    }
                )
            )
        },
        rule(131, "3 Life", "Choose one: pay 3 life and destroy a creature of your choice; or gain 3 life and sacrifice a creature.") {
            effect = Effects.ChooseAction(
                listOf(
                    EffectChoice(
                        "Pay 3 life and destroy a creature",
                        Effects.PayLife(3) then Effects.Pipeline {
                            val creatures = gather(CardSource.BattlefieldMatching(GameObjectFilter.Creature, Player.Each))
                            val chosen = chooseExactly(1, from = creatures, chooser = Chooser.Controller, prompt = "Destroy a creature")
                            destroy(chosen)
                        }
                    ),
                    EffectChoice(
                        "Gain 3 life and sacrifice a creature",
                        Effects.GainLife(3) then Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.Controller)
                    ),
                )
            )
        },
        rule(132, "Life Perm", "Each player gains 1 life for each permanent they control.") {
            effect = Effects.ForEachPlayer(
                Player.Each,
                Effects.GainLife(DynamicAmounts.battlefield(Player.You, GameObjectFilter.Permanent).count())
            )
        },
        rule(141, "Elemental Creature", "Create two 3/1 red Elemental creature tokens with haste. Exile them at the beginning of the next end step.") {
            effect = Effects.CreateToken(
                power = 3, toughness = 1, colors = setOf(Color.RED), creatureTypes = setOf("Elemental"),
                keywords = setOf(Keyword.HASTE), count = 2, exileAtStep = Step.END
            )
        },
        rule(142, "Draw, Add Mana", "Draw a card, then add one mana of any color.") {
            effect = Effects.DrawCards(1) then Effects.AddAnyColorMana(1)
        },
        rule(143, "10 Life", "Target player's life total becomes 10.") {
            val player = target(Targets.Player)
            effect = Effects.SetLifeTotal(10, player)
        },
        rule(145, "Tap All Cards", "Tap all untapped lands and creatures you control. You gain 1 life for each permanent tapped this way.") {
            val untappedLandsAndCreatures = GameObjectFilter.Land.or(GameObjectFilter.Creature).untapped()
            effect = Effects.GainLife(DynamicAmounts.battlefield(Player.You, untappedLandsAndCreatures).count()) then
                Patterns.Group.tapAll(GroupFilter(GameObjectFilter.Land.or(GameObjectFilter.Creature).youControl()))
        },
        rule(149, "Discard", "Discard your hand. Create an X/X colorless Elemental creature token with haste, where X is the number of cards discarded this way.") {
            effect = Effects.Pipeline {
                val discarded = storeNumber(DynamicAmounts.cardsInYourHand())
                run(Patterns.Hand.discardHand())
                run(
                    Effects.CreateToken(
                        power = 0, toughness = 0, creatureTypes = setOf("Elemental"), keywords = setOf(Keyword.HASTE),
                        dynamicPower = discarded.amount, dynamicToughness = discarded.amount
                    )
                )
            }
        },
        rule(197, "Goblin Science", "Create a 1/1 red Goblin creature token. Roll a six-sided die. On a 6, deal 4 damage to target creature.") {
            val creature = target(TargetFilter.Creature)
            effect = Effects.CreateToken(power = 1, toughness = 1, colors = setOf(Color.RED), creatureTypes = setOf("Goblin")) then
                Effects.Pipeline {
                    val roll = rollDiceResult(sides = 6)
                    ifThen(Conditions.CompareAmounts(roll.amount, ComparisonOperator.GTE, 6)) {
                        run(Effects.DealDamage(4, creature))
                    }
                }
        },
        rule(199, "Necromancer's Gift", "Starting with you, each player who controls a Swamp may return a creature card from their graveyard to their hand.") {
            effect = eachPlayerWhoControls(
                "Swamp",
                Effects.Pipeline {
                    val creatures = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature))
                    val chosen = chooseUpTo(1, from = creatures, chooser = Chooser.Controller, prompt = "You may return a creature card to your hand")
                    toHand(chosen)
                }
            )
        },
        rule(200, "Dread Harvest", "Starting with you, each player who controls a Swamp exiles two cards from their graveyard.") {
            effect = eachPlayerWhoControls(
                "Swamp",
                Effects.Pipeline {
                    val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Any))
                    val chosen = chooseExactly(2, from = graveyard, chooser = Chooser.Controller, prompt = "Exile two cards from your graveyard")
                    exile(chosen)
                }
            )
        },
        rule(201, "Tidal Drown", "Starting with you, each player who controls an Island returns a permanent they control to its owner's hand.") {
            effect = eachPlayerWhoControls(
                "Island",
                Effects.Pipeline {
                    val permanents = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Permanent))
                    val chosen = chooseExactly(1, from = permanents, chooser = Chooser.Controller, prompt = "Return a permanent you control to its owner's hand")
                    toHand(chosen)
                }
            )
        },
        rule(202, "Deep Insight", "Each player who controls an Island draws a card.") {
            effect = eachPlayerWhoControls("Island", Effects.DrawCards(1))
        },
        rule(203, "Burn the Weak", "Each player who controls a Mountain sacrifices a creature with power 1 or 2.") {
            effect = eachPlayerWhoControls(
                "Mountain",
                Effects.Sacrifice(GameObjectFilter.Creature.powerAtLeast(1).powerAtMost(2), 1, EffectTarget.Controller)
            )
        },
        rule(204, "Frenzy Ignition", "Starting with you, each player who controls a Mountain deals 2 damage to any target of their choice.") {
            effect = eachPlayerWhoControls(
                "Mountain",
                Effects.Pipeline {
                    val chosen = selectTarget(Targets.Any, prompt = "Deal 2 damage to any target")
                    run(Effects.DealDamage(2, chosen.asTarget(0)))
                }
            )
        },
        rule(205, "Ramp Renewal", "Each player who controls a Forest searches their library for a basic land card, puts it onto the battlefield tapped, then shuffles.") {
            effect = eachPlayerWhoControls(
                "Forest",
                Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.BasicLand,
                    destination = SearchDestination.BATTLEFIELD,
                    entersTapped = true
                )
            )
        },
        rule(
            206, "Entangling Roots",
            "Starting with you, each player who controls a Forest taps a creature they control. It doesn't untap during its controller's next untap step."
        ) {
            effect = eachPlayerWhoControls(
                "Forest",
                Effects.Pipeline {
                    val creatures = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Creature))
                    val chosen = chooseExactly(1, from = creatures, chooser = Chooser.Controller, prompt = "Tap a creature you control")
                    run(Effects.TapCollection(chosen))
                    run(
                        Effects.GrantKeyword(
                            AbilityFlag.DOESNT_UNTAP, chosen.asTarget(0), Duration.UntilAfterAffectedControllersNextUntap()
                        )
                    )
                }
            )
        },
        rule(207, "Purifying Wave", "Each player who controls a Plains sacrifices an enchantment.") {
            effect = eachPlayerWhoControls("Plains", Effects.Sacrifice(GameObjectFilter.Enchantment, 1, EffectTarget.Controller))
        },
        rule(208, "Healing Light", "Each player who controls a Plains gains 2 life.") {
            effect = eachPlayerWhoControls("Plains", Effects.GainLife(2))
        },
    ) }

    /**
     * Rules of the original table the engine doesn't play automatically, by id, with the reason —
     * so the gap is visible rather than silent. Most change the chaos roll itself; the rest have no
     * faithful reading in the engine's vocabulary yet.
     */
    val notAutomated: Map<Int, String> = mapOf(
        10 to "eugitaF (you can't draw cards this turn) — no \"can't draw\" effect for a player yet",
        11 to "Reins of Power — exchanging control of two whole boards needs a snapshot exchange effect",
        18 to "Revenge is Sweet — destroying one permanent sharing a type with the sacrificed one",
        21 to "Illicit Auction — the engine's life bid is two-sided and only rewards the roller",
        24 to "Rare-B-Gone — no rarity filter",
        40 to "Possibility — sacrificing two lands as an optional cost for exile",
        44 to "Topple — no \"greatest power\" target restriction",
        66 to "Stasis — skipping untap steps until someone pays",
        71 to "(no rule with this id)",
        76 to "Last Rites — discard any number, then choose that many from an opponent's hand",
        98 to "Price of Strength (duplicate of id 39)",
        108 to "Squee — opponents play with their hands revealed until they lose life",
        113 to "Goat Land — animating lands into Goats while keeping their abilities",
        115 to "Colorless Lands — lands are already colorless",
        119 to "Frost Mana — granted mana ability with an untap rider",
        121 to "No Spell Clone — delayed copy of any creature",
        130 to "Battle Cry — no battle cry keyword",
        133 to "Extra Mana — temporary Mana Flare for one player's lands",
        134 to "Opponent Library — reordering another player's library",
        135 to "Intimidation — \"you can't be attacked\"",
        136 to "Calm — changes the chaos roll",
        137 to "Storm — changes the chaos roll",
        138 to "Activation Cost — paying other permanents' activation costs",
        139 to "Double — doubling all life gain and damage",
        140 to "Master of Chaos — changes the chaos roll",
        146 to "Limit Attack — attack restriction to the player on your left",
        147 to "More Tokens — ambiguous wording",
        148 to "Destroy at Random — destroy one of several targets at random",
        150 to "Land pr Not — ambiguous wording",
        198 to "Left or Right — needs seat positions",
    )

    /** The roll: one rule at random from the ones that can legally happen, never one this player rolled before. */
    val roll: ModalEffect = ModalEffect(
        modes = all.map { it.mode },
        chooseCount = 1,
        excludePreviouslyChosenModes = true,
        countsAsModalSpell = false,
        chooseAtRandom = true,
    )

    /** The Chaos emblem's ability: [EMBLEM_TEXT]. */
    val upkeepRoll: TriggeredAbility = AbilityIdScope.within("$SOURCE_NAME emblem") {
        grantedTriggeredAbility {
            trigger = Triggers.you.beginningOf(Step.UPKEEP)
            effect = roll
            description = EMBLEM_TEXT
        }
    }

    /** The rule called [name], for tests and tooling. */
    fun named(name: String): ChaosRule =
        all.firstOrNull { it.name == name } ?: error("No chaos rule named '$name'")
}
