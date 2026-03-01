package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AmplifyEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Feral Throwback
 * {4}{G}{G}
 * Creature — Beast
 * 3/3
 * Amplify 2 (As this creature enters, put two +1/+1 counters on it for each
 * Beast card you reveal in your hand.)
 * Provoke (Whenever this creature attacks, you may have target creature defending
 * player controls untap and block it if able.)
 */
val FeralThrowback = card("Feral Throwback") {
    manaCost = "{4}{G}{G}"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 3
    oracleText = "Amplify 2 (As this creature enters, put two +1/+1 counters on it for each Beast card you reveal in your hand.)\nProvoke (Whenever this creature attacks, you may have target creature defending player controls untap and block it if able.)"

    keywords(Keyword.AMPLIFY, Keyword.PROVOKE)

    replacementEffect(AmplifyEffect(countersPerReveal = 2))

    triggeredAbility {
        trigger = Triggers.Attacks
        optional = true
        target = Targets.CreatureOpponentControls
        effect = Effects.Provoke(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "126"
        artist = "Carl Critchlow"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49e0c5e5-b293-419e-aac5-3b81af4b6498.jpg?1562909958"
    }
}
