package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mindstab Thrull
 * {1}{B}{B}
 * Creature — Thrull
 * 2/2
 * Whenever this creature attacks and isn't blocked, you may sacrifice it. If you do, defending
 * player discards three cards.
 *
 * The sacrifice is the price of the discard, so both happen on resolution or neither does. The
 * defending player chooses which three cards to discard.
 */
val MindstabThrull = card("Mindstab Thrull") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Thrull"
    oracleText = "Whenever this creature attacks and isn't blocked, you may sacrifice it. If you " +
        "do, defending player discards three cards."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.AttacksAndIsntBlocked
        effect = MayEffect(
            Effects.Composite(
                SacrificeSelfEffect,
                Effects.Discard(3, EffectTarget.PlayerRef(Player.DefendingPlayer))
            ),
            descriptionOverride = "sacrifice this creature. If you do, defending player discards three cards",
        )
        description = "Whenever this creature attacks and isn't blocked, you may sacrifice it. If you do, defending player discards three cards."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "40a"
        artist = "Richard Kane Ferguson"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/499a791f-ac4f-4a96-b59b-37043686a79a.jpg?1783947902"
    }
}
