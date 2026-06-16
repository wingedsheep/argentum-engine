package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Intimidation Campaign
 * {1}{U}{B}
 * Enchantment
 *
 * When this enchantment enters, each opponent loses 1 life, you gain 1 life, and you draw a card.
 * Whenever you commit a crime, you may return this enchantment to its owner's hand.
 *
 * The crime trigger uses [MayEffect] (a yes/no decision gate) so the controller may decline the
 * whole bounce — `optional = true` on a trigger only loosens target minimums, not the effect
 * itself. Returning happens only from the battlefield (the [Effects.ReturnToHand] of
 * [EffectTarget.Self] is a no-op if the source has already left), which matches the reminder text.
 */
val IntimidationCampaign = card("Intimidation Campaign") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, each opponent loses 1 life, you gain 1 life, " +
        "and you draw a card.\n" +
        "Whenever you commit a crime, you may return this enchantment to its owner's hand. " +
        "(It returns only from the battlefield. Targeting opponents, anything they control, " +
        "and/or cards in their graveyards is a crime.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            listOf(
                LoseLifeEffect(1, EffectTarget.PlayerRef(Player.EachOpponent)),
                GainLifeEffect(1, EffectTarget.Controller),
                Effects.DrawCards(1),
            ),
        )
        description = "When this enchantment enters, each opponent loses 1 life, you gain 1 life, " +
            "and you draw a card."
    }

    triggeredAbility {
        trigger = Triggers.YouCommitCrime
        effect = MayEffect(Effects.ReturnToHand(EffectTarget.Self))
        description = "Whenever you commit a crime, you may return this enchantment to its owner's hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "208"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/596fb7a2-bb79-44b7-ad84-414c8139ec13.jpg?1712356111"
    }
}
