package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Drown in the Loch
 * {U}{B}
 * Instant
 *
 * Choose one —
 * • Counter target spell with mana value less than or equal to the number of cards in its
 *   controller's graveyard.
 * • Destroy target creature with mana value less than or equal to the number of cards in its
 *   controller's graveyard.
 *
 * "Its controller's graveyard" is the *candidate's* controller, not the caster's: a dynamic
 * filter cap binds the controller of the object under test as [Player.Candidate] — the spell's
 * caster, or the creature's controller. The cap is re-read on resolution (CR 608.2b).
 */
private val cardsInItsControllersGraveyard =
    DynamicAmounts.cardsInYourGraveyard(Player.Candidate)

val DrownInTheLoch = card("Drown in the Loch") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Counter target spell with mana value less than or equal to the number of cards in its controller's graveyard.\n" +
        "• Destroy target creature with mana value less than or equal to the number of cards in its controller's graveyard."

    spell {
        modal(chooseCount = 1) {
            mode("Counter target spell with mana value less than or equal to the number of cards in its controller's graveyard") {
                target(TargetFilter.SpellOnStack.manaValueAtMostDynamic(cardsInItsControllersGraveyard))
                effect = Effects.CounterSpell()
            }
            mode("Destroy target creature with mana value less than or equal to the number of cards in its controller's graveyard") {
                val creature = target(TargetFilter.Creature.manaValueAtMostDynamic(cardsInItsControllersGraveyard))
                effect = Effects.Destroy(creature)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "188"
        artist = "John Stanko"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8bf5df5b-164d-4ec2-a5e6-bbaea152e271.jpg?1783932598"
    }
}
