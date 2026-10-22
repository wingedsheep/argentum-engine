package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pack's Betrayal
 * {2}{R}
 * Sorcery
 * Gain control of target creature until end of turn. Untap that creature. It gains haste until
 * end of turn. If you control a Wolf or Werewolf, scry 2.
 *
 * The Wolf/Werewolf check is made on resolution, after control changes — so a stolen Wolf or
 * Werewolf counts toward it.
 */
val PacksBetrayal = card("Pack's Betrayal") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature until end of turn. Untap that creature. It gains haste until end of turn. If you control a Wolf or Werewolf, scry 2. (Look at the top two cards of your library, then put any number of them on the bottom and the rest on top in any order.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.Untap(t) then
            Effects.GrantKeyword(Keyword.HASTE, t) then
            Effects.If(
                condition = Conditions.YouControl(GameObjectFilter.Permanent.withAnySubtype("Wolf", "Werewolf")),
                then = Effects.Scry(2),
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Sam Rowan"
        flavorText = "Tovolar's howl of command was too much to resist, even for Arlinn's own loyal pack."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7ebadb1e-32cd-41d6-b26e-49134973d05c.jpg?1783925594"
    }
}
