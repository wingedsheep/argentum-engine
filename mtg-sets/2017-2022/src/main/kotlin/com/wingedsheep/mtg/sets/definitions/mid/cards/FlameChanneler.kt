package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Flame Channeler // Embodiment of Flame (Innistrad: Midnight Hunt)
 * {1}{R} Creature — Human Wizard 2/2 // Creature — Elemental Wizard 3/3 (transform)
 *
 * Front — Flame Channeler
 *   When a spell you control deals damage, transform this creature.
 *
 * Back — Embodiment of Flame
 *   Whenever a spell you control deals damage, put a flame counter on this creature.
 *   {1}, Remove a flame counter from this creature: Exile the top card of your library. You may
 *   play that card this turn.
 *
 * "A spell you control" is a spell on the stack, and only an instant or sorcery deals damage as
 * part of its own resolution (rulings below) — so the source filter is Bloodfeather Phoenix's
 * `InstantOrSorcery.youControl()`. A fight spell's creatures and a permanent's enters trigger are
 * other sources and never match. Two triggers from one spell transform the Channeler only once:
 * the engine's face-change clock (CR 701.27f) stops the second.
 */

private const val CHANNELER_TEXT = "When a spell you control deals damage, transform this creature."

private val FlameChannelerFront = card("Flame Channeler") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = CHANNELER_TEXT

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.InstantOrSorcery.youControl()).dealsDamage()
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Véronique Meignaud"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be91fcba-4599-4ecb-824d-55112096c34a.jpg?1783925605"
        ruling(
            "2021-09-24",
            "Some spells cause other objects to deal damage, such as spells that have two creatures fight. In " +
                "those cases, it's those other objects that are dealing damage, not the spell itself, and this " +
                "will not cause Flame Channeler's abilities to trigger."
        )
        ruling(
            "2021-09-24",
            "\"A spell you control\" means a spell on the stack. Usually, spells on the stack can deal damage " +
                "only as part of their own resolution."
        )
        ruling(
            "2021-09-24",
            "A spell that becomes a permanent and then deals damage, such as one that has a triggered ability " +
                "when it enters the battlefield, will not cause Flame Channeler's abilities to trigger."
        )
    }
}

private const val IMPULSE_TEXT = "{1}, Remove a flame counter from this creature: Exile the top card of your " +
    "library. You may play that card this turn."

private val EmbodimentOfFlame = card("Embodiment of Flame") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Elemental Wizard"
    power = 3
    toughness = 3
    oracleText = "Whenever a spell you control deals damage, put a flame counter on this creature.\n$IMPULSE_TEXT"

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.InstantOrSorcery.youControl()).dealsDamage()
        effect = Effects.AddCounters(CounterType.FLAME, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.RemoveCounterFromSelf(CounterType.FLAME),
        )
        effect = Patterns.Exile.impulse(1)
        description = IMPULSE_TEXT
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Véronique Meignaud"
        imageUri = "https://cards.scryfall.io/normal/back/b/e/be91fcba-4599-4ecb-824d-55112096c34a.jpg?1783925605"
    }
}

val FlameChanneler: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = FlameChannelerFront,
    backFace = EmbodimentOfFlame,
)
