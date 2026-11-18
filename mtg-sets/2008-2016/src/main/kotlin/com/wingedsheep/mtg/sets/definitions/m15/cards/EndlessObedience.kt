package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Endless Obedience — Magic 2015 #94 (canonical printing)
 * {4}{B}{B} · Sorcery
 *
 * Convoke
 * Put target creature card from a graveyard onto the battlefield under your control.
 *
 * "A graveyard" means any graveyard, so the target is [TargetFilter.CreatureInGraveyard] and the
 * card enters under the caster's control regardless of who owns it.
 */
val EndlessObedience = card("Endless Obedience") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText =
        "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Put target creature card from a graveyard onto the battlefield under your control."

    keywords(Keyword.CONVOKE)

    spell {
        val creature = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.Move(
            creature,
            Zone.BATTLEFIELD,
            controllerOverride = EffectTarget.Controller,
            fromZone = Zone.GRAVEYARD
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c70ceae6-8b25-4ce2-b89b-45300401fec5.jpg?1783939184"
    }
}
