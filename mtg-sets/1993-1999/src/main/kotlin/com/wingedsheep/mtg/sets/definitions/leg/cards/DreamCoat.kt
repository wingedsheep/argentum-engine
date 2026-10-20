package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Dream Coat
 * {U}
 * Enchantment — Aura
 *
 * Enchant creature
 * {0}: Enchanted creature becomes the color or colors of your choice. Activate only once each turn.
 *
 * Per the rulings the recolor does not wear off at end of turn, so the layer-5 change is
 * [Duration.Permanent]; it stays on the creature even if Dream Coat later leaves.
 */
val DreamCoat = card("Dream Coat") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n{0}: Enchanted creature becomes the color or colors of your choice. " +
        "Activate only once each turn."
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    activatedAbility {
        cost = Costs.Mana("{0}")
        effect = Effects.ChooseColorsThen(
            Effects.ChangeColorToChosen(EffectTarget.EnchantedCreature, Duration.Permanent)
        )
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        description = "{0}: Enchanted creature becomes the color or colors of your choice. Activate only once each turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "Anthony S. Waters"
        flavorText = "\"Adopt the character of the twisting octopus, which takes on the appearance of the " +
            "nearby rock. Now follow in this direction, now turn a different hue.\"\n—Theognis, *Elegies 1, 215*"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07edbbf4-c3d6-4ec1-ae9b-4ae202fb6998.jpg?1783948077"
        ruling("2004-10-04", "You can choose any single color or combination of colors. The valid colors are Black, Blue, Green, Red, and White.")
        ruling("2004-10-04", "The color change does not wear off at end of turn.")
    }
}
