package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Sweettooth Witch
 * {2}{B}
 * Creature — Human Warlock
 * 3/2
 *
 * When this creature enters, create a Food token.
 * {2}, Sacrifice a Food: Target player loses 2 life.
 *
 * "Sacrifice a Food" is any Food *artifact*, not just a Food token (2024-11-08 ruling), so the
 * cost filter matches on the Food subtype rather than on token-ness — Tough Cookie (an Artifact
 * Creature — Food Golem) is a legal sacrifice. The Witch's own Food is the usual fodder but the
 * ability doesn't care where the Food came from.
 */
val SweettoothWitch = card("Sweettooth Witch") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warlock"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, create a Food token. " +
        "(It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "{2}, Sacrifice a Food: Target player loses 2 life."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateFood()
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Sacrifice(GameObjectFilter.Any.withSubtype("Food"))
        )
        val t = target("target", TargetPlayer())
        effect = Effects.LoseLife(2, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Konstantin Porubov"
        flavorText = "\"Try some pie! Your brother helped make it, and he really poured his heart into it.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6bdb984-06f8-4bca-a943-17fe5db97682.jpg?1783915102"
        ruling(
            "2024-11-08",
            "If an effect refers to a Food, it means any Food artifact, not just a Food artifact token."
        )
        ruling(
            "2024-11-08",
            "You can't sacrifice a Food to pay multiple costs. For example, you can't sacrifice a Food " +
                "token to activate its own ability and also to activate this ability."
        )
    }
}
