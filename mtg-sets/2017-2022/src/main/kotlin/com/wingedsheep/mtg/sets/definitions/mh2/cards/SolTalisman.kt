package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Sol Talisman — Modern Horizons 2 #236
 * (no mana cost) · Artifact
 *
 * Suspend 3—{1} (Rather than cast this card from your hand, pay {1} and exile it with three time counters on it. At the beginning of your upkeep, remove a time counter. When the last is removed, you may cast it without paying its mana cost.)
 * {T}: Add {C}{C}.
 *
 * Printed with **no mana cost** — CR 202.1b/118.6 make that an unpayable cost, so the card can't be
 * cast normally and suspend is its only route onto the battlefield (barring some other free-cast
 * effect). `manaCost = ""` is what the DSL reads to set `hasNoManaCost`; `"{0}"` would parse to a
 * payable zero cost and leave it castable for free, which is a different card. Unlike Ancestral
 * Vision there is no `colorIndicator`: Sol Talisman is genuinely colorless (CR 202.2), not a colored
 * card whose color the missing mana cost hides.
 *
 * Suspend is card-type agnostic (CR 702.62a) — the engine exiles the artifact with time counters
 * and casts it when the last is removed, exactly as for a creature or a sorcery. It is written as
 * the parameterized [KeywordAbility.Suspend], cost first and time counters second; the display-only
 * `Keyword.SUSPEND` is derived from it by `CardBuilder.build()`. Note the reminder text has no "It
 * has haste" clause: haste is granted only to permanents that are creatures.
 *
 * The mana ability is the Sol Ring shape, two colorless mana for a tap, with `manaAbility = true`
 * deriving `TimingRule.ManaAbility`.
 */
val SolTalisman = card("Sol Talisman") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Suspend 3—{1} (Rather than cast this card from your hand, pay {1} and exile it with three time counters on it. At the beginning of your upkeep, remove a time counter. When the last is removed, you may cast it without paying its mana cost.)\n" +
        "{T}: Add {C}{C}."

    keywordAbility(KeywordAbility.suspend("{1}", 3))

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(2)
        manaAbility = true
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "236"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a51fb64d-cc0c-400d-971f-78c28d42043b.jpg?1783926801"
    }
}
