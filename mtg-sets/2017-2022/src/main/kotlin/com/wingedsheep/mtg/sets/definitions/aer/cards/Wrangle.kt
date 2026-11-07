package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wrangle — Aether Revolt #101 (canonical / earliest real printing, 2017)
 * {1}{R} · Sorcery
 *
 * Gain control of target creature with power 4 or less until end of turn. Untap that creature.
 * It gains haste until end of turn.
 *
 * The threaten chain ([Effects.GainControl] until end of turn, [Effects.Untap],
 * [Effects.GrantKeyword] haste) over a creature target with power 4 or less.
 */
val Wrangle = card("Wrangle") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature with power 4 or less until end of turn. Untap that creature. " +
        "It gains haste until end of turn."

    spell {
        val creature = target(TargetFilter.Creature.powerAtMost(4))
        effect = Effects.GainControl(creature, Duration.EndOfTurn) then
            Effects.Untap(creature) then
            Effects.GrantKeyword(Keyword.HASTE, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Jason Rainville"
        flavorText = "Gremlins are loyal to nothing but their next meal."
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5ea93a49-5a7c-4d15-8548-a57c9460e0f0.jpg?1783936748"
        ruling(
            "2017-02-09",
            "Wrangle can target any creature with power 4 or less, even one that you already control or that " +
                "is already untapped."
        )
        ruling(
            "2017-02-09",
            "If the target creature's power becomes 5 or greater as you gain control of it (perhaps because an " +
                "enchantment gives creatures you control +1/+1), Wrangle will continue to resolve as normal. The " +
                "creature will still be untapped and gain haste until end of turn, and you'll still control it " +
                "until end of turn."
        )
    }
}
