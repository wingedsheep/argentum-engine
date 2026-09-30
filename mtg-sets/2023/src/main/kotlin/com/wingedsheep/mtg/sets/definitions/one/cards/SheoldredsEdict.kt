package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sheoldred's Edict — Phyrexia: All Will Be One #108
 * {1}{B}
 * Instant
 * Choose one —
 * • Each opponent sacrifices a nontoken creature of their choice.
 * • Each opponent sacrifices a creature token of their choice.
 * • Each opponent sacrifices a planeswalker of their choice.
 *
 * Three untargeted edict modes, each one [Effects.Sacrifice] over `Player.EachOpponent`
 * with the mode's filter.
 */
val SheoldredsEdict = card("Sheoldred's Edict") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Each opponent sacrifices a nontoken creature of their choice.\n" +
        "• Each opponent sacrifices a creature token of their choice.\n" +
        "• Each opponent sacrifices a planeswalker of their choice."

    spell {
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.Sacrifice(
                    filter = GameObjectFilter.Creature.nontoken(),
                    target = EffectTarget.PlayerRef(Player.EachOpponent),
                ),
                "Each opponent sacrifices a nontoken creature",
            ),
            Mode.noTarget(
                Effects.Sacrifice(
                    filter = GameObjectFilter.Creature.token(),
                    target = EffectTarget.PlayerRef(Player.EachOpponent),
                ),
                "Each opponent sacrifices a creature token",
            ),
            Mode.noTarget(
                Effects.Sacrifice(
                    filter = GameObjectFilter.Planeswalker,
                    target = EffectTarget.PlayerRef(Player.EachOpponent),
                ),
                "Each opponent sacrifices a planeswalker",
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "108"
        artist = "Helge C. Balzer"
        flavorText = "\"Congratulations. I am entertained.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9225cc3-90f0-448f-a8d9-7c6c2796d077.jpg?1783918041"
    }
}
