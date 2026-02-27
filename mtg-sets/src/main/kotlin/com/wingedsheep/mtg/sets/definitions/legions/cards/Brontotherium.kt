package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brontotherium
 * {4}{G}{G}
 * Creature — Beast
 * 5/3
 * Trample
 * Provoke (Whenever this creature attacks, you may have target creature defending player
 * controls untap and block it if able.)
 */
val Brontotherium = card("Brontotherium") {
    manaCost = "{4}{G}{G}"
    typeLine = "Creature — Beast"
    power = 5
    toughness = 3
    oracleText = "Trample\nProvoke (Whenever this creature attacks, you may have target creature defending player controls untap and block it if able.)"

    keywords(Keyword.TRAMPLE, Keyword.PROVOKE)

    triggeredAbility {
        trigger = Triggers.Attacks
        optional = true
        target = Targets.CreatureOpponentControls
        effect = Effects.Provoke(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "Carl Critchlow"
        flavorText = "Lucky victims get run over. Unlucky victims get run through."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a171f5e2-ed3d-4675-a4fc-953ebb907aa0.jpg?1562927638"
    }
}
