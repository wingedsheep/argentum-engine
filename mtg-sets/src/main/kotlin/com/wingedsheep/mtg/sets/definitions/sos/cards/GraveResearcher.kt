package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Grave Researcher // Reanimate — Secrets of Strixhaven #85
 * {2}{B} · Creature — Troll Warlock · 3/3
 *
 * At the beginning of your upkeep, surveil 1. Then if there are three or more creature cards
 * in your graveyard, this creature becomes prepared. (While it's prepared, you may cast a copy
 * of its spell. Doing so unprepares it.)
 * //
 * Reanimate — {B}, Sorcery: Put target creature card from a graveyard onto the battlefield
 * under your control. You lose life equal to that card's mana value.
 *
 * Prepare (Secrets of Strixhaven): like Joined Researchers, this creature does NOT enter prepared
 * — it has no PREPARED keyword. The upkeep trigger surveils 1 and *then*, as a single resolution,
 * makes the creature become prepared ([Effects.BecomePrepared]) when three or more creature cards
 * are in your graveyard. The "then if" rider is a [ConditionalEffect] sequenced after the surveil,
 * not a [triggerCondition] — surveil happens unconditionally, the graveyard is checked afterward.
 *
 * Reanimate (back face): a {B} sorcery prepare spell that puts a creature card from any graveyard
 * onto the battlefield under your control ([Effects.Move] to [Zone.BATTLEFIELD] defaults the
 * controller to the caster) and makes you lose life equal to that card's mana value.
 */
val GraveResearcher = card("Grave Researcher") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Troll Warlock"
    power = 3
    toughness = 3
    oracleText = "At the beginning of your upkeep, surveil 1. Then if there are three or more " +
        "creature cards in your graveyard, this creature becomes prepared. (While it's prepared, " +
        "you may cast a copy of its spell. Doing so unprepares it.)"

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Patterns.Library.surveil(1).then(
            ConditionalEffect(
                condition = Conditions.CreatureCardsInGraveyardAtLeast(3),
                effect = Effects.BecomePrepared(),
            )
        )
        description = "At the beginning of your upkeep, surveil 1. Then if there are three or more " +
            "creature cards in your graveyard, this creature becomes prepared."
    }

    // Reanimate — the prepare spell. Reanimate a creature from any graveyard; lose life = its MV.
    prepare("Reanimate") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        oracleText = "Put target creature card from a graveyard onto the battlefield under your " +
            "control. You lose life equal to that card's mana value."
        spell {
            target = Targets.CreatureCardInGraveyard
            effect = Effects.Composite(
                Effects.Move(EffectTarget.ContextTarget(0), Zone.BATTLEFIELD),
                Effects.LoseLife(
                    DynamicAmount.EntityProperty(
                        EntityReference.Target(0),
                        EntityNumericProperty.ManaValue,
                    ),
                    EffectTarget.Controller,
                ),
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "85"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b1e10e8-ea14-4761-910b-4072e2a18456.jpg?1778165067"
    }
}
