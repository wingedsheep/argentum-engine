package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Teo, Spirited Glider
 * {3}{U}
 * Legendary Creature — Human Pilot Ally
 * 1/4
 *
 * Flying
 * Whenever one or more creatures you control with flying attack, draw a card, then discard a
 * card. When you discard a nonland card this way, put a +1/+1 counter on target creature you
 * control.
 *
 * The looting + conditional +1/+1 counter is connive-shaped (CR 702.166), but the counter lands on
 * a *chosen target* creature rather than the source. The "When you discard a nonland card this way"
 * clause is a reflexive trigger: the target creature is chosen only after a nonland card is actually
 * discarded, so it's modeled via [Effects.ConniveTargeting] (which selects the recipient at
 * resolution inside the nonland gate) — not an up-front `target(...)`, which would force the choice
 * before the player knows whether they discarded a nonland.
 */
val TeoSpiritedGlider = card("Teo, Spirited Glider") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Pilot Ally"
    power = 1
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever one or more creatures you control with flying attack, draw a card, then " +
        "discard a card. When you discard a nonland card this way, put a +1/+1 counter on " +
        "target creature you control."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YouAttackWithFilter(
            GameObjectFilter.Creature.youControl().withKeyword(Keyword.FLYING)
        )
        effect = Effects.ConniveTargeting(Targets.CreatureYouControl)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Robin Har"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66906ed4-baac-4be0-9359-34f453d1a04a.jpg?1764120477"
    }
}
