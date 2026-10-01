package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Cankerbloom — Phyrexia: All Will Be One #161
 * {1}{G}
 * Creature — Phyrexian Fungus
 * 3/2
 *
 * {1}, Sacrifice this creature: Choose one —
 * • Destroy target artifact.
 * • Destroy target enchantment.
 * • Proliferate.
 *
 * Modal activated ability: `{1}` plus sacrificing itself, then [ModalEffect.chooseOne] over
 * two targeted destroy modes and an untargeted [Effects.Proliferate].
 */
val Cankerbloom = card("Cankerbloom") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Phyrexian Fungus"
    power = 3
    toughness = 2
    oracleText = "{1}, Sacrifice this creature: Choose one —\n" +
        "• Destroy target artifact.\n" +
        "• Destroy target enchantment.\n" +
        "• Proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeSelf)
        effect = ModalEffect.chooseOne(
            mode("Destroy target artifact") {
                val artifact = target(TargetFilter.Artifact)
                effect = Effects.Destroy(artifact)
            },
            mode("Destroy target enchantment") {
                val enchantment = target(TargetFilter.Enchantment)
                effect = Effects.Destroy(enchantment)
            },
            Mode.noTarget(Effects.Proliferate(), "Proliferate"),
        )
        description = "{1}, Sacrifice this creature: Choose one — Destroy target artifact; " +
            "or destroy target enchantment; or proliferate."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "161"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89b39293-6f57-4294-85fc-c718bdbb4d40.jpg?1783918019"
    }
}
