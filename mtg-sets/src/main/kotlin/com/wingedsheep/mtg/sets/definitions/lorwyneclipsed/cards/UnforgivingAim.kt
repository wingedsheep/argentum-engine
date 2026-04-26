package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Unforgiving Aim
 * {2}{G}
 * Instant
 *
 * Choose one —
 * • Destroy target creature with flying.
 * • Destroy target enchantment.
 * • Create a 2/2 black and green Elf creature token.
 */
val UnforgivingAim = card("Unforgiving Aim") {
    manaCost = "{2}{G}"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Destroy target creature with flying.\n• Destroy target enchantment.\n• Create a 2/2 black and green Elf creature token."

    spell {
        effect = ModalEffect.chooseOne(
            Mode.withTarget(
                Effects.Destroy(EffectTarget.ContextTarget(0)),
                TargetCreature(filter = TargetFilter.Creature.withKeyword(Keyword.FLYING)),
                "Destroy target creature with flying"
            ),
            Mode.withTarget(
                Effects.Destroy(EffectTarget.ContextTarget(0)),
                Targets.Enchantment,
                "Destroy target enchantment"
            ),
            Mode.noTarget(
                Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.BLACK, Color.GREEN),
                    creatureTypes = setOf("Elf"),
                    imageUri = "https://cards.scryfall.io/normal/front/3/9/39b36f22-21f9-44fe-8a49-bdc859503342.jpg?1767955588"
                ),
                "Create a 2/2 black and green Elf creature token"
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "200"
        artist = "Filip Burburan"
        flavorText = "\"I tend to my field of view like a garden. Occasionally, it needs weeding.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0bef3905-24f8-419b-aeca-396adfc6d0dc.jpg?1767872046"
    }
}
