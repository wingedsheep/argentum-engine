package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sauron, the Necromancer — The Lord of the Rings: Tales of Middle-earth #106
 * {3}{B}{B} · Legendary Creature — Avatar Horror · Rare
 * 4/4
 *
 * Menace
 * Whenever Sauron attacks, exile target creature card from your graveyard. Create a tapped and
 * attacking token that's a copy of that card, except it's a 3/3 black Wraith with menace. At the
 * beginning of the next end step, exile that token unless Sauron is your Ring-bearer.
 *
 * Modeling:
 * - The attack trigger targets a creature card in your graveyard, exiles it, then creates a token
 *   copy of "that card" via [Effects.CreateTokenCopyOfTarget]. The token copies the exiled card's
 *   copiable values (read from its CardComponent, which survives the move to exile), then applies
 *   the printed overrides: 3/3 (`overridePower`/`overrideToughness`), black (`overrideColors`),
 *   Wraith (`overrideSubtypes`, replacing the copied creature types), and menace (`addedKeywords`).
 *   It enters tapped and attacking (`tapped`/`attacking`).
 * - "At the beginning of the next end step, exile that token unless Sauron is your Ring-bearer"
 *   is the copy effect's `exileAtStep = Step.END` with `exileUnlessSourceIsRingBearer = true`. The
 *   firing step is the next end step of any player's turn ("the next end step"); the Ring-bearer
 *   gate (CR 701.54e) is re-evaluated against Sauron when the delayed trigger fires.
 */
val SauronTheNecromancer = card("Sauron, the Necromancer") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Avatar Horror"
    power = 4
    toughness = 4
    oracleText = "Menace\n" +
        "Whenever Sauron attacks, exile target creature card from your graveyard. Create a tapped " +
        "and attacking token that's a copy of that card, except it's a 3/3 black Wraith with " +
        "menace. At the beginning of the next end step, exile that token unless Sauron is your " +
        "Ring-bearer."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.Attacks
        val card = target(
            "target creature card from your graveyard",
            Targets.CreatureCardInYourGraveyard
        )
        effect = Effects.Composite(listOf(
            Effects.Exile(card),
            Effects.CreateTokenCopyOfTarget(
                target = EffectTarget.ContextTarget(0),
                overridePower = 3,
                overrideToughness = 3,
                overrideColors = setOf(Color.BLACK),
                overrideSubtypes = setOf(Subtype("Wraith")),
                addedKeywords = setOf(Keyword.MENACE),
                tapped = true,
                attacking = true,
                exileAtStep = Step.END,
                exileUnlessSourceIsRingBearer = true
            )
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "106"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/377d65d8-21c8-4292-97db-610e0173ba59.jpg?1686968699"
    }
}
