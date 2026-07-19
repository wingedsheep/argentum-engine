package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hopeless Nightmare
 * {B}
 * Enchantment
 *
 * When this enchantment enters, each opponent discards a card and loses 2 life.
 * When this enchantment is put into a graveyard from the battlefield, scry 2.
 * {2}{B}: Sacrifice this enchantment.
 *
 * The sacrifice is the *effect* of the activated ability (not part of its cost), so it uses the
 * stack and can be responded to — and sacrificing this way is what puts the enchantment into the
 * graveyard, firing the scry trigger.
 */
val HopelessNightmare = card("Hopeless Nightmare") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, each opponent discards a card and loses 2 life.\n" +
        "When this enchantment is put into a graveyard from the battlefield, scry 2.\n" +
        "{2}{B}: Sacrifice this enchantment."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            Effects.EachOpponentDiscards(1),
            Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent))
        )
    }

    triggeredAbility {
        trigger = Triggers.PutIntoGraveyardFromBattlefield
        effect = Patterns.Library.scry(2)
    }

    activatedAbility {
        cost = Costs.Mana("{2}{B}")
        effect = SacrificeSelfEffect
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "Dominik Mayer"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c2ee817-9ca9-4f09-bc71-7994c19a9470.jpg?1783915106"
    }
}
