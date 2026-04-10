package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithRevealCounters
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ghastly Remains
 * {B}{B}{B}
 * Creature — Zombie
 * 0/0
 * Amplify 1 (As this creature enters, put a +1/+1 counter on it for each
 * Zombie card you reveal in your hand.)
 * At the beginning of your upkeep, if Ghastly Remains is in your graveyard,
 * you may pay {B}{B}{B}. If you do, return Ghastly Remains to your hand.
 */
val GhastlyRemains = card("Ghastly Remains") {
    manaCost = "{B}{B}{B}"
    typeLine = "Creature — Zombie"
    power = 0
    toughness = 0
    oracleText = "Amplify 1 (As this creature enters, put a +1/+1 counter on it for each Zombie card you reveal in your hand.)\nAt the beginning of your upkeep, if Ghastly Remains is in your graveyard, you may pay {B}{B}{B}. If you do, return Ghastly Remains to your hand."

    keywords(Keyword.AMPLIFY)
    replacementEffect(EntersWithRevealCounters(countersPerReveal = 1))

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        triggerZone = Zone.GRAVEYARD
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{B}{B}{B}"),
            effect = MoveToZoneEffect(EffectTarget.Self, Zone.HAND)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "Edward P. Beard, Jr."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63e67323-df54-4043-a6b6-18bb89ef1f62.jpg?1562915232"
    }
}
