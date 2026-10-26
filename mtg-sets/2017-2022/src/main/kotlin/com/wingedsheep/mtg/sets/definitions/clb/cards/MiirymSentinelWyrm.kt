package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Miirym, Sentinel Wyrm
 * {3}{G}{U}{R}
 * Legendary Creature — Dragon Spirit
 * 6/6
 * Flying, ward {2}
 * Whenever another nontoken Dragon you control enters, create a token that's a copy of it, except
 * the token isn't legendary.
 *
 * "Another" is the OTHER binding; `nontoken()` keeps the copies from re-triggering it. The copy is
 * of the entering Dragon's copiable values (its last-known ones if it has already left), and
 * "isn't legendary" is a copy exception ([CopyExceptions.removedSupertypes]), so it is part of the
 * token's copiable values (CR 707.9b).
 */
val MiirymSentinelWyrm = card("Miirym, Sentinel Wyrm") {
    manaCost = "{3}{G}{U}{R}"
    colorIdentity = "GUR"
    typeLine = "Legendary Creature — Dragon Spirit"
    power = 6
    toughness = 6
    oracleText = "Flying, ward {2}\n" +
        "Whenever another nontoken Dragon you control enters, create a token that's a copy of it, " +
        "except the token isn't legendary."

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Permanent.withSubtype("Dragon").youControl().nontoken()).enters()
        effect = Effects.CreateTokenCopyOfTarget(
            target = EffectTarget.TriggeringEntity,
            exceptions = CopyExceptions(removedSupertypes = setOf(Supertype.LEGENDARY)),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "284"
        artist = "Kekai Kotaki"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a934590b-5c70-4f07-af67-fbe817a99531.jpg?1783922689"
        ruling("2022-06-10", "Miirym, Sentinel Wyrm's triggered ability copies the printed values of the Dragon that entered the battlefield. It won't copy any counters on that creature or any effects that have changed its power, toughness, types, color, and so on.")
        ruling("2022-06-10", "If the Dragon that entered the battlefield is copying something else as the triggered ability resolves, the token will be a copy of whatever that creature is a copy of.")
    }
}
