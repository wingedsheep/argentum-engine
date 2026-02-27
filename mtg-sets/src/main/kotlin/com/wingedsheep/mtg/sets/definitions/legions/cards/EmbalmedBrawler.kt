package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AmplifyEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Embalmed Brawler
 * {2}{B}
 * Creature — Zombie
 * 2/2
 * Amplify 1 (As this creature enters, put a +1/+1 counter on it for each
 * Zombie card you reveal in your hand.)
 * Whenever Embalmed Brawler attacks or blocks, you lose 1 life for each
 * +1/+1 counter on it.
 */
val EmbalmedBrawler = card("Embalmed Brawler") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 2
    oracleText = "Amplify 1 (As this creature enters, put a +1/+1 counter on it for each Zombie card you reveal in your hand.)\nWhenever Embalmed Brawler attacks or blocks, you lose 1 life for each +1/+1 counter on it."

    keywords(Keyword.AMPLIFY)

    replacementEffect(AmplifyEffect(countersPerReveal = 1))

    // "Whenever ~ attacks or blocks" needs two triggered abilities
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.LoseLife(
            amount = DynamicAmount.CountersOnSelf(CounterTypeFilter.PlusOnePlusOne),
            target = EffectTarget.Controller
        )
    }

    triggeredAbility {
        trigger = Triggers.Blocks
        effect = Effects.LoseLife(
            amount = DynamicAmount.CountersOnSelf(CounterTypeFilter.PlusOnePlusOne),
            target = EffectTarget.Controller
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "69"
        artist = "Justin Sweet"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e214da0-68c0-4cf6-ba12-e2b2394909c1.jpg?1562904398"
    }
}
