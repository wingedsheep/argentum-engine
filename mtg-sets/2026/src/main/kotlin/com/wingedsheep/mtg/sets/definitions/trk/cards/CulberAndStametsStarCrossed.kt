package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Culber and Stamets, Star-Crossed
 * {3}{G}
 * Legendary Creature — Human Doctor Artificer
 * 3/4
 * When Culber and Stamets enter, choose one —
 * • Put a +1/+1 counter on target creature.
 * • Add two mana of any one color.
 */
val CulberAndStametsStarCrossed = card("Culber and Stamets, Star-Crossed") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Doctor Artificer"
    power = 3
    toughness = 4
    oracleText = "When Culber and Stamets enter, choose one —\n" +
        "• Put a +1/+1 counter on target creature.\n" +
        "• Add two mana of any one color."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Put a +1/+1 counter on target creature.") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
            },
            Mode(effect = Effects.AddAnyColorMana(2), description = "Add two mana of any one color."),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "191"
        artist = "Harkalé Linaï"
        flavorText = "\"One tends to worry when they're doomed to love a brilliant but reckless maniac " +
            "who's willing to risk his life for glory.\"\n—Dr. Hugh Culber"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/962752f7-5c94-41e4-ab7d-3e8741c7a502.jpg?1793290364"
    }
}
