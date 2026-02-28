package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.TurnFaceDownEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wall of Deceit
 * {1}{U}
 * Creature — Wall
 * 0/5
 * Defender
 * {3}: Turn Wall of Deceit face down.
 * Morph {U}
 */
val WallOfDeceit = card("Wall of Deceit") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Wall"
    power = 0
    toughness = 5
    oracleText = "Defender\n{3}: Turn Wall of Deceit face down.\nMorph {U}"

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = TurnFaceDownEffect(target = EffectTarget.Self)
    }

    morph = "{U}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/1496d941-88fd-433e-8fae-1218316ef3a9.jpg?1562899105"
        ruling("2004-10-04", "As of the Champions of Kamigawa rules update, the Wall creature type no longer inherently prevents attacking. All Walls printed before this update received errata granting the defender keyword.")
    }
}
