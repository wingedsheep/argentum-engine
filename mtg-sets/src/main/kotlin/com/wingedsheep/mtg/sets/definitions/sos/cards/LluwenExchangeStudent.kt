package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Lluwen, Exchange Student // Pest Friend — Secrets of Strixhaven #199
 * {2}{B}{G} · Legendary Creature — Elf Druid · 3/4
 *
 * Lluwen enters prepared. (While it's prepared, you may cast a copy of its spell. Doing so
 * unprepares it.)
 * Exile a creature card from your graveyard: Lluwen becomes prepared. Activate only as a sorcery.
 * //
 * Pest Friend — {B/G}, Sorcery: Create a 1/1 black and green Pest creature token with "Whenever
 * this token attacks, you gain 1 life."
 *
 * Prepare (Secrets of Strixhaven): the [Keyword.PREPARED] keyword makes Lluwen enter prepared.
 * The re-prepare ability is a non-tap activated ability whose cost exiles a creature card from
 * your graveyard ([Costs.ExileFromGraveyard]); at sorcery speed it makes Lluwen become prepared
 * again via [Effects.BecomePrepared]. The Pest token carries its own self-attack life-gain
 * trigger via [CreateTokenEffect.triggeredAbilities].
 */
val LluwenExchangeStudent = card("Lluwen, Exchange Student") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Elf Druid"
    power = 3
    toughness = 4
    oracleText = "Lluwen enters prepared. (While it's prepared, you may cast a copy of its spell. " +
        "Doing so unprepares it.)\nExile a creature card from your graveyard: Lluwen becomes " +
        "prepared. Activate only as a sorcery."

    keywords(Keyword.PREPARED)

    // Exile a creature card from your graveyard: Lluwen becomes prepared. Sorcery speed only.
    activatedAbility {
        cost = Costs.ExileFromGraveyard(1, GameObjectFilter.Creature)
        timing = TimingRule.SorcerySpeed
        effect = Effects.BecomePrepared(EffectTarget.Self)
    }

    // Pest Friend — the prepare spell. Create a 1/1 B/G Pest with an attack life-gain trigger.
    prepare("Pest Friend") {
        manaCost = "{B/G}"
        typeLine = "Sorcery"
        oracleText = "Create a 1/1 black and green Pest creature token with \"Whenever this token " +
            "attacks, you gain 1 life.\""
        spell {
            effect = CreateTokenEffect(
                count = DynamicAmount.Fixed(1),
                power = 1,
                toughness = 1,
                colors = setOf(Color.BLACK, Color.GREEN),
                creatureTypes = setOf("Pest"),
                triggeredAbilities = listOf(
                    TriggeredAbility.create(
                        trigger = Triggers.Attacks.event,
                        binding = Triggers.Attacks.binding,
                        effect = Effects.GainLife(1),
                    ),
                ),
                imageUri = "https://cards.scryfall.io/normal/front/b/a/ba854032-6ad2-4654-990a-64006e7f92fd.jpg?1777982237",
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "199"
        artist = "Alix Branwyn"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0bcb638-c3c8-4973-9537-5c471f43f34f.jpg?1778165015"
    }
}
