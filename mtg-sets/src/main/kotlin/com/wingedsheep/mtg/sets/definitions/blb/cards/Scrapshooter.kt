package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.MiscPatterns

/**
 * Scrapshooter {1}{G}{G}
 * Creature — Raccoon Archer
 * 4/4
 *
 * Gift a card (You may promise an opponent a gift as you cast this spell.
 * If you do, when it enters, they draw a card.)
 * Reach
 * When this creature enters, if the gift was promised, destroy target artifact
 * or enchantment an opponent controls.
 */
val Scrapshooter = card("Scrapshooter") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Raccoon Archer"
    power = 4
    toughness = 4
    oracleText = "Gift a card (You may promise an opponent a gift as you cast this spell. If you do, when it enters, they draw a card.)\nReach\nWhen this creature enters, if the gift was promised, destroy target artifact or enchantment an opponent controls."

    keywords(Keyword.REACH)

    // Gift modeled as a modal ETB triggered ability
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MiscPatterns.giftSpell(
            // Mode 1: No gift — do nothing
            Mode.noTarget(
                Effects.Composite(emptyList()),
                "Don't promise a gift"
            ),
            // Mode 2: Gift a card — opponent draws, destroy target artifact or enchantment
            Mode(
                effect = DrawCardsEffect(1, EffectTarget.PlayerRef(Player.EachOpponent))
                    .then(Effects.Destroy(EffectTarget.ContextTarget(0)))
                    .then(Effects.GiftGiven()),
                targetRequirements = listOf(
                    TargetObject(filter = TargetFilter.ArtifactOrEnchantment.opponentControls())
                ),
                description = "Promise a gift — opponent draws a card, destroy target artifact or enchantment an opponent controls"
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "191"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c42ab407-e72d-4c48-9a9e-2055b5e71c69.jpg?1721426916"

        ruling("2024-07-26", "For permanent spells with gift, an ability triggers when that permanent enters if the gift was promised. When that ability resolves, the gift is given to the appropriate opponent.")
        ruling("2024-07-26", "If a spell for which the gift was promised is countered, doesn't resolve, or is otherwise removed from the stack, the gift won't be given.")
    }
}
