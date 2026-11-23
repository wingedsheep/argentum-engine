package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Martial Glory
 * {R}{W}
 * Instant
 * Target creature gets +3/+0 until end of turn.
 * Target creature gets +0/+3 until end of turn.
 *
 * Two independent target requirements with no [com.wingedsheep.sdk.scripting.targets.TargetOther]
 * wrapper: separate instances of the word "target" may pick the same creature, which then gets
 * +3/+3.
 */
val MartialGlory = card("Martial Glory") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Instant"
    oracleText = "Target creature gets +3/+0 until end of turn.\nTarget creature gets +0/+3 until end of turn."

    spell {
        val attacker = target(TargetFilter.Creature)
        val defender = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(3, 0, attacker) then Effects.ModifyStats(0, 3, defender)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "175"
        artist = "Raymond Swanland"
        flavorText = "\"Yes, Boros tactics are unparalleled. But when your comrades are dying around you, " +
            "inner strength must carry the day.\"\n—Gideon Jura, to Aurelia"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/3690c96c-70a3-45e5-84d7-5c82809a8f45.jpg?1783940105"
    }
}
