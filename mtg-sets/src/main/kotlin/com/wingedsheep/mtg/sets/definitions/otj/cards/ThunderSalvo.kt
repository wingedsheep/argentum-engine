package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Thunder Salvo {1}{R}
 * Instant
 *
 * Thunder Salvo deals X damage to target creature, where X is 2 plus the number
 * of other spells you've cast this turn.
 *
 * X is computed at resolution as `2 + (other spells you've cast this turn)` via
 * [DynamicAmount.Add] of a constant and [DynamicAmount.SpellsCastThisTurn] with
 * `excludeSelf = true`. Thunder Salvo's own cast record is on the per-player history
 * by the time it resolves, so excludeSelf keeps "other" faithful.
 */
val ThunderSalvo = card("Thunder Salvo") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Thunder Salvo deals X damage to target creature, where X is 2 plus " +
        "the number of other spells you've cast this turn."

    spell {
        val creature = target("creature", Targets.Creature)
        effect = Effects.DealDamage(
            amount = DynamicAmount.Add(
                left = DynamicAmount.Fixed(2),
                right = DynamicAmount.SpellsCastThisTurn(Player.You, excludeSelf = true)
            ),
            target = creature
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Johann Bodin"
        flavorText = "\"You said one weapon in the duel. Ain't my fault you forgot to specify size.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0bf0ea9-0929-4d33-815a-6df29c399e7e.jpg?1712355867"
    }
}
