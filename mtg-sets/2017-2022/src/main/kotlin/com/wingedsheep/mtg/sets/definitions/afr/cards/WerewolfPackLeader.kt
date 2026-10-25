package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Werewolf Pack Leader
 * {G}{G}
 * Creature — Human Werewolf
 * 3/3
 * Pack tactics — Whenever this creature attacks, if you attacked with creatures with total power 6
 * or greater this combat, draw a card.
 * {3}{G}: Until end of turn, this creature has base power and toughness 5/3, gains trample, and
 * isn't a Human.
 *
 * The activated ability is a [Effects.BecomeCreature] on an already-creature permanent: layer 7b
 * base P/T 5/3 (counters and pumps still apply on top), trample in layer 6, and a layer-4
 * `removeSubtypes` that drops only Human — it stays a Werewolf.
 */
val WerewolfPackLeader = card("Werewolf Pack Leader") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Werewolf"
    oracleText = "Pack tactics — Whenever this creature attacks, if you attacked with creatures with " +
        "total power 6 or greater this combat, draw a card.\n" +
        "{3}{G}: Until end of turn, this creature has base power and toughness 5/3, gains trample, " +
        "and isn't a Human."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackersTotalPowerAtLeast(6)))
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Mana("{3}{G}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 5,
            toughness = 3,
            keywords = setOf(Keyword.TRAMPLE),
            removeSubtypes = setOf("Human"),
            duration = Duration.EndOfTurn,
        )
        description = "{3}{G}: Until end of turn, this creature has base power and toughness 5/3, " +
            "gains trample, and isn't a Human."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "211"
        artist = "Miranda Meeks"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e88e6b39-bb4d-4d69-8007-d42f31bcbc29.jpg?1783926452"
    }
}
