package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Argent Dais
 * {1}{W}
 * Artifact
 *
 * This artifact enters with two oil counters on it.
 * Whenever two or more creatures attack, put an oil counter on this artifact.
 * {2}, {T}, Remove two oil counters from this artifact: Exile another target nonland permanent.
 * Its controller draws two cards.
 *
 * The attack trigger is any player's declaration (ruling: "not just when you attack with two or
 * more creatures"), so it is `Triggers.anyPlayer.attacks`. "Its controller" resolves through
 * last-known information once the permanent is exiled.
 */
val ArgentDais = card("Argent Dais") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact"
    oracleText = "This artifact enters with two oil counters on it.\n" +
        "Whenever two or more creatures attack, put an oil counter on this artifact.\n" +
        "{2}, {T}, Remove two oil counters from this artifact: Exile another target nonland permanent. " +
        "Its controller draws two cards."

    replacementEffect(EntersWithCounters(counterType = CounterType.OIL, count = 2, selfOnly = true))

    triggeredAbility {
        trigger = Triggers.anyPlayer.attacks(minAttackers = 2)
        effect = Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.RemoveCounterFromSelf(CounterType.OIL, 2))
        val permanent = target(TargetFilter.NonlandPermanent.other())
        effect = Effects.Exile(permanent) then Effects.DrawCards(2, EffectTarget.TargetController)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Carlos Palma Cruchaga"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b56ad225-1249-4e94-898c-ca21b132e62d.jpg?1783911304"
    }
}
