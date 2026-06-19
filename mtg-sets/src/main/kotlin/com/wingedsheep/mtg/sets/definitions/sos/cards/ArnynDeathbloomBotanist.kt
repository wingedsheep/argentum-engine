package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Arnyn, Deathbloom Botanist — Secrets of Strixhaven #74
 * {2}{B} · Legendary Creature — Vampire Druid · 2/2
 *
 * Deathtouch
 * Whenever a creature you control with power or toughness 1 or less dies, target opponent
 * loses 2 life and you gain 2 life.
 *
 * The trigger matches a creature you control whose **power OR toughness** is 1 or less (the OR
 * cap is modeled by [GameObjectFilter.powerOrToughnessAtMost], evaluated against last-known
 * battlefield stats when the dies event fires). The triggering creature can be Arnyn itself if
 * it qualifies. "Target opponent loses 2 life and you gain 2 life" is a single chosen-opponent
 * target plus a controller life gain.
 */
val ArnynDeathbloomBotanist = card("Arnyn, Deathbloom Botanist") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Vampire Druid"
    power = 2
    toughness = 2
    oracleText = "Deathtouch\n" +
        "Whenever a creature you control with power or toughness 1 or less dies, target opponent loses 2 life and you gain 2 life."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().powerOrToughnessAtMost(1),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD,
            ),
            binding = TriggerBinding.ANY,
        )
        val opponent = target("target opponent", Targets.Opponent)
        effect = Effects.Composite(
            Effects.LoseLife(2, opponent),
            Effects.GainLife(2),
        )
        description = "Whenever a creature you control with power or toughness 1 or less dies, target opponent loses 2 life and you gain 2 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Anna Steinbauer"
        flavorText = "\"My lovelies need meat, just like any other carnivore.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/1/6168b472-0930-4db5-9920-407340b99050.jpg?1775937426"
    }
}
