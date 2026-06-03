package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Sarkhan's Resolve — Tarkir: Dragonstorm #158
 * {1}{G} · Instant
 *
 * Choose one —
 * • Target creature gets +3/+3 until end of turn.
 * • Destroy target creature with flying.
 */
val SarkhansResolve = card("Sarkhan's Resolve") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Target creature gets +3/+3 until end of turn.\n" +
        "• Destroy target creature with flying."

    spell {
        effect = ModalEffect.chooseOne(
            Mode(
                effect = Effects.ModifyStats(power = 3, toughness = 3, target = EffectTarget.ContextTarget(0)),
                targetRequirements = listOf(TargetCreature()),
                description = "Target creature gets +3/+3 until end of turn"
            ),
            Mode(
                effect = Effects.Destroy(EffectTarget.ContextTarget(0)),
                targetRequirements = listOf(TargetCreature(filter = TargetFilter.Creature.withKeyword(Keyword.FLYING))),
                description = "Destroy target creature with flying"
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "158"
        artist = "Billy Christian"
        flavorText = "With Taigam whispering encouragement in his ear, Sarkhan threw aside his misgivings. The ritual would return him to his former glory. All he needed was a dragon's heart."
        imageUri = "https://cards.scryfall.io/normal/front/c/a/cae56fef-b661-4bc5-b9a1-3871ae06e491.jpg?1743204600"
    }
}
