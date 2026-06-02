package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.GroupPatterns
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Gandalf, White Rider
 * {3}{W}
 * Legendary Creature — Avatar Wizard
 * 3/3
 *
 * Vigilance
 * Whenever you cast a spell, creatures you control get +1/+0 until end of turn. Scry 1.
 * When Gandalf dies, you may put it into its owner's library fifth from the top.
 */
val GandalfWhiteRider = card("Gandalf, White Rider") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Avatar Wizard"
    power = 3
    toughness = 3
    oracleText = "Vigilance\n" +
        "Whenever you cast a spell, creatures you control get +1/+0 until end of turn. Scry 1.\n" +
        "When Gandalf dies, you may put it into its owner's library fifth from the top."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.YouCastSpell
        effect = GroupPatterns.modifyStatsForAll(1, 0, Filters.Group.creaturesYouControl)
            .then(LibraryPatterns.scry(1))
    }

    triggeredAbility {
        trigger = Triggers.Dies
        optional = true
        effect = Effects.PutIntoLibraryNthFromTop(EffectTarget.Self, positionFromTop = 4)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "290"
        artist = "Ekaterina Burmak"
        flavorText = "He has passed through the fire and the abyss, and the enemy shall fear him."
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b422ce26-06fb-4748-9c01-32c4be914a77.jpg?1687424784"
    }
}
