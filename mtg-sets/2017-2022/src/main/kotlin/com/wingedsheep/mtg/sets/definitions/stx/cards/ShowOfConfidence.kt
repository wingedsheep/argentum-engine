package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Show of Confidence — Strixhaven: School of Mages #28 (canonical printing)
 * {1}{W} · Instant
 *
 * When you cast this spell, copy it for each other instant and sorcery spell you've cast this
 * turn. You may choose new targets for the copies.
 * Put a +1/+1 counter on target creature. It gains vigilance until end of turn.
 *
 * The cast trigger ([Triggers.self] `isCast()`, Mentor's Guidance's shape) copies the triggering
 * spell [DynamicAmounts.spellsCastThisTurn] times. The count is read as the trigger resolves, so
 * instants cast in response to it count too (ruling) — that is `excludeSelf = true` (drops Show
 * of Confidence's own cast record, keyed by its stack entity id), *not* the storm-style
 * `beforeTriggeringSpell`. Copies are put on the stack, not cast (CR 707.10), so they neither
 * count nor re-trigger.
 */
val ShowOfConfidence = card("Show of Confidence") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "When you cast this spell, copy it for each other instant and sorcery spell " +
        "you've cast this turn. You may choose new targets for the copies.\n" +
        "Put a +1/+1 counter on target creature. It gains vigilance until end of turn."

    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.CopyTargetSpell(
            target = EffectTarget.TriggeringEntity,
            copies = DynamicAmounts.spellsCastThisTurn(
                player = Player.You,
                filter = GameObjectFilter.InstantOrSorcery,
                excludeSelf = true,
            ),
        )
        description = "When you cast this spell, copy it for each other instant and sorcery spell " +
            "you've cast this turn. You may choose new targets for the copies."
    }

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.GrantKeyword(Keyword.VIGILANCE, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "28"
        artist = "Paul Scott Canavan"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65837f39-deb7-4a4b-8b67-125a71cd0d45.jpg?1783927386"
        ruling("2021-04-16", "The triggered ability counts all instants and sorceries that were cast before it resolves. If you cast instant spells in response to the ability, those spells will count.")
        ruling("2021-04-16", "Magecraft abilities trigger once for each copy created as Show of Confidence's triggered ability resolves.")
        ruling("2021-04-16", "If the target of the original Show of Confidence has become an illegal target when that spell is copied, the copies will initially still target the same creature. If you don't choose new targets for the copies, they each will fail to resolve. Notably, you don't have to change targets, especially not to opposing creatures, just because the current target of Show of Confidence is illegal.")
    }
}
