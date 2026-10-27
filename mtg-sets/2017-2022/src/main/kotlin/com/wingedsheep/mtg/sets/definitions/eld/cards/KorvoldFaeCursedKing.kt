package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Korvold, Fae-Cursed King — Throne of Eldraine #329
 * {2}{B}{R}{G} · Legendary Creature — Dragon Noble · 4/4
 *
 * Flying
 * Whenever Korvold enters or attacks, sacrifice another permanent.
 * Whenever you sacrifice a permanent, put a +1/+1 counter on Korvold and draw a card.
 *
 * The sacrifice is mandatory but not a target; with no other permanent nothing happens (ruling).
 * The payoff is a per-permanent ANY-binding sacrifice trigger, so sacrificing Korvold itself still
 * draws (the counter has nowhere to go — ruling).
 */
val KorvoldFaeCursedKing = card("Korvold, Fae-Cursed King") {
    manaCost = "{2}{B}{R}{G}"
    colorIdentity = "BRG"
    typeLine = "Legendary Creature — Dragon Noble"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever Korvold enters or attacks, sacrifice another permanent.\n" +
        "Whenever you sacrifice a permanent, put a +1/+1 counter on Korvold and draw a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.or(Triggers.self.enters(), Triggers.self.attacks())
        effect = Effects.SacrificeOwn(GameObjectFilter.Permanent, excludeSource = true)
        description = "Whenever Korvold enters or attacks, sacrifice another permanent."
    }

    triggeredAbility {
        trigger = Triggers.you.sacrifices(GameObjectFilter.Permanent)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.DrawCards(1)
        description = "Whenever you sacrifice a permanent, put a +1/+1 counter on Korvold and draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "329"
        artist = "Wisnu Tan"
        flavorText = "Transformed at his own wedding, he promptly ate the banquet, the gifts, and the guests."
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92ea1575-eb64-43b5-b604-c6e23054f228.jpg?1783932546"
        ruling("2019-10-04", "If you somehow control no other permanents, you can attack with Korvold. You won't sacrifice anything, and there's no penalty for being unable to do so.")
        ruling("2019-10-04", "A legendary permanent that is put into a graveyard because of the \"legend rule\" isn't sacrificed.")
        ruling("2019-10-04", "If you sacrifice a permanent as part of a spell or activated ability's cost, Korvold's last ability will resolve before that spell or ability.")
        ruling("2019-10-04", "If you sacrifice Korvold, its last ability triggers. You won't put a +1/+1 counter on anything, but you'll draw a card.")
    }
}
