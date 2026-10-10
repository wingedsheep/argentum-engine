package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kenrith, the Returned King — Throne of Eldraine #303
 * {4}{W} · Legendary Creature — Human Noble · 5/5
 *
 * {R}: All creatures gain trample and haste until end of turn.
 * {1}{G}: Put a +1/+1 counter on target creature.
 * {2}{W}: Target player gains 5 life.
 * {3}{U}: Target player draws a card.
 * {4}{B}: Put target creature card from a graveyard onto the battlefield under its owner's control.
 *
 * The {R} grant iterates the creatures on the battlefield at resolution, so later arrivals don't
 * gain the keywords (ruling 2019-10-04). The {B} reanimation leaves `Move`'s controller at its
 * default — the card's owner — and `fromZone = GRAVEYARD` makes it a no-op if the card has left.
 */
val KenrithTheReturnedKing = card("Kenrith, the Returned King") {
    manaCost = "{4}{W}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Human Noble"
    power = 5
    toughness = 5
    oracleText = "{R}: All creatures gain trample and haste until end of turn.\n" +
        "{1}{G}: Put a +1/+1 counter on target creature.\n" +
        "{2}{W}: Target player gains 5 life.\n" +
        "{3}{U}: Target player draws a card.\n" +
        "{4}{B}: Put target creature card from a graveyard onto the battlefield under its owner's control."

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreatures,
            effect = Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity)
        )
        description = "All creatures gain trample and haste until end of turn."
    }

    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
        description = "Put a +1/+1 counter on target creature."
    }

    activatedAbility {
        cost = Costs.Mana("{2}{W}")
        val player = target(Targets.Player)
        effect = Effects.GainLife(5, player)
        description = "Target player gains 5 life."
    }

    activatedAbility {
        cost = Costs.Mana("{3}{U}")
        val player = target(Targets.Player)
        effect = Effects.DrawCards(1, player)
        description = "Target player draws a card."
    }

    activatedAbility {
        cost = Costs.Mana("{4}{B}")
        val creature = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.Move(creature, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
        description = "Put target creature card from a graveyard onto the battlefield under its owner's control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "303"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56c1227e-bea7-47cb-bbec-389a3d585af5.jpg?1783932556"
        ruling(
            "2019-10-04",
            "Kenrith's first ability affects only creatures on the battlefield at the time it resolves. " +
                "Creatures that enter the battlefield later in the turn won't gain trample or haste."
        )
        ruling(
            "2023-04-14",
            "Kenrith's last ability can target a creature card in any player's graveyard. Its owner will " +
                "control the creature, and it will remain on the battlefield even if you leave the game " +
                "(if you don't own it)."
        )
    }
}
