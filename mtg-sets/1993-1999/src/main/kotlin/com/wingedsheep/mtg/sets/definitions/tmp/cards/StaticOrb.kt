package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.UntapLimitPerStep

/**
 * Static Orb
 * {3}
 * Artifact
 *
 * As long as this artifact is untapped, players can't untap more than two permanents during
 * their untap steps.
 *
 * The Winter Orb shape over every permanent: the cap is read before the simultaneous untap, so a
 * tapped Orb untapping alongside other permanents does not restrict that untap. Two Orbs don't add
 * together — the tighter cap over the same permanents wins.
 */
val StaticOrb = card("Static Orb") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "As long as this artifact is untapped, players can't untap more than two permanents during their untap steps."

    staticAbility {
        condition = Conditions.SourceIsUntapped
        ability = UntapLimitPerStep(GameObjectFilter.Permanent, 2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "310"
        artist = "Dermot Power"
        flavorText = "Time passes, time crawls;\nTime doesn't move at all."
        imageUri = "https://cards.scryfall.io/normal/front/3/5/3574d758-9143-4a2b-9ebd-ed8dab238251.jpg?1783946599"
        ruling("2004-10-04", "If there are two of these on the battlefield, they do not add together. The result is that only two permanents can be untapped.")
    }
}
