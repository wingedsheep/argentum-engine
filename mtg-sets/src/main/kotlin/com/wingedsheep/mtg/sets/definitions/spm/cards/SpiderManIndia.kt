package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.webSlinging
import com.wingedsheep.sdk.model.Rarity

/**
 * Spider-Man India — Marvel's Spider-Man #151
 * {3}{G}{W} · Legendary Creature — Spider Human Hero · 4/4
 *
 * Web-slinging {1}{G}{W}
 * Pavitr's Sevā — Whenever you cast a creature spell, put a +1/+1 counter on target creature you
 * control. It gains flying until end of turn.
 */
val SpiderManIndia = card("Spider-Man India") {
    manaCost = "{3}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Spider Human Hero"
    power = 4
    toughness = 4
    oracleText = "Web-slinging {1}{G}{W} (You may cast this spell for {1}{G}{W} if you also return " +
        "a tapped creature you control to its owner's hand.)\n" +
        "Pavitr's Sevā — Whenever you cast a creature spell, put a +1/+1 counter on target creature " +
        "you control. It gains flying until end of turn."

    webSlinging("{1}{G}{W}")

    triggeredAbility {
        trigger = Triggers.YouCastCreature
        val t = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, t) then
            Effects.GrantKeyword(Keyword.FLYING, t)
        description = "Pavitr's Sevā — Whenever you cast a creature spell, put a +1/+1 counter on " +
            "target creature you control. It gains flying until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "151"
        artist = "Lie Setiawan"
        flavorText = "\"That's why I do what I do. For my people.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65b8af30-559b-43a9-8526-62c28c378339.jpg?1783905310"
    }
}
