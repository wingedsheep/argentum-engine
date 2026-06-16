package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ferocification
 * {2}{R}
 * Enchantment
 *
 * At the beginning of combat on your turn, choose one —
 * • Target creature you control gets +2/+0 until end of turn.
 * • Target creature you control gains menace and haste until end of turn.
 *
 * A modal beginning-of-combat trigger ([Triggers.BeginCombat] fires on the controller's
 * turn only). Each mode declares its own "target creature you control"
 * ([Mode.withTarget]) and applies an existing effect: mode 1 is [Effects.ModifyStats]
 * +2/+0; mode 2 grants menace and haste ([Effects.GrantKeyword]) until end of turn.
 */
val Ferocification = card("Ferocification") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "At the beginning of combat on your turn, choose one —\n" +
        "• Target creature you control gets +2/+0 until end of turn.\n" +
        "• Target creature you control gains menace and haste until end of turn."

    triggeredAbility {
        trigger = Triggers.BeginCombat
        effect = ModalEffect.chooseOne(
            Mode.withTarget(
                Effects.ModifyStats(2, 0, EffectTarget.ContextTarget(0)),
                Targets.CreatureYouControl,
                "Target creature you control gets +2/+0 until end of turn",
            ),
            Mode.withTarget(
                Effects.Composite(
                    Effects.GrantKeyword(Keyword.MENACE, EffectTarget.ContextTarget(0)),
                    Effects.GrantKeyword(Keyword.HASTE, EffectTarget.ContextTarget(0)),
                ),
                Targets.CreatureYouControl,
                "Target creature you control gains menace and haste until end of turn",
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Mila Pesic"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78db4260-6fc8-4500-afd7-2c845ac0d53b.jpg?1712355750"
    }
}
