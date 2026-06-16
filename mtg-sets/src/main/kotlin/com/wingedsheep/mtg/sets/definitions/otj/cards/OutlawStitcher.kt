package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Outlaw Stitcher
 * {3}{U}
 * Creature — Human Warlock
 * 1/4
 *
 * When this creature enters, create a 2/2 blue and black Zombie Rogue creature token, then put two
 * +1/+1 counters on that token for each spell you've cast this turn other than the first.
 * Plot {4}{U}
 *
 * The ETB composes the atomic create-token + add-counters pipeline: [Effects.CreateToken] publishes
 * the new token's entity id under [CREATED_TOKENS], and [Effects.AddDynamicCounters] addresses it via
 * `EffectTarget.PipelineTarget(CREATED_TOKENS, 0)`. The "two counters for each spell other than the
 * first" amount is `2 * max(spellsCastThisTurn - 1, 0)`. Per the 2024-04-12 ruling, the count includes
 * Outlaw Stitcher itself (if it wasn't the first spell), countered/unresolved spells, and spells still
 * on the stack — exactly the `spellsCastThisTurnByPlayer` cast-history that [DynamicAmount.SpellsCastThisTurn]
 * reads.
 */
val OutlawStitcher = card("Outlaw Stitcher") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Warlock"
    power = 1
    toughness = 4
    oracleText = "When this creature enters, create a 2/2 blue and black Zombie Rogue creature token, " +
        "then put two +1/+1 counters on that token for each spell you've cast this turn other than the first.\n" +
        "Plot {4}{U} (You may pay {4}{U} and exile this card from your hand. Cast it as a sorcery on a later " +
        "turn without paying its mana cost. Plot only as a sorcery.)"

    keywordAbility(KeywordAbility.plot("{4}{U}"))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        // 2 counters per spell cast this turn other than the first: 2 * max(spellsCast - 1, 0).
        val otherThanFirst = DynamicAmount.Max(
            DynamicAmount.Subtract(
                DynamicAmount.SpellsCastThisTurn(),
                DynamicAmount.Fixed(1)
            ),
            DynamicAmount.Fixed(0)
        )
        val counterAmount = DynamicAmount.Multiply(otherThanFirst, 2)
        effect = Effects.Composite(
            Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.BLUE, Color.BLACK),
                creatureTypes = setOf("Zombie", "Rogue"),
                imageUri = "https://cards.scryfall.io/normal/front/7/4/74c7a0bd-6011-495a-b56c-8fa707dd7f12.jpg?1712316777"
            ),
            Effects.AddDynamicCounters(
                counterType = Counters.PLUS_ONE_PLUS_ONE,
                amount = counterAmount,
                target = EffectTarget.PipelineTarget(CREATED_TOKENS, 0)
            )
        )
        description = "When this creature enters, create a 2/2 blue and black Zombie Rogue creature token, " +
            "then put two +1/+1 counters on that token for each spell you've cast this turn other than the first."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Alix Branwyn"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba9584f0-55b8-448d-99a7-041934053f42.jpg?1712355468"

        ruling("2024-04-12", "Outlaw Stitcher's ability will count all spells you've cast this turn except the first, including itself (if it wasn't the first spell), spells that were countered or didn't resolve, and spells that are still on the stack.")
    }
}
