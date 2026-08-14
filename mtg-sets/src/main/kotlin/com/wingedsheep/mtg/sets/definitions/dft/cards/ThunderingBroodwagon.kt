package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Thundering Broodwagon — Aetherdrift #225
 * {2}{B}{B}{G}{G} · Artifact — Vehicle · 6/5
 *
 * Menace, reach
 * When this Vehicle enters, destroy target nonland permanent an opponent controls with mana value
 * 4 or less.
 * Crew 3
 * Cycling {2}
 *
 * A Vehicle carries its keywords in every zone but only *matters* while it's a creature — menace
 * and reach sit on the card as printed keywords and the combat rules read them off projected state
 * once it's crewed, so nothing special is needed here.
 *
 * The ETB is the plain destroy shape over [TargetFilter.NonlandPermanentOpponentControls] narrowed
 * by `manaValueAtMost(4)`. Mana value is read off the *card*, so an animated land is still a land
 * (not a legal target) and a face-down 2/2 has mana value 0.
 *
 * Cycling it never fires the ETB: cycling discards the card from hand, so the Vehicle never enters.
 */
val ThunderingBroodwagon = card("Thundering Broodwagon") {
    manaCost = "{2}{B}{B}{G}{G}"
    colorIdentity = "BG"
    typeLine = "Artifact — Vehicle"
    power = 6
    toughness = 5
    oracleText = "Menace, reach\n" +
        "When this Vehicle enters, destroy target nonland permanent an opponent controls with " +
        "mana value 4 or less.\n" +
        "Crew 3\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    keywords(Keyword.MENACE, Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val permanent = target(
            "nonland permanent an opponent controls with mana value 4 or less",
            TargetPermanent(filter = TargetFilter.NonlandPermanentOpponentControls.manaValueAtMost(4))
        )
        effect = Effects.Destroy(permanent)
        description = "When this Vehicle enters, destroy target nonland permanent an opponent " +
            "controls with mana value 4 or less."
    }

    keywordAbility(KeywordAbility.crew(3))
    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "225"
        artist = "Bartek Fedyczak"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d1576d4-15a3-4e91-84ef-e71e258185e7.jpg?1783907851"
    }
}
