package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Voyager Glidecar
 * {W}
 * Artifact — Vehicle
 * 2/3
 * When this Vehicle enters, scry 1.
 * Tap three other untapped creatures you control: Until end of turn, this Vehicle becomes an
 * artifact creature and gains flying. Put a +1/+1 counter on it.
 * Crew 1
 *
 * The activated ability is a pure tap-cost animate — `excludeSelf` gives the "other" in "three
 * other untapped creatures you control" (the Glidecar is only a creature while animated, but the
 * word is printed, so it's modelled explicitly). Only the animate is until-end-of-turn; the
 * +1/+1 counter is permanent.
 */
val VoyagerGlidecar = card("Voyager Glidecar") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Vehicle"
    oracleText = "When this Vehicle enters, scry 1.\n" +
        "Tap three other untapped creatures you control: Until end of turn, this Vehicle becomes " +
        "an artifact creature and gains flying. Put a +1/+1 counter on it.\n" +
        "Crew 1 (Tap any number of creatures you control with total power 1 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Scry(1)
    }

    activatedAbility {
        cost = Costs.TapPermanents(
            count = 3,
            filter = GameObjectFilter.Creature,
            excludeSelf = true
        )
        effect = Effects.Composite(
            Effects.BecomeCreature(
                target = EffectTarget.Self,
                power = 2,
                toughness = 3,
                keywords = setOf(Keyword.FLYING),
                duration = Duration.EndOfTurn
            ),
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        )
        description = "Tap three other untapped creatures you control: Until end of turn, this " +
            "Vehicle becomes an artifact creature and gains flying. Put a +1/+1 counter on it."
    }

    keywordAbility(KeywordAbility.crew(1))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Eduardo Francisco"
        flavorText = "\"Exclamatory: Hang on to something.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13eb445a-dd41-4760-8299-9ba5d6de6aaf.jpg?1783907912"
    }
}
