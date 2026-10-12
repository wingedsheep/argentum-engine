package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Defabricate
 * {1}{U}
 * Instant
 * Choose one —
 * • Counter target artifact or enchantment spell. If that spell is countered this way, exile it
 *   instead of putting it into its owner's graveyard.
 * • Counter target activated or triggered ability.
 *
 * Mode one is Dissipate's exile-counter narrowed to artifact/enchantment spells; mode two is Stifle.
 */
val Defabricate = card("Defabricate") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Counter target artifact or enchantment spell. If that spell is countered this way, exile it " +
        "instead of putting it into its owner's graveyard.\n" +
        "• Counter target activated or triggered ability."

    spell {
        modal(chooseCount = 1) {
            mode("Counter target artifact or enchantment spell; exile it instead of putting it into its owner's graveyard") {
                target(TargetFilter(GameObjectFilter.ArtifactOrEnchantment, zone = Zone.STACK))
                effect = Effects.CounterSpellToExile()
            }
            mode("Counter target activated or triggered ability") {
                target(TargetFilter.ActivatedOrTriggeredAbilityOnStack)
                effect = Effects.CounterAbility()
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "45"
        artist = "Uriah Voth"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7dd32168-0b9c-4633-adec-d41cf125cc35.jpg?1783920114"
        ruling(
            "2022-10-14",
            "The last mode can target delayed triggered abilities. Notably, it can target the delayed " +
                "triggered ability that exiles an unearthed permanent at the beginning of the next end step. " +
                "If that delayed triggered ability is countered, the unearthed permanent remains on the " +
                "battlefield indefinitely."
        )
    }
}
