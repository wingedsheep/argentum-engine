package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vraska's Fall — Phyrexia: All Will Be One #116
 * {2}{B}
 * Instant
 * Each opponent sacrifices a creature or planeswalker of their choice and gets a poison counter.
 *
 * The edict is one [Effects.Sacrifice] over `Player.EachOpponent` with the single Or-filter
 * [GameObjectFilter.CreatureOrPlaneswalker]. The poison half is per-opponent — `AddCounters`
 * resolves one player — so it runs under `ForEachPlayer(EachOpponent)`, which rebinds
 * `Player.You` to the visited opponent. An opponent with nothing to sacrifice still gets poisoned.
 */
val VraskasFall = card("Vraska's Fall") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Each opponent sacrifices a creature or planeswalker of their choice and gets a poison counter."

    spell {
        effect = Effects.Sacrifice(
            filter = GameObjectFilter.CreatureOrPlaneswalker,
            target = EffectTarget.PlayerRef(Player.EachOpponent),
        ) then Effects.ForEachPlayer(
            Player.EachOpponent,
            Effects.AddCounters(CounterType.POISON, 1, EffectTarget.PlayerRef(Player.You)),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "116"
        artist = "Dibujante Nocturno"
        flavorText = "The dull, creeping pain of phyresis curdled in Vraska's blood. She knew as soon as it took her, she would betray everything—even Jace."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/0173b7b4-2832-4943-ba45-8ba498d7a056.jpg?1783918037"
    }
}
