package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Psemilla, Meletian Poet — Foundations Jumpstart #31
 * {2}{W} · Legendary Creature — Human Bard · 1/1
 *
 * Whenever you cast your first enchantment spell each turn, create a 2/2 white Nymph enchantment
 * creature token.
 * At the beginning of each combat, if you control five or more enchantments, Psemilla gets +4/+4 and
 * gains lifelink until end of turn.
 *
 * - "Your first enchantment spell each turn" is `castsNth(1, Enchantment)` (Arcane Bombardment's
 *   shape): the ordinal counts only enchantment casts, so an earlier creature spell doesn't close
 *   the window.
 * - "Each combat" is every player's beginning of combat; the five-enchantment count is an
 *   intervening-if, checked when it would trigger and again on resolution.
 */
val PsemillaMeletianPoet = card("Psemilla, Meletian Poet") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Bard"
    power = 1
    toughness = 1
    oracleText = "Whenever you cast your first enchantment spell each turn, create a 2/2 white Nymph " +
        "enchantment creature token.\n" +
        "At the beginning of each combat, if you control five or more enchantments, Psemilla gets +4/+4 " +
        "and gains lifelink until end of turn. (Damage dealt by this creature also causes you to gain " +
        "that much life.)"

    triggeredAbility {
        trigger = Triggers.you.castsNth(1, GameObjectFilter.Enchantment)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Nymph"),
            enchantmentToken = true
        )
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.YouControlAtLeast(5, GameObjectFilter.Enchantment)
        effect = Effects.ModifyStats(4, 4, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "31"
        artist = "KiKi"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/431258f5-705e-411d-b396-6992bc84d816.jpg?1783908861"
    }
}
