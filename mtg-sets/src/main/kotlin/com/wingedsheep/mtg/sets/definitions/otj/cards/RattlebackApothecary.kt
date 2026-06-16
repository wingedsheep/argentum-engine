package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Rattleback Apothecary
 * {2}{B}
 * Creature — Gorgon Warlock
 * 3/2
 *
 * Deathtouch
 * Whenever you commit a crime, target creature you control gains your choice of menace or lifelink
 * until end of turn.
 *
 * The crime trigger ([Triggers.YouCommitCrime]) targets a creature you control and offers a
 * [ModalEffect.chooseOne] between two [GrantKeywordEffect]s — the same "your choice of keyword X or Y"
 * shape as Manifold Mouse. Each mode grants its keyword to the chosen creature (ContextTarget(0)) for
 * `Duration.EndOfTurn`. The crime-this-turn tracker is read at the engine's `CrimeDetector` emit site;
 * this card only consumes [Triggers.YouCommitCrime].
 */
val RattlebackApothecary = card("Rattleback Apothecary") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Gorgon Warlock"
    power = 3
    toughness = 2
    oracleText = "Deathtouch\n" +
        "Whenever you commit a crime, target creature you control gains your choice of menace or " +
        "lifelink until end of turn. (Targeting opponents, anything they control, and/or cards in " +
        "their graveyards is a crime.)"

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.YouCommitCrime
        val t = target("target", TargetCreature(filter = TargetFilter.Creature.youControl()))
        effect = ModalEffect.chooseOne(
            Mode.noTarget(GrantKeywordEffect(Keyword.MENACE, t, Duration.EndOfTurn), "Menace"),
            Mode.noTarget(GrantKeywordEffect(Keyword.LIFELINK, t, Duration.EndOfTurn), "Lifelink")
        )
        description = "Whenever you commit a crime, target creature you control gains your choice of " +
            "menace or lifelink until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "100"
        artist = "Loïc Canavaggia"
        flavorText = "\"Looking for a little liquid courage? I also stock liquid murder, if that's what you're after.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a88e233-f09c-49e7-b1e3-386fba851fdf.jpg?1712355645"
    }
}
