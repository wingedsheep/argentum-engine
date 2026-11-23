package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rust Scarab
 * {4}{G}
 * Creature — Insect
 * 4/5
 * Whenever this creature becomes blocked, you may destroy target artifact or enchantment
 * defending player controls.
 */
val RustScarab = card("Rust Scarab") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect"
    oracleText = "Whenever this creature becomes blocked, you may destroy target artifact or enchantment " +
        "defending player controls."
    power = 4
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.becomesBlocked()
        val t = target(TargetFilter.ArtifactOrEnchantment.defendingPlayerControls())
        effect = Effects.May(Effects.Destroy(t))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "130"
        artist = "Adam Paquette"
        flavorText = "\"Behold nature's answer to the problem of society.\"\n—Nikya of the Old Ways"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c57335b5-30e2-431f-ab50-d5f1981783c3.jpg?1783940116"
    }
}
