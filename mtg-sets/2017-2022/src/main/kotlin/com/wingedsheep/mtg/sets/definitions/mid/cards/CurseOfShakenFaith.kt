package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private const val SHAKEN_FAITH_TEXT =
    "Whenever enchanted player casts a spell other than the first spell they cast each turn or " +
        "copies a spell, this Aura deals 2 damage to them."

/**
 * Curse of Shaken Faith (Innistrad: Midnight Hunt #134)
 * {1}{R}
 * Enchantment — Aura Curse
 * Enchant player
 * Whenever enchanted player casts a spell other than the first spell they cast each turn or copies
 * a spell, this Aura deals 2 damage to them.
 *
 * The two halves of the trigger gate differently — only the *cast* half excludes the first spell —
 * so they are written as the cast trigger with Ichneumon Druid's `triggerRestriction` (the
 * enchanted player's own cast history, which already holds the triggering spell, so "at least 2"
 * is exactly "not the first") and a separate, ungated copy trigger. A cast and a copy are disjoint
 * events (CR 707.10: a copy isn't cast), so every event fires exactly one of them. Counting the
 * cast history rather than spells seen since the Curse arrived is the 2021-09-24 ruling.
 */
val CurseOfShakenFaith = card("Curse of Shaken Faith") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura Curse"
    oracleText = "Enchant player\n$SHAKEN_FAITH_TEXT"

    auraTarget = Targets.Player

    // ... casts a spell other than the first spell they cast each turn ...
    triggeredAbility {
        trigger = Triggers.player(Player.EnchantedPlayer).casts()
        triggerRestriction = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastThisTurn(player = Player.TriggeringPlayer),
            ComparisonOperator.GTE,
            2,
        )
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = SHAKEN_FAITH_TEXT
    }

    // ... or copies a spell.
    triggeredAbility {
        trigger = Triggers.player(Player.EnchantedPlayer).copies()
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = SHAKEN_FAITH_TEXT
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "134"
        artist = "Campbell White"
        flavorText = "Even within the chapel's hallowed halls, the devils' laughter pierced his prayers."
        imageUri = "https://cards.scryfall.io/normal/front/3/7/371b7795-5cfc-4e2b-b26c-2def52c61dbf.jpg?1783925603"
        ruling(
            "2021-09-24",
            "If a player who has already cast a spell this turn becomes enchanted by Curse of Shaken Faith, " +
                "casting more spells the same turn will cause the ability to trigger.",
        )
    }
}
