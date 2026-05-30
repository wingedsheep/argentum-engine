package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Unnatural Growth
 * {1}{G}{G}{G}{G}
 * Enchantment
 * At the beginning of each combat, double the power and toughness of each creature you control until end of turn.
 */
val UnnaturalGrowth = card("Unnatural Growth") {
    manaCost = "{1}{G}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "At the beginning of each combat, double the power and toughness of each creature you control until end of turn."

    triggeredAbility {
        trigger = Triggers.EachCombat
        effect = EffectPatterns.doublePowerAndToughnessForAll(Filters.Group.creaturesYouControl)
        description = "At the beginning of each combat, double the power and toughness of each creature you control until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "206"
        artist = "Svetlin Velinov"
        flavorText = "With the cycles of day and night unspooled, the twisted sway of the moon only grew. And grew."
        imageUri = "https://cards.scryfall.io/normal/front/6/7/6748a844-e185-4e3b-ac1d-8a735666d8ae.jpg?1636224994"
        ruling("2021-09-24", "If an effect instructs you to \"double\" a creature's power, that creature gets +X/+0, where X is its power as that effect begins to apply. Similarly, a creature whose toughness is doubled gets +0/+X, where X is its toughness as the effect begins to apply.")
        ruling("2021-09-24", "If a creature's power is less than 0 when it's doubled, instead that creature gets -X/-0, where X is how much less than 0 its power is.")
        ruling("2021-09-24", "If you control more than one Unnatural Growth, each one applies independently.")
    }
}
