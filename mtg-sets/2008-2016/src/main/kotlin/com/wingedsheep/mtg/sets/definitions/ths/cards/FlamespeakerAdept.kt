package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Flamespeaker Adept
 * {2}{R}
 * Creature — Human Shaman
 * 2/3
 *
 * Whenever you scry, this creature gets +2/+0 and gains first strike until end of turn.
 *
 * Triggers once per scry instruction, however many cards were looked at.
 */
val FlamespeakerAdept = card("Flamespeaker Adept") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    oracleText = "Whenever you scry, this creature gets +2/+0 and gains first strike until end of turn."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.you.scries()
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Lucas Graciano"
        flavorText = "\"I see your future, mantled in ash.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b84b90b6-71e2-429e-9853-50bcbd6538db.jpg?1783939763"
        ruling(
            "2013-09-15",
            "Flamespeaker Adept's ability triggers once for each scry instruction, no matter how many cards you're scrying."
        )
    }
}
