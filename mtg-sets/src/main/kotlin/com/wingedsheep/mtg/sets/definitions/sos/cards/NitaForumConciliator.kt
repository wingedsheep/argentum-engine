package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Nita, Forum Conciliator — Secrets of Strixhaven #206
 * {1}{W}{B} · Legendary Creature — Human Advisor · 2/3
 *
 * Whenever you cast a spell you don't own, put a +1/+1 counter on each creature you control.
 * {2}, Sacrifice another creature: Exile target instant or sorcery card from an opponent's
 *   graveyard. You may cast it this turn, and mana of any type can be spent to cast that spell.
 *   If that spell would be put into a graveyard, exile it instead. Activate only as a sorcery.
 *
 * "A spell you don't own" is gated by the new [SpellCastPredicate.NotOwnedByController] cast-time
 * predicate (the spell's owner — fixed at game start — differs from its controller, i.e. you).
 * It would be true precisely for the card Nita's own activated ability lets you cast.
 *
 * The activated ability targets an instant/sorcery in an opponent's graveyard, moves it to exile,
 * then grants a may-play-from-exile permission with `withAnyManaType = true` (the "you may cast it
 * this turn / mana of any type" clause — you still pay the cost). `exileAfterResolve = true` carries
 * the "if it would be put into a graveyard, exile it instead" rider so the borrowed card never
 * returns to its owner's graveyard.
 */
val NitaForumConciliator = card("Nita, Forum Conciliator") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Advisor"
    power = 2
    toughness = 3
    oracleText = "Whenever you cast a spell you don't own, put a +1/+1 counter on each creature you control.\n" +
        "{2}, Sacrifice another creature: Exile target instant or sorcery card from an opponent's graveyard. " +
        "You may cast it this turn, and mana of any type can be spent to cast that spell. If that spell would " +
        "be put into a graveyard, exile it instead. Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.youCastSpell(requires = setOf(SpellCastPredicate.NotOwnedByController))
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
        )
        description = "Whenever you cast a spell you don't own, put a +1/+1 counter on each creature you control."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature),
        )
        target = TargetObject(
            filter = TargetFilter.InstantOrSorceryInGraveyard.ownedByOpponent(),
        )
        timing = TimingRule.SorcerySpeed
        effect = Effects.Composite(
            // Exile the targeted card from the opponent's graveyard.
            Effects.Move(EffectTarget.ContextTarget(0), Zone.EXILE),
            // Gather it back into a named collection so the may-play grant can key off it.
            GatherCardsEffect(source = CardSource.ChosenTargets, storeAs = "borrowed"),
            // "You may cast it this turn, mana of any type" + "if it would be put into a graveyard,
            // exile it instead."
            Effects.GrantMayPlayFromExile(
                from = "borrowed",
                expiry = MayPlayExpiry.EndOfTurn,
                withAnyManaType = true,
                exileAfterResolve = true,
            ),
        )
        description = "{2}, Sacrifice another creature: Exile target instant or sorcery card from an " +
            "opponent's graveyard. You may cast it this turn, and mana of any type can be spent to cast " +
            "that spell. If that spell would be put into a graveyard, exile it instead. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "206"
        artist = "Jodie Muir"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd80a87d-35d3-4ad1-8172-c85e93032d1d.jpg?1775938431"
    }
}
