package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Arguel's Blood Fast // Temple of Aclazotz (Ixalan — the card's earliest printing)
 * {1}{B}
 * Legendary Enchantment // Legendary Land
 *
 * Front — Arguel's Blood Fast (Legendary Enchantment, {1}{B})
 *   {1}{B}, Pay 2 life: Draw a card.
 *   At the beginning of your upkeep, if you have 5 or less life, you may transform
 *   Arguel's Blood Fast.
 *
 * Back — Temple of Aclazotz (Legendary Land)
 *   {T}: Add {B}.
 *   {T}, Sacrifice a creature: You gain life equal to the sacrificed creature's toughness.
 *
 * Implementation:
 *  - Upkeep trigger uses an intervening-if [Conditions.LifeAtMost]`(5)`, checked both when the
 *    upkeep begins and on resolution (rulings), wrapping an optional [Effects.Transform].
 *  - Temple's life gain reads [DynamicAmounts.sacrificedToughness], the sacrificed creature's
 *    toughness as it last existed on the battlefield (ruling), like Diamond Valley.
 */

private val ArguelsBloodFastFront = card("Arguel's Blood Fast") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Enchantment"
    oracleText = "{1}{B}, Pay 2 life: Draw a card.\n" +
        "At the beginning of your upkeep, if you have 5 or less life, you may transform " +
        "Arguel's Blood Fast."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.PayLife(2))
        effect = Effects.DrawCards(1)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.LifeAtMost(5)
        effect = Effects.May(Effects.Transform(EffectTarget.Self))
        description = "At the beginning of your upkeep, if you have 5 or less life, you may " +
            "transform Arguel's Blood Fast."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Daarken"
        flavorText = "Arguel's vision led him into the jungles of Ixalan . . ."
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4ac7570-e74e-4081-ac53-cf41e695b7eb.jpg?1783935780"
        ruling(
            "2017-09-29",
            "If your life total is 6 or greater as your upkeep begins, the second ability of " +
                "Arguel's Blood Fast won't trigger. You can't take any actions during your turn " +
                "before your upkeep begins.",
        )
        ruling(
            "2017-09-29",
            "If your life total is 6 or greater as the second ability of Arguel's Blood Fast " +
                "tries to resolve, it won't do anything.",
        )
        ruling(
            "2017-09-29",
            "Once you've announced the last ability of Temple of Aclazotz, it's too late for " +
                "anyone to interrupt you by trying to remove the creature you sacrifice.",
        )
        ruling(
            "2017-09-29",
            "For the last ability of Temple of Aclazotz, use the creature's toughness as it last " +
                "existed on the battlefield to determine how much life you gain.",
        )
    }
}

private val TempleOfAclazotz = card("Temple of Aclazotz") {
    manaCost = ""
    colorIdentity = "B"
    typeLine = "Legendary Land"
    oracleText = "(Transforms from Arguel's Blood Fast.)\n" +
        "{T}: Add {B}.\n" +
        "{T}, Sacrifice a creature: You gain life equal to the sacrificed creature's toughness."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK, 1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Sacrifice(GameObjectFilter.Creature))
        effect = Effects.GainLife(DynamicAmounts.sacrificedToughness(), EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Daarken"
        flavorText = ". . . to the lost temple of a bat-god of night, eternal enemy to the " +
            "Threefold Sun."
        imageUri = "https://cards.scryfall.io/normal/back/c/4/c4ac7570-e74e-4081-ac53-cf41e695b7eb.jpg?1783935780"
    }
}

val ArguelsBloodFast: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = ArguelsBloodFastFront,
    backFace = TempleOfAclazotz,
)
