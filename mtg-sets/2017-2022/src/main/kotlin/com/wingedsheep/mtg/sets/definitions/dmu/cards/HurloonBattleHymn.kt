package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

val HurloonBattleHymn = card("Hurloon Battle Hymn") {
    manaCost = "{2}{R}"
    colorIdentity = "WR"
    typeLine = "Instant"
    oracleText = "Kicker {W} (You may pay an additional {W} as you cast this spell.)\nHurloon Battle Hymn deals 4 damage to target creature or planeswalker. If this spell was kicked, you gain 4 life."

    keywordAbility(KeywordAbility.kicker("{W}"))

    spell {
        val victim = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(4, victim) then
            Effects.If(Conditions.WasKicked, Effects.GainLife(4))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "131"
        artist = "Dominik Mayer"
        flavorText = "The magical echoes of the Hurloon mountain range greatly increase the potency of spells based on song or speech."
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff252e80-c155-467b-8df3-9bddc5cc45c3.jpg?1783921315"

        ruling("2022-09-09", "If the target creature or planeswalker is an illegal target as Hurloon Battle Hymn tries to resolve, it won't do anything, even if it was kicked.")
    }
}
