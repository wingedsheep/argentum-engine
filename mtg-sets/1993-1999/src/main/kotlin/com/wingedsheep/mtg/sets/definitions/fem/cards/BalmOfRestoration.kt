package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Balm of Restoration
 * {2}
 * Artifact
 * {1}, {T}, Sacrifice this artifact: Choose one —
 * • You gain 2 life.
 * • Prevent the next 2 damage that would be dealt to any target this turn.
 *
 * A modal *ability*, not a modal spell, so `countsAsModalSpell = false`.
 */
val BalmOfRestoration = card("Balm of Restoration") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{1}, {T}, Sacrifice this artifact: Choose one —\n" +
        "• You gain 2 life.\n" +
        "• Prevent the next 2 damage that would be dealt to any target this turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.SacrificeSelf)
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.GainLife(2),
                "You gain 2 life"
            ),
            Mode.withTarget(
                Effects.PreventNextDamage(2, EffectTarget.ContextTarget(0)),
                Targets.Any,
                "Prevent the next 2 damage that would be dealt to any target this turn"
            ),
            countsAsModalSpell = false
        )
        description = "{1}, {T}, Sacrifice this artifact: Choose one — You gain 2 life; or " +
            "prevent the next 2 damage that would be dealt to any target this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "82"
        artist = "Margaret Organ-Kean"
        flavorText = "\"Not all armies enjoyed the services of a medic. For them, Balm of Restoration was that much more valuable.\"\n—*Sarpadian Empires, vol. I*"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f95de4a-7fae-42bc-9660-39ea7685ca02.jpg?1783947882"
    }
}
