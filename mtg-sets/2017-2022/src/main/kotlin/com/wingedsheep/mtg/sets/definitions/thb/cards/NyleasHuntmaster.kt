package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Nylea's Huntmaster
 * {3}{G}
 * Creature — Centaur Shaman
 * 4/3
 * When this creature enters, target creature you control gets +X/+0 until end of turn, where X is
 * your devotion to green.
 *
 * X is [DynamicAmounts.devotionTo] read once as the trigger resolves and locked into the
 * until-end-of-turn bonus (first ruling); the Huntmaster's own {G} counts if it's still there.
 */
val NyleasHuntmaster = card("Nylea's Huntmaster") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Centaur Shaman"
    power = 4
    toughness = 3
    oracleText = "When this creature enters, target creature you control gets +X/+0 until end of turn, " +
        "where X is your devotion to green. (Each {G} in the mana costs of permanents you control counts " +
        "toward your devotion to green.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.ModifyStats(DynamicAmounts.devotionTo(Color.GREEN), DynamicAmounts.fixed(0), creature)
        description = "When this creature enters, target creature you control gets +X/+0 until end of " +
            "turn, where X is your devotion to green."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "187"
        artist = "Winona Nelson"
        flavorText = "Nature is stillness and savagery in equal measure."
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a4fe89a-0d2b-493f-883e-a1e0b0918340.jpg?1783931533"
        ruling("2020-01-24", "The value of X is determined only as the triggered ability resolves. Once that happens, the value of X won't change later in the turn even if your devotion to green changes.")
        ruling("2020-01-24", "If an activated ability or triggered ability has an effect that depends on your devotion to a color, you count the number of mana symbols of that color among the mana costs of permanents you control as the ability resolves. The permanent with that ability will be counted if it's still on the battlefield at that time.")
    }
}
