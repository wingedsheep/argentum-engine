package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Impossible Choice
 * {2}{B}
 * Instant
 *
 * Choose one —
 * • Surgical Separation — Destroy target creature. Its controller creates two 1/1 red Officer
 *   creature tokens.
 * • Remain Fused — You draw two cards and each player loses 2 life.
 */
val ImpossibleChoice = card("Impossible Choice") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Surgical Separation — Destroy target creature. Its controller creates two 1/1 red Officer creature tokens.\n" +
        "• Remain Fused — You draw two cards and each player loses 2 life."

    spell {
        modal(chooseCount = 1) {
            mode("Surgical Separation — Destroy target creature. Its controller creates two 1/1 red Officer creature tokens") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.Destroy(creature) then
                    Effects.CreateToken(
                        power = 1,
                        toughness = 1,
                        colors = setOf(Color.RED),
                        creatureTypes = setOf("Officer"),
                        count = 2,
                        controller = EffectTarget.TargetController,
                        imageUri = "https://cards.scryfall.io/normal/front/d/6/d676cbef-8e31-4ae6-8dfe-6b84160f71e0.jpg?1793363893"
                    )
            }

            mode("Remain Fused — You draw two cards and each player loses 2 life") {
                effect = Effects.DrawCards(2) then
                    Effects.LoseLife(2, EffectTarget.PlayerRef(Player.Each))
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Douglas Shuler"
        flavorText = "Saving Neelix and Tuvok meant destroying the person they'd become."
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c5a87cf-7790-4335-8497-ff793bbdcb05.jpg?1793013157"
    }
}
