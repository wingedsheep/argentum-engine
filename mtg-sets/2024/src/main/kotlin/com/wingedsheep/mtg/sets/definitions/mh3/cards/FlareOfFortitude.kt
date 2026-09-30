package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Flare of Fortitude {2}{W}{W}
 * Instant
 *
 * You may sacrifice a nontoken white creature rather than pay this spell's mana cost.
 * Until end of turn, your life total can't change, and permanents you control gain hexproof and
 * indestructible.
 *
 * "Your life total can't change" is both player locks (CR 119.7–8): no gain, no loss.
 */
val FlareOfFortitude = card("Flare of Fortitude") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "You may sacrifice a nontoken white creature rather than pay this spell's mana cost.\n" +
        "Until end of turn, your life total can't change, and permanents you control gain hexproof and indestructible."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        additionalCosts = listOf(
            Costs.additional.SacrificePermanent(GameObjectFilter.Creature.withColor(Color.WHITE).nontoken())
        )
    )

    spell {
        effect = Effects.LockLifeGain(EffectTarget.Controller, Duration.EndOfTurn) then
            Effects.LockLifeLoss(EffectTarget.Controller, Duration.EndOfTurn) then
            Patterns.Group.grantKeywordToAll(Keyword.HEXPROOF, Filters.Group.permanentsYouControl) then
            Patterns.Group.grantKeywordToAll(Keyword.INDESTRUCTIBLE, Filters.Group.permanentsYouControl)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Winona Nelson"
        flavorText = "\"I call upon a power greater than myself.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37b41b59-0296-443b-8a62-8d5c4641ef66.jpg?1783911302"
    }
}
