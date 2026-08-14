package com.wingedsheep.mtg.sets.definitions.vis.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Creeping Mold
 * {2}{G}{G}
 * Sorcery
 * Destroy target artifact, enchantment, or land.
 *
 * Visions is the earliest real-expansion printing, so the canonical definition lives here;
 * later printings (Mirrodin, Sixth Edition, …) contribute only `Printing` rows.
 *
 * The three-way union is [Targets.ArtifactEnchantmentOrLand] — a single target slot, not
 * three modes, so a permanent that is (say) both an artifact and a land is one legal choice.
 */
val CreepingMold = card("Creeping Mold") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Destroy target artifact, enchantment, or land."

    spell {
        val t = target("target", Targets.ArtifactEnchantmentOrLand)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "103"
        artist = "David Seeley"
        flavorText = "\"Mold could catch you.\"\n—Suq'Ata insult"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36e7691f-c771-4451-ac54-3532ca10d48f.jpg?1783946984"
    }
}
