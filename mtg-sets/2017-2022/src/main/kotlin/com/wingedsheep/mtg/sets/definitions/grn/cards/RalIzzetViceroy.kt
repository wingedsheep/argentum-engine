package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Ral, Izzet Viceroy — Guilds of Ravnica #195 (canonical printing)
 * {3}{U}{R} · Legendary Planeswalker — Ral · Loyalty 5
 *
 * +1: Look at the top two cards of your library. Put one of them into your hand and the other into
 *     your graveyard.
 * −3: Ral deals damage to target creature equal to the total number of instant and sorcery cards you
 *     own in exile and in your graveyard.
 * −8: You get an emblem with "Whenever you cast an instant or sorcery spell, this emblem deals 4
 *     damage to any target and you draw two cards."
 *
 * The +1 is Ral's Outburst's look-and-keep; with one card left it goes to hand (ruling). The −3
 * count is Beacon Bolt's: exile zones are keyed by owner, and `faceUp()` drops face-down exiled
 * cards (ruling). The emblem is Chandra, Torch of Defiance's global triggered ability; the draw sits
 * in the same targeted ability, so an illegal target means no draw (ruling).
 */
val RalIzzetViceroy = card("Ral, Izzet Viceroy") {
    manaCost = "{3}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Planeswalker — Ral"
    startingLoyalty = 5
    oracleText = "+1: Look at the top two cards of your library. Put one of them into your hand and " +
        "the other into your graveyard.\n" +
        "−3: Ral deals damage to target creature equal to the total number of instant and sorcery " +
        "cards you own in exile and in your graveyard.\n" +
        "−8: You get an emblem with \"Whenever you cast an instant or sorcery spell, this emblem " +
        "deals 4 damage to any target and you draw two cards.\""

    // +1: Look at the top two cards of your library. One into your hand, the other into your graveyard.
    loyaltyAbility(+1) {
        effect = Patterns.Library.lookAtTopAndKeep(count = 2, keepCount = 1)
    }

    // −3: Damage to target creature equal to instant/sorcery cards you own in exile and graveyard.
    loyaltyAbility(-3) {
        val creature = target(TargetFilter.Creature)
        val spells = GameObjectFilter.InstantOrSorcery.faceUp()
        effect = Effects.DealDamage(
            DynamicAmounts.count(Player.You, Zone.EXILE, spells) +
                DynamicAmounts.count(Player.You, Zone.GRAVEYARD, spells),
            creature
        )
    }

    // −8: Emblem.
    val emblemText = "Whenever you cast an instant or sorcery spell, this emblem deals 4 damage to " +
        "any target and you draw two cards."
    loyaltyAbility(-8) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
                val anyTarget = target(Targets.Any)
                effect = Effects.DealDamage(4, anyTarget) then Effects.DrawCards(2)
                description = emblemText
            },
            emblem = true,
            descriptionOverride = emblemText
        )
        description = "You get an emblem with \"$emblemText\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "195"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c52066b3-84dc-4198-8090-3336e7df8b5d.jpg?1783934125"
        ruling(
            "2018-10-05",
            "If you have only one card in your library, Ral's first ability puts it into your hand. " +
                "Nothing is put into your graveyard."
        )
        ruling(
            "2018-10-05",
            "If any exiled cards you own are face down, they have no characteristics. If they're normally " +
                "instants or sorceries, they won't be counted for Ral's second ability."
        )
        ruling(
            "2018-10-05",
            "The emblem created by Ral's last ability is colorless. The damage it deals is from a colorless source."
        )
        ruling(
            "2018-10-05",
            "The triggered ability of Ral's emblem resolves before the spell that caused it to trigger. " +
                "It resolves even if that spell is countered."
        )
        ruling(
            "2018-10-05",
            "If the target permanent or player becomes an illegal target for the triggered ability of " +
                "Ral's emblem, the ability doesn't resolve. You don't draw two cards."
        )
    }
}
