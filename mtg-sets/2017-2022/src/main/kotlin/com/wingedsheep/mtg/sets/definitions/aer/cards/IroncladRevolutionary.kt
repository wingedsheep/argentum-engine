package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Ironclad Revolutionary — Aether Revolt #65
 * {4}{B}{B} · Creature — Aetherborn Artificer · Uncommon · 4/4
 *
 * When this creature enters, you may sacrifice an artifact. If you do, put two +1/+1 counters on
 * this creature and each opponent loses 2 life.
 *
 * Kuldotha Flamefiend's shape: the sacrifice is a resolution-time [Effects.MayPay] cost (one
 * artifact, once), and the counters and life loss only follow an actual sacrifice.
 */
val IroncladRevolutionary = card("Ironclad Revolutionary") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Aetherborn Artificer"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, you may sacrifice an artifact. If you do, put two +1/+1 " +
        "counters on this creature and each opponent loses 2 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.MayPay(
            cost = Effects.SacrificeOwn(filter = GameObjectFilter.Artifact),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self) then
                Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)),
            descriptionOverride = "You may sacrifice an artifact. If you do, put two +1/+1 counters on " +
                "Ironclad Revolutionary and each opponent loses 2 life."
        )
        description = "When Ironclad Revolutionary enters, you may sacrifice an artifact. If you do, put two " +
            "+1/+1 counters on Ironclad Revolutionary and each opponent loses 2 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Eric Deschamps"
        flavorText = "As the rebellion spread, renegade ingenuity outpaced the Consulate's greatest fears."
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43a5535b-b1d1-4648-aa72-4f64e9fcd95d.jpg?1783936760"
        ruling(
            "2017-02-09",
            "You choose whether to sacrifice an artifact or not as Ironclad Revolutionary's ability resolves. " +
                "No player may take actions between the time you choose whether to sacrifice an artifact and, if " +
                "you do so, the time that counters are added and life is lost."
        )
        ruling(
            "2017-02-09",
            "You can sacrifice an artifact only once when Ironclad Revolutionary's triggered ability resolves. " +
                "You can't sacrifice more to get more counters or more life loss."
        )
    }
}
