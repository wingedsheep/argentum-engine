package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.CreateTokenEffect
import com.wingedsheep.rulesengine.ability.DestroyEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.ModalEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.ManaCost

/**
 * Unforgiving Aim
 *
 * {2}{G} Instant
 * Choose one —
 * • Destroy target creature with flying.
 * • Destroy target enchantment.
 * • Create a 2/2 black and green Elf creature token.
 */
object UnforgivingAim {
    val definition = CardDefinition.instant(
        name = "Unforgiving Aim",
        manaCost = ManaCost.parse("{2}{G}"),
        oracleText = "Choose one —\n• Destroy target creature with flying.\n• Destroy target enchantment.\n• Create a 2/2 black and green Elf creature token.",
        metadata = ScryfallMetadata(
            collectorNumber = "200",
            rarity = Rarity.COMMON,
            artist = "Filip Burburan",
            imageUri = "https://cards.scryfall.io/normal/front/d/d/ddee4567-8901-2345-fghi-ddee45678901.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Unforgiving Aim") {
        // TODO: Each mode needs its own targeting setup
        // Mode 1: Destroy target creature with flying
        // Mode 2: Destroy target enchantment
        // Mode 3: Create a 2/2 black and green Elf creature token

        spell(
            ModalEffect(
                modes = listOf(
                    DestroyEffect(EffectTarget.TargetCreature),  // Mode 1: Destroy creature with flying
                    DestroyEffect(EffectTarget.TargetCreature),  // Mode 2: Destroy enchantment (placeholder)
                    CreateTokenEffect(
                        count = 1,
                        power = 2,
                        toughness = 2,
                        colors = setOf(Color.BLACK, Color.GREEN),
                        creatureTypes = setOf("Elf")
                    )  // Mode 3: Create Elf token
                )
            )
        )
    }
}
