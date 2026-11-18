package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Reviving Melody
 * {2}{G}
 * Sorcery
 *
 * Choose one or both —
 * • Return target creature card from your graveyard to your hand.
 * • Return target enchantment card from your graveyard to your hand.
 *
 * "Choose one or both" is the mode *count* — `chooseCount = 2, minChooseCount = 1` (CR 700.2),
 * not a third "both" mode. Each mode carries its own graveyard target. An enchantment creature
 * card fits either mode, but one card can't be chosen for both (CR 115.3).
 */
val RevivingMelody = card("Reviving Melody") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose one or both —\n" +
        "• Return target creature card from your graveyard to your hand.\n" +
        "• Return target enchantment card from your graveyard to your hand."

    spell {
        modal(chooseCount = 2, minChooseCount = 1) {
            mode("Return target creature card from your graveyard to your hand") {
                val creature = target(TargetFilter.CreatureInYourGraveyard)
                effect = Effects.ReturnToHand(creature)
            }
            mode("Return target enchantment card from your graveyard to your hand") {
                val enchantment = target(TargetFilter(GameObjectFilter.Enchantment.ownedByYou(), zone = Zone.GRAVEYARD))
                effect = Effects.ReturnToHand(enchantment)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Cynthia Sheppard"
        flavorText = "\"Listen to my song, and you need never forge a golden mask to return to me.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49b152cc-d39f-470e-8b62-e4a833db7b88.jpg?1783939409"
    }
}
