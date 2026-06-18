package com.wingedsheep.mtg.sets.definitions.big.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Greed's Gambit
 * {3}{B}
 * Enchantment
 *
 * When this enchantment enters, you draw three cards, gain 6 life, and create three 2/1 black
 * Bat creature tokens with flying.
 * At the beginning of your end step, you discard a card, lose 2 life, and sacrifice a creature.
 * When this enchantment leaves the battlefield, you discard three cards, lose 6 life, and
 * sacrifice three creatures.
 *
 * Three triggered abilities, each a [Effects.Composite] of atomic controller-scoped effects. Note
 * the loss clauses target [EffectTarget.Controller] explicitly because [Effects.LoseLife] and
 * [Effects.Sacrifice] default to the opponent. The leaves-the-battlefield trigger
 * ([Triggers.LeavesBattlefield]) fires off the enchantment's last controller, so the cleanup costs
 * fall on its owner regardless of how it left (destroyed, sacrificed, bounced, exiled).
 */
val GreedsGambit = card("Greed's Gambit") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, you draw three cards, gain 6 life, and create " +
        "three 2/1 black Bat creature tokens with flying.\n" +
        "At the beginning of your end step, you discard a card, lose 2 life, and sacrifice a creature.\n" +
        "When this enchantment leaves the battlefield, you discard three cards, lose 6 life, and " +
        "sacrifice three creatures."

    // When this enchantment enters: draw 3, gain 6 life, create three 2/1 black flying Bats.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            listOf(
                Effects.DrawCards(3),
                Effects.GainLife(6),
                Effects.CreateToken(
                    power = 2,
                    toughness = 1,
                    colors = setOf(Color.BLACK),
                    creatureTypes = setOf("Bat"),
                    keywords = setOf(Keyword.FLYING),
                    count = 3,
                    imageUri = "https://cards.scryfall.io/normal/front/5/9/59f138e5-501d-486f-b9a0-3f37640398a0.jpg?1712317347"
                )
            )
        )
        description = "You draw three cards, gain 6 life, and create three 2/1 black Bat creature " +
            "tokens with flying."
    }

    // At the beginning of your end step: discard a card, lose 2 life, sacrifice a creature.
    triggeredAbility {
        trigger = Triggers.YourEndStep
        effect = Effects.Composite(
            listOf(
                Effects.Discard(1, EffectTarget.Controller),
                Effects.LoseLife(2, EffectTarget.Controller),
                Effects.Sacrifice(GameObjectFilter.Creature, count = 1, target = EffectTarget.Controller)
            )
        )
        description = "You discard a card, lose 2 life, and sacrifice a creature."
    }

    // When this enchantment leaves the battlefield: discard 3, lose 6 life, sacrifice 3 creatures.
    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.Composite(
            listOf(
                Effects.Discard(3, EffectTarget.Controller),
                Effects.LoseLife(6, EffectTarget.Controller),
                Effects.Sacrifice(GameObjectFilter.Creature, count = 3, target = EffectTarget.Controller)
            )
        )
        description = "You discard three cards, lose 6 life, and sacrifice three creatures."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "8"
        artist = "Inkognit"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b60a1a6-b2ca-4dc2-a6d9-eff97092079a.jpg?1739804170"
    }
}
