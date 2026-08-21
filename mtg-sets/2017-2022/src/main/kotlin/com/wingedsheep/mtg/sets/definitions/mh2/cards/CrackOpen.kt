package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Crack Open — Modern Horizons 2 #154
 * {2}{G} · Sorcery
 *
 * Destroy target artifact or enchantment. Create a Treasure token. (It's an artifact with "{T}, Sacrifice this token: Add one mana of any color.")
 *
 * `Targets.ArtifactOrEnchantment` is the single heterogeneous target requirement — one target that
 * may be either card type, not two separate targets. [Effects.Destroy] lowers to a move to the
 * graveyard flagged `byDestruction`, so regeneration and indestructible get their chance.
 *
 * The Treasure is created unconditionally: it is a separate sentence, not a rider on the
 * destruction, so it still happens if the target has become illegal by resolution — only the
 * destruction is skipped. [Effects.CreateTreasure] pulls the predefined token, whose own
 * "{T}, Sacrifice this token: Add one mana of any color" ability is the reminder text above.
 */
val CrackOpen = card("Crack Open") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Destroy target artifact or enchantment. Create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    spell {
        val t = target("target artifact or enchantment", Targets.ArtifactOrEnchantment)
        effect = Effects.Destroy(t) then Effects.CreateTreasure()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "154"
        artist = "Yeong-Hao Han"
        flavorText = "What no spell or lockpick could open, the forest coaxed apart."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42b77a34-9b69-4b01-9f2c-2ff8de47fc12.jpg?1783926833"
    }
}
