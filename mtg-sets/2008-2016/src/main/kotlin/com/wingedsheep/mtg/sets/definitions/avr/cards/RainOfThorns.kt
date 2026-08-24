package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Rain of Thorns
 * {4}{G}{G}
 * Sorcery
 * Choose one or more —
 * • Destroy target artifact.
 * • Destroy target enchantment.
 * • Destroy target land.
 *
 * A choose-one-or-more modal spell — [modal] with `chooseCount = 3, minChooseCount = 1`, so any
 * one, two, or all three modes may be picked. Each mode carries its own [TargetPermanent], filtered
 * by [TargetFilter.Artifact], [TargetFilter.Enchantment] and [TargetFilter.Land] respectively, and
 * each body is the plain [Effects.Destroy].
 */
val RainOfThorns = card("Rain of Thorns") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose one or more —\n" +
        "• Destroy target artifact.\n" +
        "• Destroy target enchantment.\n" +
        "• Destroy target land."

    spell {
        modal(chooseCount = 3, minChooseCount = 1) {
            mode("Destroy target artifact") {
                val t = target("target", TargetPermanent(filter = TargetFilter.Artifact))
                effect = Effects.Destroy(t)
            }
            mode("Destroy target enchantment") {
                val t = target("target", TargetPermanent(filter = TargetFilter.Enchantment))
                effect = Effects.Destroy(t)
            }
            mode("Destroy target land") {
                val t = target("target", TargetPermanent(filter = TargetFilter.Land))
                effect = Effects.Destroy(t)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "190"
        artist = "Sam Burley"
        flavorText = "When the forests became havens for evil, the archmages devised new ways to cleanse the wilds."
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd1cb530-b9d5-4386-b89e-2acecc8294c8.jpg"
        ruling("2012-05-01", "You can choose just one mode, any two of the modes, or all three. You make this choice as you cast Rain of Thorns.")
    }
}
