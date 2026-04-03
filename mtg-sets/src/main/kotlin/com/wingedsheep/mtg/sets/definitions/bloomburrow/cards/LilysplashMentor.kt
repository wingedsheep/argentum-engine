package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lilysplash Mentor {2}{G}{U}
 * Creature — Frog Druid
 * 4/4
 *
 * Reach
 * {1}{G}{U}: Exile another target creature you control, then return it to the
 * battlefield under its owner's control with a +1/+1 counter on it.
 * Activate only as a sorcery.
 */
val LilysplashMentor = card("Lilysplash Mentor") {
    manaCost = "{2}{G}{U}"
    typeLine = "Creature — Frog Druid"
    oracleText = "Reach\n{1}{G}{U}: Exile another target creature you control, then return it to the battlefield under its owner's control with a +1/+1 counter on it. Activate only as a sorcery."
    power = 4
    toughness = 4

    keywords(Keyword.REACH)

    activatedAbility {
        cost = Costs.Mana("{1}{G}{U}")
        timing = TimingRule.SorcerySpeed
        val creature = target("another creature you control", TargetCreature(filter = TargetFilter.OtherCreatureYouControl))
        effect = MoveToZoneEffect(creature, Zone.EXILE)
            .then(MoveToZoneEffect(creature, Zone.BATTLEFIELD))
            .then(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "222"
        artist = "Aldo Domínguez"
        flavorText = "\"Splashy entrances are my specialty. And they can be yours too. Try again!\""
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64de7b1f-a03e-4407-91f1-e108a2f26735.jpg?1721427132"

        ruling("2024-07-26", "Once the exiled permanent returns, it's considered a new object with no relation to the object that it was. Auras attached to the exiled permanent will be put into their owners' graveyards. Equipment attached to the exiled permanent will become unattached and remain on the battlefield. Any counters on the exiled permanent will cease to exist.")
        ruling("2024-07-26", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
    }
}
