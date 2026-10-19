package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tameshi, Reality Architect — Kamigawa: Neon Dynasty #82
 * {2}{U} · Legendary Creature — Moonfolk Wizard · 2/3
 *
 * Whenever one or more noncreature permanents are returned to hand, draw a card. This ability
 * triggers only once each turn.
 * {X}{W}, Return a land you control to its owner's hand: Return target artifact or enchantment card
 * with mana value X or less from your graveyard to the battlefield. Activate only as a sorcery.
 *
 * - The trigger watches any controller's noncreature permanents leaving the battlefield for a hand
 *   (the ruling: other zones → hand don't count). It is per-object, but `oncePerTurn` collapses a
 *   simultaneous batch to one trigger and caps it for the turn — exactly "one or more … only once
 *   each turn". The noncreature check reads last-known types, so an animated land doesn't count.
 * - The returned land is itself a noncreature permanent, so paying the cost fires the trigger.
 * - X is the announced {X} of the activation, bound into the target's "mana value X or less".
 */
val TameshiRealityArchitect = card("Tameshi, Reality Architect") {
    manaCost = "{2}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Moonfolk Wizard"
    power = 2
    toughness = 3
    oracleText = "Whenever one or more noncreature permanents are returned to hand, draw a card. " +
        "This ability triggers only once each turn.\n" +
        "{X}{W}, Return a land you control to its owner's hand: Return target artifact or enchantment " +
        "card with mana value X or less from your graveyard to the battlefield. Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.NoncreaturePermanent).leaves(to = Zone.HAND)
        oncePerTurn = true
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{X}{W}"),
            Costs.ReturnToHand(GameObjectFilter.Land),
        )
        val card = target(
            TargetFilter(
                baseFilter = GameObjectFilter.ArtifactOrEnchantment.ownedByYou().manaValueAtMostX(),
                zone = Zone.GRAVEYARD,
            ),
        )
        effect = Effects.PutOntoBattlefieldFromGraveyard(card)
        timing = TimingRule.SorcerySpeed
        description = "{X}{W}, Return a land you control to its owner's hand: Return target artifact or " +
            "enchantment card with mana value X or less from your graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "82"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26594b52-3e9c-4cde-88df-1f4e9e16676e.jpg?1783923894"
        ruling(
            "2022-02-18",
            "Tameshi's first ability triggers only whenever one or more noncreature permanents are returned " +
                "to hand from the battlefield. Noncreature permanent cards returned to hand from other zones " +
                "do not cause this ability to trigger."
        )
    }
}
