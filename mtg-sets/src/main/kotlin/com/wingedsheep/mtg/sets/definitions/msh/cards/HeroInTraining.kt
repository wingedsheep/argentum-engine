package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Hero in Training
 * {2}{W}
 * Creature — Human Hero
 * 2/2
 * When this creature enters, draw a card. If you control another Hero, you gain 2 life.
 *
 * Implementation note: one enters trigger whose body is a composite — the unconditional draw
 * followed by a [ConditionalEffect] (lowers to `Gate.WhenCondition`) gated on
 * `Conditions.YouControl(..., excludeSelf = true)`, so the source itself (a Hero) doesn't
 * satisfy its own "another Hero" clause. The condition is checked on resolution, per the
 * card's wording (no intervening-if).
 */
val HeroInTraining = card("Hero in Training") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Hero"
    oracleText = "When this creature enters, draw a card. If you control another Hero, you gain 2 life."
    power = 2
    toughness = 2
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            Effects.DrawCards(1),
            ConditionalEffect(
                condition = Conditions.YouControl(
                    GameObjectFilter.Permanent.withSubtype(Subtype.HERO),
                    excludeSelf = true,
                ),
                effect = Effects.GainLife(2),
            ),
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Taurin Clarke"
        flavorText = "\"Here at Avengers Academy, we'll show you how to control your powers and become the heroes of tomorrow.\"\n—Hank Pym, Avengers Academy founder"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/105a937b-289c-47b5-96a4-654c697bbb7d.jpg?1783902973"
    }
}
