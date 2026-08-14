package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Amazing Acrobatics
 * {1}{U}{U}
 * Instant
 * Choose one or both —
 * • Counter target spell.
 * • Tap one or two target creatures.
 */
val AmazingAcrobatics = card("Amazing Acrobatics") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one or both —\n• Counter target spell.\n• Tap one or two target creatures."

    spell {
        effect = ModalEffect(
            modes = listOf(
                Mode.withTarget(
                    effect = Effects.CounterSpell(),
                    target = Targets.Spell,
                    description = "Counter target spell."
                ),
                Mode.withTarget(
                    effect = ForEachTargetEffect(listOf(Effects.Tap(EffectTarget.ContextTarget(0)))),
                    target = TargetCreature(count = 2, minCount = 1),
                    description = "Tap one or two target creatures."
                )
            ),
            chooseCount = 2,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Justyna Dura"
        flavorText = "\"Hey, who added these deadly laser beams? Rude.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a2f6d84-3d83-4f48-9906-11f681171930.jpg?1757376894"
    }
}
