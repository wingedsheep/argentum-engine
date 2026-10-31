package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Syr Carah, the Bold (ELD #145)
 * {3}{R}{R} · Legendary Creature — Human Knight · 3/3
 *
 * Whenever Syr Carah or an instant or sorcery spell you control deals damage to a player, exile the
 * top card of your library. You may play that card this turn.
 * {T}: Syr Carah deals 1 damage to any target.
 *
 * "Syr Carah or an instant or sorcery spell you control" is two trigger subjects with different
 * bindings (SELF vs ANY), so it is written as two triggered abilities sharing one impulse effect.
 * A damage event has exactly one source and Syr Carah is never an instant or sorcery spell, so the
 * two can never both fire for the same damage — together they are exactly the printed trigger. It
 * fires once per damaged player (ruling 2019-10-04), as `dealsDamage` is per recipient.
 */
private val exileTopMayPlay: Effect = Effects.Pipeline {
    val exiledCard = gather(CardSource.TopOfLibrary(1))
    exile(exiledCard)
    run(Effects.GrantMayPlayFromExile(exiledCard))
}

val SyrCarahTheBold = card("Syr Carah, the Bold") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Knight"
    oracleText = "Whenever Syr Carah or an instant or sorcery spell you control deals damage to a player, " +
        "exile the top card of your library. You may play that card this turn.\n" +
        "{T}: Syr Carah deals 1 damage to any target."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.dealsDamage(Recipient.AnyPlayer)
        effect = exileTopMayPlay
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.InstantOrSorcery.youControl()).dealsDamage(Recipient.AnyPlayer)
        effect = exileTopMayPlay
    }

    activatedAbility {
        cost = Costs.Tap
        val t = target(Targets.Any)
        effect = Effects.DealDamage(1, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "145"
        artist = "Wisnu Tan"
        flavorText = "\"Duty and honor are reasons enough, but beating you will also be a pleasure.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/8/080a249a-df47-4769-bb63-0d8ab3f2467c.jpg?1783932615"
        ruling("2019-10-04", "If Syr Carah or an instant or sorcery spell you control deals damage to two or more players at the same time, Syr Carah's first ability triggers for each of those players.")
        ruling("2019-10-04", "Syr Carah's first ability triggers even if Syr Carah is dealt lethal damage at the same time that it or an instant or sorcery spell you control deals damage to a player.")
        ruling("2019-10-04", "Syr Carah doesn't change when you can cast the exiled card. For example, if you exile a sorcery card, you can cast it only during your main phase when the stack is empty. If you exile a land card, you can play it only during your main phase and only if you have an available land play remaining.")
        ruling("2019-10-04", "Playing an exiled card causes it to leave exile. You can't play it multiple times.")
        ruling("2019-10-04", "If you don't play the exiled card, it remains in exile.")
    }
}
