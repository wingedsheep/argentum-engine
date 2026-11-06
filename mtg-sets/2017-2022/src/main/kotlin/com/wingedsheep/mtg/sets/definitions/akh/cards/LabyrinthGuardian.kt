package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.embalm
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Labyrinth Guardian — Amonkhet #60
 * {1}{U} · Creature — Illusion Warrior · Uncommon
 * 2/3
 *
 * When this creature becomes the target of a spell, sacrifice it.
 * Embalm {3}{U}
 *
 * Spells only: abilities targeting it don't fire the sacrifice. The embalmed token is a copy,
 * so it carries the same drawback.
 */
val LabyrinthGuardian = card("Labyrinth Guardian") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Illusion Warrior"
    power = 2
    toughness = 3
    oracleText = "When this creature becomes the target of a spell, sacrifice it.\n" +
        "Embalm {3}{U} ({3}{U}, Exile this card from your graveyard: Create a token that's a copy of it, " +
        "except it's a white Zombie Illusion Warrior with no mana cost. Embalm only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.becomesTarget(spellsOnly = true)
        effect = Effects.SacrificeTarget(EffectTarget.Self)
    }

    embalm("{3}{U}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "60"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17921573-e3d1-45a7-b7e5-a27ae1fa3c2c.jpg"
    }
}
