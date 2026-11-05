package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Solitary Camel — Hour of Devastation #23 (canonical printing)
 * {2}{W} · Creature — Camel · 3/2
 *
 * This creature has lifelink as long as you control a Desert or there is a Desert card in your
 * graveyard.
 *
 * A layer-6 self [GrantKeyword] gated by a [ConditionalStaticAbility]; the Desert condition is the
 * same either-half composition Gilded Cerodon uses, re-read at every projection so lifelink comes
 * and goes as Deserts enter or leave the battlefield and graveyard.
 */
val SolitaryCamel = card("Solitary Camel") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Camel"
    power = 3
    toughness = 2
    oracleText = "This creature has lifelink as long as you control a Desert or there is a Desert card in your graveyard. " +
        "(Damage dealt by this creature also causes you to gain that much life.)"

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.LIFELINK, GroupFilter.source()),
            condition = Conditions.Any(
                Conditions.ControlPermanentOfType(Subtype.DESERT),
                Conditions.GraveyardContainsSubtype(Subtype.DESERT),
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "23"
        artist = "Aaron Miller"
        flavorText = "Deserts are inhospitable, not uninhabitable."
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8adb8dbf-c83c-4169-b822-8e32aa36cffa.jpg?1783936060"
        ruling("2017-07-14", "If an ability checks whether you control a Desert or there is a Desert card in your graveyard, having more than one doesn’t matter. Controlling one is the same as controlling five. There is also no extra bonus for both controlling one and having one in your graveyard.")
    }
}
