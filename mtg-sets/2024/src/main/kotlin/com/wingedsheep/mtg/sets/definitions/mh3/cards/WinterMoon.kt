package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.UntapLimitPerStep

/**
 * Winter Moon — Modern Horizons 3 #213
 * {2} · Artifact
 *
 * Players can't untap more than one nonbasic land during their untap steps.
 *
 * The Damping Field shape: a global, per-player [UntapLimitPerStep] cap over nonbasic lands.
 * Basic lands are unaffected, and other untap restrictions stack with it (per the rulings).
 */
val WinterMoon = card("Winter Moon") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Players can't untap more than one nonbasic land during their untap steps."

    staticAbility {
        ability = UntapLimitPerStep(GameObjectFilter.NonbasicLand, 1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "213"
        artist = "Drew Baker"
        flavorText = "\"The sun and moon have decided to make a competition of our misery.\"\n" +
            "—Naromin, veteran explorer"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f76bc2da-8f4b-4153-8a7b-c601b19affaf.jpg?1783911242"
        ruling(
            "2024-06-07",
            "If multiple Winter Moons are on the battlefield, their effects are redundant. Each player will " +
                "still be able to untap no more than one nonbasic land during their untap step."
        )
        ruling(
            "2024-06-07",
            "If Winter Moon is on the battlefield while another effect restricts untapping lands or permanents " +
                "in a different way, their effects are cumulative. For example, if you control both Winter Moon " +
                "and Static Orb, each player will be able to untap no more than two permanents, up to one of " +
                "which can be a nonbasic land, during their untap step."
        )
    }
}
