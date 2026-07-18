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
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Growing Rites of Itlimoc // Itlimoc, Cradle of the Sun (The Lost Caverns of Ixalan;
 * originally Ixalan)
 * {2}{G}
 * Legendary Enchantment // Legendary Land
 *
 * Front — Growing Rites of Itlimoc (Legendary Enchantment, {2}{G})
 *   When Growing Rites of Itlimoc enters, look at the top four cards of your library. You may
 *   reveal a creature card from among them and put it into your hand. Put the rest on the
 *   bottom of your library in any order.
 *   At the beginning of your end step, if you control four or more creatures, transform
 *   Growing Rites of Itlimoc.
 *
 * Back — Itlimoc, Cradle of the Sun (Legendary Land)
 *   {T}: Add {G}.
 *   {T}: Add {G} for each creature you control.
 *
 * Implementation:
 *  - ETB uses [Patterns.Library.lookAtTopRevealMatchingToHand] (count 4, [GameObjectFilter.Creature],
 *    rest to the bottom in the controller's chosen order, [CardOrder.ControllerChooses]).
 *  - End-step transform is a [Triggers.YourEndStep] ability with an intervening-if
 *    [Conditions.YouControlAtLeast]`(4, Creature)` → [TransformEffect].
 *  - Back's scaling mana ability is [Effects.AddMana]`(GREEN, `[DynamicAmount.AggregateBattlefield]`)`,
 *    the Gaea's Cradle idiom.
 */

private val GrowingRitesOfItlimocFront = card("Growing Rites of Itlimoc") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Enchantment"
    oracleText = "When Growing Rites of Itlimoc enters, look at the top four cards of your " +
        "library. You may reveal a creature card from among them and put it into your hand. " +
        "Put the rest on the bottom of your library in any order.\n" +
        "At the beginning of your end step, if you control four or more creatures, transform " +
        "Growing Rites of Itlimoc."

    // When Growing Rites of Itlimoc enters, look at the top four cards, reveal a creature to
    // hand, rest to the bottom in any order.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = DynamicAmount.Fixed(4),
            filter = GameObjectFilter.Creature,
            prompt = "You may reveal a creature card and put it into your hand",
            restOrder = CardOrder.ControllerChooses,
        )
    }

    // At the beginning of your end step, if you control four or more creatures, transform.
    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.YouControlAtLeast(4, GameObjectFilter.Creature)
        effect = TransformEffect(EffectTarget.Self)
        description = "At the beginning of your end step, if you control four or more " +
            "creatures, transform Growing Rites of Itlimoc."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "188"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/004524bf-b249-4dac-9c10-44d57143feb9.jpg?1782694458"
    }
}

private val ItlimocCradleOfTheSun = card("Itlimoc, Cradle of the Sun") {
    manaCost = ""
    colorIdentity = "G"
    typeLine = "Legendary Land"
    oracleText = "{T}: Add {G}.\n" +
        "{T}: Add {G} for each creature you control."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN, 1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(
            Color.GREEN,
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "188"
        artist = "Josu Hernaiz"
        flavorText = "Though the Sun Empire and the River Heralds once came together to plant " +
            "the sacred forest, its roots have been tended by the mycoids ever since."
        imageUri = "https://cards.scryfall.io/normal/back/0/0/004524bf-b249-4dac-9c10-44d57143feb9.jpg?1782694458"
    }
}

val GrowingRitesOfItlimoc: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = GrowingRitesOfItlimocFront,
    backFace = ItlimocCradleOfTheSun,
)
