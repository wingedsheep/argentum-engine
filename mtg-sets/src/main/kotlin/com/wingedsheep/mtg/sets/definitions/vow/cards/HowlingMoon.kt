package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Howling Moon
 * {2}{G}
 * Enchantment
 *
 * At the beginning of combat on your turn, target Wolf or Werewolf you control gets +2/+2 until
 * end of turn.
 * Whenever an opponent casts their second spell each turn, create a 2/2 green Wolf creature token.
 *
 * Implementation:
 *  - The begin-combat pump targets a Wolf-or-Werewolf you control via a [TargetFilter] union
 *    (`.or`), matching Blood Mist's begin-combat targeted-pump idiom.
 *  - "Whenever an opponent casts their second spell each turn" is [Triggers.NthSpellCast] with
 *    n = 2 scoped to [Player.EachOpponent]. `Player.EachOpponent` is used deliberately rather than
 *    `Player.AnOpponent`: the engine's trigger `matchesPlayer` routes `EachOpponent` to
 *    "caster != controller", so the count is per-opponent and never fires on your own spells.
 */
val HowlingMoon = card("Howling Moon") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "At the beginning of combat on your turn, target Wolf or Werewolf you control gets " +
        "+2/+2 until end of turn.\n" +
        "Whenever an opponent casts their second spell each turn, create a 2/2 green Wolf creature token."

    // At the beginning of combat on your turn, target Wolf or Werewolf you control gets +2/+2.
    triggeredAbility {
        trigger = Triggers.BeginCombat
        val wolfOrWerewolf = target(
            "target Wolf or Werewolf you control",
            TargetCreature(
                filter = TargetFilter.CreatureYouControl.withSubtype(Subtype.WOLF)
                    .or(TargetFilter.CreatureYouControl.withSubtype(Subtype.WEREWOLF)),
            ),
        )
        effect = Effects.ModifyStats(2, 2, wolfOrWerewolf)
        description = "At the beginning of combat on your turn, target Wolf or Werewolf you control " +
            "gets +2/+2 until end of turn."
    }

    // Whenever an opponent casts their second spell each turn, create a 2/2 green Wolf token.
    triggeredAbility {
        trigger = Triggers.NthSpellCast(2, Player.EachOpponent)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf"),
            controller = EffectTarget.Controller,
            imageUri = "https://cards.scryfall.io/normal/front/d/5/d5f1e139-3054-4273-8a4d-faaaa9c383a8.jpg?1783924694",
        )
        description = "Whenever an opponent casts their second spell each turn, create a 2/2 green " +
            "Wolf creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "204"
        artist = "Alessandra Pisano"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca50b6a5-2e58-4de3-b0e1-0b33536f69a6.jpg?1783924810"
    }
}
