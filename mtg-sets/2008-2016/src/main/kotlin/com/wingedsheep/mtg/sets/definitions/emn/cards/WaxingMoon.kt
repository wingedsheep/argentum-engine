package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Waxing Moon
 * {1}{G}
 * Instant
 *
 * Transform up to one target Werewolf you control. Creatures you control gain trample until end
 * of turn.
 *
 * "Up to one" is an optional target slot (the Crawl from the Cellar shape): with no Werewolf
 * chosen the transform step is skipped and the team still gains trample. The target is a Werewolf
 * *permanent* — Oracle's bare tribal noun. A single-faced Werewolf, or a daybound/nightbound one,
 * doesn't transform (the transform executor no-ops on both). If the chosen Werewolf becomes an
 * illegal target the spell doesn't resolve at all, so no trample — the ruling below, which the
 * standard all-targets-illegal fizzle already gives. The trample grant gathers the group after the
 * transform, so the flipped Werewolf is included.
 */
val WaxingMoon = card("Waxing Moon") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Transform up to one target Werewolf you control. Creatures you control gain trample " +
        "until end of turn."

    spell {
        val werewolf = target(
            TargetFilter(GameObjectFilter.Permanent.withSubtype(Subtype.WEREWOLF).youControl()),
            optional = true
        )

        effect = Effects.Transform(werewolf) then
            Patterns.Group.grantKeywordToAll(Keyword.TRAMPLE, Filters.Group.creaturesYouControl)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "177"
        artist = "Adam Paquette"
        flavorText = "\"Show them what it means to be the source of nightmares!\"\n—Ulrich, Krallenhorde alpha"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/617f9f3a-61a1-4c73-8efe-3d74fe362901.jpg?1783937438"
        ruling(
            "2016-07-13",
            "You may cast Waxing Moon without targeting a Werewolf. If you do choose a target, and that " +
                "target becomes illegal, Waxing Moon doesn't resolve, and your creatures won't gain trample."
        )
    }
}
