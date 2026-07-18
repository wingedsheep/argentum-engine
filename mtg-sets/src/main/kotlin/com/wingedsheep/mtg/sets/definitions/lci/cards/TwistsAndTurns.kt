package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyExplore
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Twists and Turns // Mycoid Maze (The Lost Caverns of Ixalan)
 * {G}
 * Enchantment // Land — Cave
 *
 * Front — Twists and Turns (Enchantment, {G})
 *   If a creature you control would explore, instead you scry 1, then that creature explores.
 *   When this enchantment enters, target creature you control explores.
 *   When a land you control enters, if you control seven or more lands, transform this enchantment.
 *
 * Back — Mycoid Maze (Land — Cave)
 *   {T}: Add {G}.
 *   {3}{G}, {T}: Look at the top four cards of your library. You may reveal a creature card from
 *   among them and put that card into your hand. Put the rest on the bottom of your library in a
 *   random order.
 *
 * Implementation:
 *  - The explore-modifying clause is a [ModifyExplore] replacement (CR 614): "if a creature you
 *    control would explore, first [Effects.Scry] 1, then it explores." `ExploreEffectExecutor`
 *    consults it and re-issues the explore as a Composite so the scry's top/bottom decision
 *    resolves before the explore.
 *  - ETB is [Triggers.EntersBattlefield] → [Effects.Explore] on a target creature you control; the
 *    replacement applies to that explore too (scry 1 first).
 *  - The transform is [Triggers.LandYouControlEnters] with intervening-if
 *    [Conditions.YouControlAtLeast]`(7, Land)` → [TransformEffect].
 *  - Back's activated ability is [Patterns.Library.lookAtTopRevealMatchingToHand] (count 4,
 *    creature, rest to the bottom in random order).
 */

private val TwistsAndTurnsFront = card("Twists and Turns") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "If a creature you control would explore, instead you scry 1, then that " +
        "creature explores.\n" +
        "When Twists and Turns enters, target creature you control explores.\n" +
        "When a land you control enters, if you control seven or more lands, transform Twists " +
        "and Turns."

    // If a creature you control would explore, instead you scry 1, then that creature explores.
    replacementEffect(
        ModifyExplore(
            prefixEffect = Effects.Scry(1),
            appliesTo = EventPattern.ExploredEvent(filter = GameObjectFilter.Creature.youControl()),
        )
    )

    // When Twists and Turns enters, target creature you control explores.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "target creature you control",
            TargetCreature(filter = TargetFilter.Creature.youControl()),
        )
        effect = Effects.Explore(creature)
    }

    // When a land you control enters, if you control seven or more lands, transform.
    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        triggerCondition = Conditions.YouControlAtLeast(7, GameObjectFilter.Land)
        effect = TransformEffect(EffectTarget.Self)
        description = "When a land you control enters, if you control seven or more lands, " +
            "transform Twists and Turns."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3cdf691e-96a5-45c7-9b94-6f04af81c8e4.jpg?1782694436"
    }
}

private val MycoidMaze = card("Mycoid Maze") {
    manaCost = ""
    colorIdentity = "G"
    typeLine = "Land — Cave"
    oracleText = "{T}: Add {G}.\n" +
        "{3}{G}, {T}: Look at the top four cards of your library. You may reveal a creature card " +
        "from among them and put that card into your hand. Put the rest on the bottom of your " +
        "library in a random order."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN, 1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{G}"), Costs.Tap)
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = DynamicAmount.Fixed(4),
            filter = GameObjectFilter.Creature,
            prompt = "You may reveal a creature card and put it into your hand",
            restOrder = CardOrder.Random,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/back/3/c/3cdf691e-96a5-45c7-9b94-6f04af81c8e4.jpg?1782694436"
    }
}

val TwistsAndTurns: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = TwistsAndTurnsFront,
    backFace = MycoidMaze,
)
