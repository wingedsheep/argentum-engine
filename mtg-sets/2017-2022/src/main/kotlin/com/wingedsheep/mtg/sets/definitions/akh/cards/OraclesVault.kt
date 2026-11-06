package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Oracle's Vault — {4} Artifact.
 *
 * The first ability is plain impulse draw plus a brick counter. The second exiles the top card and
 * pairs the play-from-exile permission with a "without paying its mana cost" grant on the same
 * collection (the Mind's Desire shape); a land exiled this way is simply played as the land drop.
 */
val OraclesVault = card("Oracle's Vault") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "{2}, {T}: Exile the top card of your library. Until end of turn, you may play that card. " +
        "Put a brick counter on this artifact.\n" +
        "{T}: Exile the top card of your library. Until end of turn, you may play that card without paying " +
        "its mana cost. Activate only if there are three or more brick counters on this artifact."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Patterns.Exile.impulse(1) then
            Effects.AddCounters(CounterType.BRICK, 1, EffectTarget.Self)
        description = "Exile the top card of your library. Until end of turn, you may play that card. " +
            "Put a brick counter on this artifact."
    }

    activatedAbility {
        cost = Costs.Tap
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.SourceCounterCountAtLeast(CounterType.BRICK, 3))
        )
        effect = Effects.Pipeline {
            val vaultTop = gather(CardSource.TopOfLibrary(count = 1, player = Player.You))
            exile(vaultTop)
            run(Effects.GrantMayPlayFromExile(vaultTop))
            run(Effects.GrantPlayWithoutPayingCost(vaultTop))
        }
        description = "Exile the top card of your library. Until end of turn, you may play that card " +
            "without paying its mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "234"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43fcc4a5-3ab0-4344-85af-d80aae293b75.jpg?1783936448"
    }
}
