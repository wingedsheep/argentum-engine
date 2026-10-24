package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crackle with Power — Strixhaven: School of Mages #95 (canonical printing)
 * {X}{X}{X}{R}{R} · Sorcery
 *
 * Crackle with Power deals five times X damage to each of up to X targets.
 *
 * The announced X caps the targets (`Targets.AnyUpTo(X)`) and
 * sets the damage, so X = 0 is a legal cast with no targets that does nothing.
 */
val CrackleWithPower = card("Crackle with Power") {
    manaCost = "{X}{X}{X}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Crackle with Power deals five times X damage to each of up to X targets."

    spell {
        target(Targets.AnyUpTo(DynamicAmounts.xValue()))
        effect = Effects.ForEachTarget(
            Effects.DealDamage(DynamicAmounts.xValue() * 5, EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "95"
        artist = "Micah Epstein"
        flavorText = "Will gasped as Rowan drew from the raw power of the Strixhaven Snarl. " +
            "For a moment, she was luminous, transcendent, terrible."
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de547f52-3798-4b3a-947d-24873251204b.jpg?1783927356"
    }
}
