package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shadow of the Goblin — Marvel's Spider-Man #87
 * {1}{R} · Enchantment
 *
 * Unreliable Visions — At the beginning of your first main phase, discard a card. If you do,
 * draw a card.
 * Undying Vengeance — Whenever you play a land or cast a spell from anywhere other than your
 * hand, this enchantment deals 1 damage to each opponent.
 *
 * The "play a land … from anywhere other than your hand" half uses the new
 * `Triggers.youPlayLand(fromZoneOtherThan = Zone.HAND)` (`LandPlayedEvent`); the spell half uses
 * the existing `SpellCastPredicate.CastFromZoneOtherThan`. Modeled as two triggered abilities —
 * each event fires once, matching the single printed ability.
 */
val ShadowOfTheGoblin = card("Shadow of the Goblin") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Unreliable Visions — At the beginning of your first main phase, discard a card. " +
        "If you do, draw a card.\n" +
        "Undying Vengeance — Whenever you play a land or cast a spell from anywhere other than " +
        "your hand, this enchantment deals 1 damage to each opponent."

    // Unreliable Visions — first main phase: discard a card. If you do, draw a card.
    triggeredAbility {
        trigger = Triggers.FirstMainPhase
        effect = IfYouDoEffect(action = Patterns.Hand.discardCards(1), ifYouDo = DrawCardsEffect(1))
    }

    // Undying Vengeance — play a land from a non-hand zone → 1 damage to each opponent.
    triggeredAbility {
        trigger = Triggers.youPlayLand(fromZoneOtherThan = Zone.HAND)
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    // Undying Vengeance — cast a spell from a non-hand zone → 1 damage to each opponent.
    triggeredAbility {
        trigger = Triggers.youCastSpell(
            requires = setOf(SpellCastPredicate.CastFromZoneOtherThan(Zone.HAND))
        )
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Pavel Kolomeyets"
        flavorText = "\"It's time, Harry. Take the mask and avenge me.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/5/854b6898-c480-435b-8952-a077c7977cec.jpg?1783905334"
    }
}
