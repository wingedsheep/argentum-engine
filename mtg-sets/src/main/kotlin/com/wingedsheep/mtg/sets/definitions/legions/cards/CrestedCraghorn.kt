package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crested Craghorn
 * {4}{R}
 * Creature — Goat Beast
 * 4/1
 * Haste
 * Provoke (Whenever this creature attacks, you may have target creature defending player
 * controls untap and block it if able.)
 */
val CrestedCraghorn = card("Crested Craghorn") {
    manaCost = "{4}{R}"
    typeLine = "Creature — Goat Beast"
    power = 4
    toughness = 1
    oracleText = "Haste\nProvoke (Whenever this creature attacks, you may have target creature defending player controls untap and block it if able.)"

    keywords(Keyword.HASTE, Keyword.PROVOKE)

    triggeredAbility {
        trigger = Triggers.Attacks
        optional = true
        target = Targets.CreatureOpponentControls
        effect = Effects.Provoke(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "91"
        artist = "Matt Cavotta"
        flavorText = "Craghorns experience a wide range of emotions: rage, fury, anger . . .\n—Foothill guide"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aadb40c8-3d54-4705-82dc-54e8d6e315d5.jpg?1562929450"
    }
}
