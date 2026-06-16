package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Railway Brawler
 * {3}{G}{G}
 * Creature — Rhino Warrior
 * 5/5
 *
 * Reach, trample
 * Whenever another creature you control enters, put X +1/+1 counters on it, where X is its power.
 * Plot {3}{G}
 *
 * The ETB trigger fires for any OTHER creature you control entering ([Triggers.OtherCreatureEnters],
 * OTHER binding) and addresses that entering creature via [EffectTarget.TriggeringEntity]. The counter
 * count "X is its power" reads the entering creature's power through
 * [DynamicAmount.EntityProperty]([EntityReference.Triggering], [EntityNumericProperty.Power]) — the same
 * triggering-creature property idiom Terror of the Peaks uses. A 0- or negative-power creature yields
 * 0 counters (the dynamic-counter executor no-ops on amounts <= 0), matching CR intuition.
 *
 * Plot is the standard OTJ special action via [KeywordAbility.plot].
 */
val RailwayBrawler = card("Railway Brawler") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Rhino Warrior"
    power = 5
    toughness = 5
    oracleText = "Reach, trample\n" +
        "Whenever another creature you control enters, put X +1/+1 counters on it, where X is its power.\n" +
        "Plot {3}{G} (You may pay {3}{G} and exile this card from your hand. Cast it as a sorcery on a " +
        "later turn without paying its mana cost. Plot only as a sorcery.)"

    keywords(Keyword.REACH, Keyword.TRAMPLE)

    keywordAbility(KeywordAbility.plot("{3}{G}"))

    triggeredAbility {
        trigger = Triggers.OtherCreatureEnters
        effect = Effects.AddDynamicCounters(
            counterType = Counters.PLUS_ONE_PLUS_ONE,
            amount = DynamicAmount.EntityProperty(EntityReference.Triggering, EntityNumericProperty.Power),
            target = EffectTarget.TriggeringEntity
        )
        description = "Whenever another creature you control enters, put X +1/+1 counters on it, " +
            "where X is its power."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "175"
        artist = "Kevin Sidharta"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9ec1f76f-f21d-4f06-8c02-be6745183348.jpg?1712355970"
    }
}
