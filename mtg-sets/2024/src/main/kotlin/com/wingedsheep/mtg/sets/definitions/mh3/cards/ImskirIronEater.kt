package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.div
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Imskir Iron-Eater — Modern Horizons 3 #189
 * {6}{B}{R} · Legendary Creature — Demon · 5/5
 *
 * Affinity for artifacts. The ETB counts artifacts once at resolution (ruling) and stores the
 * halved, floored number so the draw and the life loss read the same X. The activated ability is
 * Bosh, Iron Golem's shape: the sacrificed artifact's mana value is read off its last-known
 * information via [EffectTarget.SacrificedAsCost].
 */
val ImskirIronEater = card("Imskir Iron-Eater") {
    manaCost = "{6}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Demon"
    power = 5
    toughness = 5
    oracleText = "Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)\n" +
        "When Imskir enters, you draw X cards and you lose X life, where X is half the number of " +
        "artifacts you control, rounded down.\n" +
        "{3}{R}, Sacrifice an artifact: Imskir deals damage equal to the sacrificed artifact's mana " +
        "value to any target."

    keywordAbility(KeywordAbility.Affinity(CardType.ARTIFACT))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val x = storeNumber(
                DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count() / 2
            )
            run(Effects.DrawCards(x.amount))
            run(Effects.LoseLife(x.amount, EffectTarget.Controller))
        }
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{R}"), Costs.Sacrifice(GameObjectFilter.Artifact))
        val t = target(Targets.Any)
        effect = Effects.DealDamage(
            DynamicAmounts.manaValueOf(EffectTarget.SacrificedAsCost(0)),
            t
        )
        description = "{3}{R}, Sacrifice an artifact: Imskir deals damage equal to the sacrificed " +
            "artifact's mana value to any target."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "189"
        artist = "Xavier Ribeiro"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/541209fa-38b6-4d61-a72d-26bfb6ad5ead.jpg?1783911250"
        ruling("2024-06-07", "The value of X is calculated only once, as Imskir Iron-Eater's triggered ability resolves.")
        ruling("2024-06-07", "Use the mana value of the sacrificed artifact as it last existed on the battlefield to determine how much damage is dealt.")
        ruling("2024-06-07", "If an artifact on the battlefield has {X} in its mana cost, X is 0 when determining its mana value.")
    }
}
