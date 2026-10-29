package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sublime Epiphany
 * {4}{U}{U}
 * Instant
 * Choose one or more —
 * • Counter target spell.
 * • Counter target activated or triggered ability.
 * • Return target nonland permanent to its owner's hand.
 * • Create a token that's a copy of target creature you control.
 * • Target player draws a card.
 *
 * A five-mode "one or more" modal (min 1, max 5, no escalate cost); modes resolve in printed
 * order, each against its own targets — Cryptic Command's counter + bounce, Defabricate's Stifle
 * mode, Electroduplicate's token copy, and a targeted draw.
 */
val SublimeEpiphany = card("Sublime Epiphany") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one or more —\n" +
        "• Counter target spell.\n" +
        "• Counter target activated or triggered ability.\n" +
        "• Return target nonland permanent to its owner's hand.\n" +
        "• Create a token that's a copy of target creature you control.\n" +
        "• Target player draws a card."

    spell {
        modal(chooseCount = 5, minChooseCount = 1) {
            mode("Counter target spell") {
                target(TargetFilter.SpellOnStack)
                effect = Effects.CounterSpell()
            }
            mode("Counter target activated or triggered ability") {
                target(TargetFilter.ActivatedOrTriggeredAbilityOnStack)
                effect = Effects.CounterAbility()
            }
            mode("Return target nonland permanent to its owner's hand") {
                val permanent = target(TargetFilter.NonlandPermanent)
                effect = Effects.ReturnToHand(permanent)
            }
            mode("Create a token that's a copy of target creature you control") {
                val creature = target(TargetFilter.CreatureYouControl)
                effect = Effects.CreateTokenCopyOfTarget(creature)
            }
            mode("Target player draws a card") {
                val player = target(Targets.Player)
                effect = Effects.DrawCards(1, player)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "74"
        artist = "Lindsey Look"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad1bcb44-a562-4f66-b862-6d0ef3546ab4.jpg?1783930718"
        ruling(
            "2020-06-23",
            "Sublime Epiphany's second mode can't counter an activated or triggered mana ability."
        )
        ruling(
            "2020-06-23",
            "The token copies exactly what is printed on the creature and nothing else (unless that " +
                "creature is a token or is copying something else). It doesn't copy whether that creature " +
                "is tapped or untapped, whether it has any counters on it or Auras and/or Equipment attached " +
                "to it, or any non-copy effects that changed its power, toughness, types, color, and so on."
        )
        ruling("2020-06-23", "If any of the targets become illegal, the remaining targets are affected as appropriate.")
        ruling("2020-06-23", "You can't choose any one mode more than once.")
        ruling(
            "2020-06-23",
            "If you choose more than one mode, you perform the modes in the order specified. For example, " +
                "if you choose to counter target spell, return target nonland permanent to its owner's hand, " +
                "and have target player draw a card, that permanent will be on the battlefield while that " +
                "spell is countered (and may modify, affect, or trigger on that event) but will not be on " +
                "the battlefield while that player draws a card."
        )
        ruling(
            "2020-06-23",
            "If you choose the third and fourth modes and target the same creature you control both " +
                "times, you'll return it and then create a token that's a copy of it, using its copiable " +
                "values as it last appeared on the battlefield. This is because the legality of targets is " +
                "checked only as the spell begins to resolve, before any actions are performed."
        )
    }
}
