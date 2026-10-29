package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hammerhead Tyrant
 * {4}{U}{U}
 * Creature — Dragon
 * 6/6
 *
 * Flying
 * Whenever you cast a spell, return up to one target nonland permanent an opponent controls with
 * mana value less than or equal to that spell's mana value to its owner's hand.
 *
 * "That spell" is the trigger's triggering entity — the cast spell, still on the stack while the
 * trigger is put on the stack and resolves above it — so the cap is
 * `manaValueAtMostEntity(EffectTarget.TriggeringEntity)` (Light-Paws / Saruman shape). The engine's
 * mana-value read on the stack includes the chosen X (CR 202.3e), matching the ruling.
 */
val HammerheadTyrant = card("Hammerhead Tyrant") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Dragon"
    power = 6
    toughness = 6
    oracleText = "Flying\n" +
        "Whenever you cast a spell, return up to one target nonland permanent an opponent controls " +
        "with mana value less than or equal to that spell's mana value to its owner's hand."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts()
        val t = target(
            TargetFilter(
                GameObjectFilter.NonlandPermanent
                    .opponentControls()
                    .manaValueAtMostEntity(EffectTarget.TriggeringEntity),
            ),
            optional = true,
        )
        effect = Effects.ReturnToHand(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "21"
        artist = "Xavier Ribeiro"
        flavorText = "Without subjects, but a tyrant nonetheless."
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87d4d694-c80e-435a-ba1b-f2f1949f91fc.jpg?1783907181"
        ruling("2025-04-04", "If a spell has {X} in its mana cost, use the value of X chosen as it was cast to determine that spell's mana value.")
        ruling("2025-04-04", "If a permanent has {X} in its mana cost, X is 0 for the purpose of determining its mana value.")
    }
}
