package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Complete the Circuit
 * {5}{U}
 * Instant
 * Convoke
 * You may cast sorcery spells this turn as though they had flash.
 * When you next cast an instant or sorcery spell this turn, copy that spell twice.
 * You may choose new targets for the copies.
 *
 * Flash grant is `GrantFlashToSpells` filtered to sorceries; the "next cast" rider is a one-shot
 * delayed trigger (as The Clone Saga) copying the triggering spell twice, each copy retargeted
 * independently (CR 707.10c).
 */
val CompleteTheCircuit = card("Complete the Circuit") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while " +
        "casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "You may cast sorcery spells this turn as though they had flash.\n" +
        "When you next cast an instant or sorcery spell this turn, copy that spell twice. " +
        "You may choose new targets for the copies."

    keywords(Keyword.CONVOKE)

    spell {
        effect = Effects.GrantFlashToSpells(
            spellFilter = GameObjectFilter.Sorcery,
            duration = Duration.EndOfTurn
        ) then Effects.CreateDelayedTrigger(
            trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery),
            effect = Effects.CopyTargetSpell(
                target = EffectTarget.TriggeringEntity,
                copies = DynamicAmounts.fixed(2),
            ),
            fireOnce = true,
            expiry = DelayedTriggerExpiry.EndOfTurn,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Eelis Kyttanen"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cdf5de96-9418-48f1-a18e-01a0108c4f97.jpg?1783917038"
    }
}
