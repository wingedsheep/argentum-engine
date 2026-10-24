package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mila, Crafty Companion // Lukka, Wayward Bonder — Strixhaven: School of Mages #153 (canonical
 * printing)
 * {1}{W}{W} Legendary Creature — Fox 2/3 // {4}{R}{R} Legendary Planeswalker — Lukka · Loyalty 5
 * Modal double-faced card (CR 712): cast either face; Lukka cast from hand enters with its own
 * printed loyalty.
 *
 * Front — Mila:
 *  - "Whenever an opponent attacks one or more planeswalkers you control, put a loyalty counter
 *    on each planeswalker you control." An opposing attack declaration ([Triggers.anOpponent]
 *    `attacks`) counting only attackers whose defender is a planeswalker you control — attacking
 *    you or your planeswalkers, but not you yourself. It fires once per declaration however many
 *    creatures or planeswalkers are involved, and the counters go on *every* planeswalker you
 *    control, attacked or not (rulings).
 *  - "Whenever a permanent you control becomes the target of a spell or ability an opponent
 *    controls, you may draw a card." The Pawpatch Recruit trigger widened to any permanent, with
 *    an optional draw; it resolves before the targeting spell or ability (ruling).
 *
 * Back — Lukka:
 *  - +1: a pipeline choosing up to one card from hand to discard, then two draws if a creature
 *    card was discarded this way, one if a noncreature card was, none if nothing was.
 *  - −2: the Greasefang shape — the targeted creature card is moved to the battlefield as a
 *    tracked collection, gains haste, and a delayed trigger on your next upkeep exiles that
 *    same object (a new object after it leaves the battlefield is left alone, CR 603.7c).
 *  - −7: an emblem with Warstorm Surge's trigger — the entering creature itself deals damage
 *    equal to its power to any target.
 */
private val opponentAttackingYourPlaneswalker =
    GameObjectFilter.Creature
        .attackingYouOrYourPlaneswalkers()
        .withStatePredicate(StatePredicate.Not(StatePredicate.IsAttackingPlayer(EffectTarget.Controller)))

private val MilaCraftyCompanionFront = card("Mila, Crafty Companion") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Fox"
    power = 2
    toughness = 3
    oracleText = "Whenever an opponent attacks one or more planeswalkers you control, put a loyalty " +
        "counter on each planeswalker you control.\n" +
        "Whenever a permanent you control becomes the target of a spell or ability an opponent " +
        "controls, you may draw a card."

    triggeredAbility {
        trigger = Triggers.anOpponent.attacks(with = opponentAttackingYourPlaneswalker)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Planeswalker.youControl()),
            Effects.AddCounters(CounterType.LOYALTY, 1, EffectTarget.IterationEntity),
        )
        description = "Whenever an opponent attacks one or more planeswalkers you control, put a " +
            "loyalty counter on each planeswalker you control."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.youControl()).becomesTarget(byOpponent = true)
        effect = Effects.May(Effects.DrawCards(1))
        description = "Whenever a permanent you control becomes the target of a spell or ability " +
            "an opponent controls, you may draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "153"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e4e0f81-f92b-4a3a-bb29-adcc3de211b4.jpg?1783927336"
        ruling("2021-04-16", "When Mila's first ability resolves, you will put a loyalty counter on all planeswalkers you control, even ones that aren't being attacked.")
        ruling("2021-04-16", "Mila's first ability triggers only once no matter how many creatures are attacking or how many planeswalkers are being attacked.")
        ruling("2021-04-16", "Mila's second ability will resolve before the spell or ability that caused it to trigger.")
    }
}

private const val LUKKA_EMBLEM_TEXT =
    "Whenever a creature you control enters, it deals damage equal to its power to any target."

private val LukkaWaywardBonderBack = card("Lukka, Wayward Bonder") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "RW"
    typeLine = "Legendary Planeswalker — Lukka"
    startingLoyalty = 5
    oracleText = "+1: You may discard a card. If you do, draw a card. If a creature card was " +
        "discarded this way, draw two cards instead.\n" +
        "−2: Return target creature card from your graveyard to the battlefield. It gains haste. " +
        "Exile it at the beginning of your next upkeep.\n" +
        "−7: You get an emblem with \"$LUKKA_EMBLEM_TEXT\""

    loyaltyAbility(+1) {
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val discarded = chooseUpTo(1, from = hand, prompt = "You may discard a card")
            discard(discarded)
            run(
                Effects.If(
                    condition = whenMatches(discarded, GameObjectFilter.Creature),
                    then = Effects.DrawCards(2),
                    otherwise = Effects.If(
                        condition = whenMatches(discarded),
                        then = Effects.DrawCards(1),
                    ),
                )
            )
        }
        description = "You may discard a card. If you do, draw a card. If a creature card was " +
            "discarded this way, draw two cards instead."
    }

    loyaltyAbility(-2) {
        target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val returned = moveTracked(
                chosen,
                CardDestination.ToZone(Zone.BATTLEFIELD),
                name = "lukkaReturned",
            )
            ifNotEmpty(returned) {
                run(Effects.GrantKeyword(Keyword.HASTE, returned.asTarget, Duration.Permanent))
                run(Effects.CreateDelayedTrigger(
                    step = Step.UPKEEP,
                    fireOnPlayer = EffectTarget.PlayerRef(Player.You),
                    timing = DelayedTriggerTiming.NEXT_TURN,
                    effect = Effects.Pipeline {
                        move(returned, CardDestination.ToZone(Zone.EXILE))
                    },
                    carryCollections = listOf(returned.key),
                ))
            }
        }
        description = "Return target creature card from your graveyard to the battlefield. It " +
            "gains haste. Exile it at the beginning of your next upkeep."
    }

    loyaltyAbility(-7) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
                val anyTarget = target(Targets.Any)
                effect = Effects.DealDamage(
                    amount = DynamicAmounts.triggeringPower(),
                    target = anyTarget,
                    damageSource = EffectTarget.TriggeringEntity
                )
                description = LUKKA_EMBLEM_TEXT
            },
            descriptionOverride = LUKKA_EMBLEM_TEXT
        )
        description = "You get an emblem with \"$LUKKA_EMBLEM_TEXT\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "153"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/back/8/e/8e4e0f81-f92b-4a3a-bb29-adcc3de211b4.jpg?1783927336"
    }
}

val MilaCraftyCompanion: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = MilaCraftyCompanionFront,
    backFace = LukkaWaywardBonderBack,
)
