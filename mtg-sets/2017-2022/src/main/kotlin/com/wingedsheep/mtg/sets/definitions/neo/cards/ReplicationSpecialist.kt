package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Replication Specialist
 * {4}{U}
 * Creature — Moonfolk Artificer
 * 3/4
 * Flying
 * Whenever a nontoken artifact you control enters, you may pay {1}{U}. If you do, create a token
 * that's a copy of that artifact.
 *
 * "That artifact" is the triggering entity; if it has left the battlefield by resolution, the copy
 * uses its last-known characteristics (as Necroduality / Esoteric Duplicator do).
 */
val ReplicationSpecialist = card("Replication Specialist") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Moonfolk Artificer"
    power = 3
    toughness = 4
    oracleText = "Flying\nWhenever a nontoken artifact you control enters, you may pay {1}{U}. " +
        "If you do, create a token that's a copy of that artifact."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Artifact.nontoken().youControl()).enters()
        effect = Effects.MayPay(
            ManaCost.parse("{1}{U}"),
            Effects.CreateTokenCopyOfTarget(target = EffectTarget.TriggeringEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Cristi Balanescu"
        flavorText = "\"Step one is innovation. Step two is production. Sometimes step one is optional.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/7/475bb3b5-b648-460c-b0ea-89ecfe0ff226.jpg?1783923895"
    }
}
