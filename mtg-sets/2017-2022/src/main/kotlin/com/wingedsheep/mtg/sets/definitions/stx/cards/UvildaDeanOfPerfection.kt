package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Uvilda, Dean of Perfection // Nassari, Dean of Expression — Strixhaven modal double-faced card.
 *
 * Uvilda's exiled card *gains* two triggered abilities, printed in quotes. They are granted to the
 * card in exile (`Effects.GrantTriggeredAbility` onto an exiled card, `Duration.Permanent`) and end
 * when it leaves exile, so a card cast, bounced or otherwise moved keeps nothing:
 *  - the upkeep countdown functions from exile (`triggerZone = EXILE`) and is gated by its printed
 *    "if this card is exiled";
 *  - the last-counter trigger is a separate ability, so a refine counter removed any other way
 *    still lets the card be cast, and it goes on the stack on its own. Its cast pays the mana cost
 *    with `costReduction = 4` — a generic-only reduction applied after any increase (rulings).
 *
 * Nassari's upkeep trigger is Stolen Strategy's; the counter trigger fires on any spell cast from
 * exile, not only the cards Nassari exiled (ruling).
 */
private val UvildaDeanOfPerfectionFront = card("Uvilda, Dean of Perfection") {
    manaCost = "{2}{U}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Djinn Wizard"
    power = 2
    toughness = 2
    oracleText = "{T}: You may exile an instant or sorcery card from your hand and put three refine counters " +
        "on it. It gains \"At the beginning of your upkeep, if this card is exiled, remove a refine counter " +
        "from it\" and \"When the last refine counter is removed from this card, if it's exiled, you may cast " +
        "it. It costs {4} less to cast this way.\""

    // The two abilities the exiled card gains. Built inside card { } so they get this card's ability ids.
    val refineCountdown = grantedTriggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        triggerZone = Zone.EXILE
        interveningIf = Conditions.SourceInZone(Zone.EXILE)
        effect = Effects.RemoveCounters(CounterType.REFINE, 1, EffectTarget.Self)
        description = "At the beginning of your upkeep, if this card is exiled, remove a refine counter from it."
    }

    val castWhenRefined = grantedTriggeredAbility {
        trigger = Triggers.self.losesCounters(CounterType.REFINE, lastRemoved = true)
        triggerZone = Zone.EXILE
        interveningIf = Conditions.SourceInZone(Zone.EXILE)
        effect = Effects.May(
            Effects.Pipeline {
                val self = gather(CardSource.Entity(EffectTarget.Self))
                run(Effects.CastFromCollection(self, costReduction = 4))
            },
            prompt = "Cast this card for {4} less?",
        )
        description = "When the last refine counter is removed from this card, if it's exiled, you may cast it. " +
            "It costs {4} less to cast this way."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.InstantOrSorcery))
            val chosen = chooseUpTo(
                1,
                from = hand,
                prompt = "You may exile an instant or sorcery card from your hand",
                selectedLabel = "Exile",
            )
            exile(chosen)
            run(
                Effects.ForEachInCollection(
                    chosen,
                    Effects.AddCounters(CounterType.REFINE, 3, EffectTarget.IterationEntity) then
                        Effects.GrantTriggeredAbility(refineCountdown, EffectTarget.IterationEntity, Duration.Permanent) then
                        Effects.GrantTriggeredAbility(castWhenRefined, EffectTarget.IterationEntity, Duration.Permanent),
                )
            )
        }
        description = "{T}: You may exile an instant or sorcery card from your hand and put three refine " +
            "counters on it. It gains \"At the beginning of your upkeep, if this card is exiled, remove a refine " +
            "counter from it\" and \"When the last refine counter is removed from this card, if it's exiled, " +
            "you may cast it. It costs {4} less to cast this way.\""
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "160"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8cfd0887-0c83-4b33-a85e-8b8ec5bf758d.jpg?1785596047"
        ruling(
            "2021-04-16",
            "After removing the last hone counter, casting the spell is optional. Timing permissions based on " +
                "the card's type are ignored. If an effect prohibits you from casting the card or you choose not " +
                "to cast it, it remains exiled with no hone counters on it, and you won't be able to cast it later.",
        )
        ruling(
            "2021-04-16",
            "The cost reduction given by Uvilda, Dean of Perfection won't reduce any colored mana requirements " +
                "in the spell's cost.",
        )
        ruling(
            "2021-04-16",
            "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, " +
                "add any cost increases, then apply any cost reductions (such as that of Uvilda, Dean of " +
                "Perfection). The mana value of the spell is determined only by its mana cost, no matter what " +
                "the total cost to cast the spell was.",
        )
    }
}

private val NassariDeanOfExpressionBack = card("Nassari, Dean of Expression") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Efreet Shaman"
    power = 4
    toughness = 4
    oracleText = "At the beginning of your upkeep, exile the top card of each opponent's library. Until end " +
        "of turn, you may cast spells from among those exiled cards, and you may spend mana as though it were " +
        "mana of any color to cast those spells.\n" +
        "Whenever you cast a spell from exile, put a +1/+1 counter on Nassari."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val stolen = gather(CardSource.TopOfLibrary(count = 1, player = Player.EachOpponent))
            exile(stolen)
            run(
                Effects.GrantMayPlayFromExile(
                    from = stolen,
                    expiry = MayPlayExpiry.EndOfTurn,
                    withAnyManaColor = true,
                    nonLandOnly = true,
                )
            )
        }
    }

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.CastFromZone(Zone.EXILE)))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "160"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/back/8/c/8cfd0887-0c83-4b33-a85e-8b8ec5bf758d.jpg?1785596047"
        ruling(
            "2021-04-16",
            "You exile cards with Nassari, Dean of Expression face up. Casting those spells is subject to the " +
                "normal timing restrictions for spells.",
        )
        ruling(
            "2021-04-16",
            "Any cards exiled with Nassari's ability that you don't cast will remain exiled. You won't be able " +
                "to cast those cards on future turns, although Nassari's ability will give you another group of " +
                "cards to choose from each time it resolves.",
        )
        ruling(
            "2021-04-16",
            "Nassari's last ability triggers when you cast any spell from exile, not just the cards that were " +
                "exiled with its first ability.",
        )
    }
}

val UvildaDeanOfPerfection: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = UvildaDeanOfPerfectionFront,
    backFace = NassariDeanOfExpressionBack,
)
