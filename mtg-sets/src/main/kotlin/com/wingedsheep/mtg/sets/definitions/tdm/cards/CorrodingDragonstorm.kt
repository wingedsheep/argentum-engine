package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Corroding Dragonstorm — Tarkir: Dragonstorm #75
 * {1}{B} · Enchantment · Uncommon
 *
 * When this enchantment enters, each opponent loses 2 life and you gain 2 life. Surveil 2.
 * When a Dragon you control enters, return this enchantment to its owner's hand.
 *
 * ETB drains each opponent for 2 and gains you 2 ([Effects.LoseLife] to [Player.EachOpponent]
 * then [Effects.GainLife]), then [LibraryPatterns.surveil]. Shares the Dragonstorm-cycle
 * Dragon-bounce trigger.
 */
val CorrodingDragonstorm = card("Corroding Dragonstorm") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, each opponent loses 2 life and you gain 2 life. " +
        "Surveil 2. (Look at the top two cards of your library, then put any number of them into " +
        "your graveyard and the rest on top of your library in any order.)\n" +
        "When a Dragon you control enters, return this enchantment to its owner's hand."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent))
            .then(Effects.GainLife(2))
            .then(LibraryPatterns.surveil(2))
        description = "When this enchantment enters, each opponent loses 2 life and you gain " +
            "2 life. Surveil 2."
    }

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.youControl().withSubtype(Subtype.DRAGON),
            binding = TriggerBinding.OTHER
        )
        effect = Effects.ReturnToHand(EffectTarget.Self)
        description = "When a Dragon you control enters, return this enchantment to its owner's hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "75"
        artist = "Sergey Glushakov"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2a2d395-26d6-4eb2-9e8c-ed7dbbd3a8f5.jpg?1743204259"
    }
}
