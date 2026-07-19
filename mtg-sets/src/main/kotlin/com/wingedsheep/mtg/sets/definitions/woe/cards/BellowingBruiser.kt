package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CantBlockEffect
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Bellowing Bruiser // Beat a Path
 * {4}{R}
 * Creature — Ogre
 * 4/4
 * Haste
 *
 * Adventure: Beat a Path — {2}{R}, Sorcery — Adventure
 * Up to two target creatures can't block this turn.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 * caster cast it as the creature spell while it remains in exile.)
 */
val BellowingBruiser = card("Bellowing Bruiser") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre"
    oracleText = "Haste"
    power = 4
    toughness = 4

    keywords(Keyword.HASTE)

    adventure("Beat a Path") {
        manaCost = "{2}{R}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Up to two target creatures can't block this turn. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            target = TargetCreature(count = 2, optional = true)
            effect = ForEachTargetEffect(listOf(CantBlockEffect(EffectTarget.ContextTarget(0))))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "121"
        artist = "Kai Carpenter"
        flavorText = "You can't give an ogre the runaround."
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26ece013-f3ef-4c12-9dea-b2789f61f8a0.jpg?1783915098"
    }
}
