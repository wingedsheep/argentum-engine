package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Role Reversal — War of the Spark #214
 * {U}{U}{R} · Sorcery · Rare
 *
 * Exchange control of two target permanents that share a permanent type.
 *
 * Dependent target selection, the shape of The Trickster-God's Heist chapter II: the second slot's
 * filter reads the first through `sharingCardTypeWith(first, among = permanent types)`. The `among`
 * set keeps the shared type to a *permanent* type (artifact, battle, creature, enchantment, land,
 * planeswalker) — two Kindred permanents don't pair up on Kindred alone, and supertypes such as
 * legendary never count. Either target may have types the other lacks. Both targets are re-checked
 * on resolution; if either is illegal the exchange doesn't happen, and if one player controls both
 * nothing happens.
 */
val RoleReversal = card("Role Reversal") {
    manaCost = "{U}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Sorcery"
    oracleText = "Exchange control of two target permanents that share a permanent type."

    spell {
        val first = target(TargetFilter(GameObjectFilter.Permanent))
        val second = target(
            TargetFilter(
                GameObjectFilter.Permanent.sharingCardTypeWith(
                    first,
                    among = CardType.entries.filter { it.isPermanent }.toSet()
                )
            )
        )
        effect = Effects.ExchangeControl(first, second)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "214"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59b47017-d982-4d4c-94fc-f265a5b7f36a.jpg?1783933388"
        ruling("2019-05-03", "Gaining control of a permanent doesn't cause you to gain control of any Auras or Equipment attached to it.")
        ruling("2019-05-03", "If one of the target permanents is an illegal target when Role Reversal resolves, the exchange won't happen.")
        ruling("2019-05-03", "Role Reversal's effect lasts indefinitely. It doesn't wear off during the cleanup step, and it doesn't expire if the permanents stop sharing a permanent type after Role Reversal has resolved.")
        ruling("2019-05-03", "You don't have to control either target permanent.")
        ruling("2019-05-03", "If the same player controls both permanents when Role Reversal resolves, nothing happens.")
        ruling("2019-05-03", "The permanent types are artifact, creature, enchantment, land, and planeswalker. Supertypes, like legendary, aren't permanent types. For example, you can't exchange control of a legendary creature with a legendary planeswalker.")
    }
}
