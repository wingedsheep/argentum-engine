package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Krosan Vorine
 * {3}{G}
 * Creature — Cat Beast
 * 3/2
 * Provoke (Whenever this creature attacks, you may have target creature defending player
 * controls untap and block it if able.)
 * Krosan Vorine can't be blocked by more than one creature.
 */
val KrosanVorine = card("Krosan Vorine") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Cat Beast"
    power = 3
    toughness = 2
    oracleText = "Provoke (Whenever this creature attacks, you may have target creature defending player controls untap and block it if able.)\nKrosan Vorine can't be blocked by more than one creature."

    keywords(Keyword.PROVOKE)

    triggeredAbility {
        trigger = Triggers.Attacks
        optional = true
        target = Targets.CreatureOpponentControls
        effect = Effects.Provoke(EffectTarget.ContextTarget(0))
    }

    staticAbility {
        ability = CantBeBlockedByMoreThan(maxBlockers = 1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "131"
        artist = "Carl Critchlow"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7d1c6c6-16b3-4a52-aeda-683b1aeb0e7f.jpg?1562931992"
    }
}
