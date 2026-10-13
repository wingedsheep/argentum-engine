package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.divRoundedUp
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Legate Lanius, Caesar's Ace
 * {2}{B}{R}
 * Legendary Creature — Human Soldier
 * 2/2
 *
 * Decimate — When Legate Lanius enters, each opponent sacrifices a tenth of the creatures they
 * control of their choice, rounded up.
 * Whenever an opponent sacrifices a creature, put a +1/+1 counter on Legate Lanius.
 *
 * "A tenth … rounded up" is measured per opponent, so the sacrifice runs inside
 * `ForEachPlayer(EachOpponent)`, which rebinds `Player.You` to the opponent being processed — the
 * Zodiark, Umbral God shape with `divRoundedUp 10` (Rush of Dread's rounding). The counter trigger
 * is the per-permanent `Triggers.anOpponent.sacrifices`, so Decimate itself grows Lanius by one
 * counter for each creature it makes an opponent sacrifice.
 */
val LegateLaniusCaesarsAce = card("Legate Lanius, Caesar's Ace") {
    manaCost = "{2}{B}{R}"
    typeLine = "Legendary Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Decimate — When Legate Lanius enters, each opponent sacrifices a tenth of the creatures " +
        "they control of their choice, rounded up.\n" +
        "Whenever an opponent sacrifices a creature, put a +1/+1 counter on Legate Lanius."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachPlayer(
            players = Player.EachOpponent,
            effect = Effects.Sacrifice(
                filter = GameObjectFilter.Creature,
                count = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).count() divRoundedUp 10,
                target = EffectTarget.PlayerRef(Player.You)
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.sacrifices(GameObjectFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "107"
        artist = "Bruno Biazotto"
        flavorText = "\"Victory shall be ours, it shall be swift, and it will be honest, purchased with blood.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f14eb369-b8e3-4abf-9e77-79d1fe5b426f.jpg?1783912387"
        ruling("2024-03-08", "For example, when Legate Lanius's first ability resolves, an opponent who controls between one and ten creatures will sacrifice one creature. An opponent who controls between eleven and twenty creatures will sacrifice two creatures, and so on.")
    }
}
