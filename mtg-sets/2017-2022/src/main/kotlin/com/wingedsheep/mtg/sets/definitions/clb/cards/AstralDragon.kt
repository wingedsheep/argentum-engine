package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Astral Dragon — Commander Legends: Battle for Baldur's Gate #613
 * {6}{U}{U} · Creature — Dragon · 4/4
 *
 * Flying
 * Project Image — When this creature enters, create two tokens that are copies of target
 * noncreature permanent, except they're 3/3 Dragon creatures in addition to their other types, and
 * they have flying.
 *
 * Rebuild the City / Nexus of Becoming's copy-exception rail on
 * [Effects.CreateTokenCopyOfTarget]: `overridePower/Toughness = 3`, `addCardTypes = {CREATURE}`,
 * `addedSubtypes = {Dragon}` and `addedKeywords = {FLYING}` are copiable-value exceptions, so the
 * copied permanent's tapped state, counters and attachments don't carry over. Copying an Aura
 * creates nothing: a creature Aura can't enchant anything (CR 303.4d), so it has no legal host and
 * the token copy isn't created (CR 303.4g) — the ruling on this card.
 */
val AstralDragon = card("Astral Dragon") {
    manaCost = "{6}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Project Image — When this creature enters, create two tokens that are copies of target " +
        "noncreature permanent, except they're 3/3 Dragon creatures in addition to their other types, " +
        "and they have flying."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val original = target(TargetFilter.NoncreaturePermanent)
        effect = Effects.CreateTokenCopyOfTarget(
            target = original,
            count = 2,
            overridePower = 3,
            overrideToughness = 3,
            addCardTypes = setOf("CREATURE"),
            addedSubtypes = setOf(Subtype("Dragon")),
            addedKeywords = setOf(Keyword.FLYING),
        )
        description = "Project Image — Create two tokens that are copies of target noncreature " +
            "permanent, except they're 3/3 Dragon creatures with flying."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "613"
        artist = "Anastasia Ovchinnikova"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5bf8b03-1f5c-46c5-a684-ae5c04630286.jpg?1783922540"
        ruling("2022-06-10", "Except for power, toughness, types, and the flying ability, the token copies exactly what was printed on the original permanent and nothing else (unless that permanent is copying something else or is a token; see below). It doesn't copy whether that permanent is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, and so on.")
        ruling("2022-06-10", "If the copied permanent has {X} in its mana cost, X is considered to be 0.")
        ruling("2022-06-10", "If the copied permanent is a token, the new token that's created copies the original characteristics of that token, with the exceptions noted above.")
        ruling("2022-06-10", "If the copied permanent is copying something else, then the token enters the battlefield as whatever that permanent copied, with the exceptions noted above.")
        ruling("2022-06-10", "Any enters-the-battlefield abilities of the copied permanent will trigger when the token enters the battlefield. Any “as [this permanent] enters the battlefield” or “[this permanent] enters the battlefield with” abilities of the chosen permanent will also work.")
        ruling("2022-06-10", "If you target an Aura with Astral Dragon's ability, no tokens will be created.")
        ruling("2022-06-10", "Since the tokens are creatures, they can't attack or {T} until your next turn if they don't have haste.")
    }
}
