package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Wise Mothman — the mill payoff is a batch trigger ("one or more nonland cards are milled",
 * once per mill instruction, CR 603.2c). X is the captured nonland cards, read when the trigger is
 * put on the stack and its targets are chosen (CR 603.3d), so the target cap is fixed then.
 */
val TheWiseMothman = card("The Wise Mothman") {
    manaCost = "{1}{B}{G}{U}"
    typeLine = "Legendary Creature — Insect Mutant"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever The Wise Mothman enters or attacks, each player gets a rad counter.\n" +
        "Whenever one or more nonland cards are milled, put a +1/+1 counter on each of up to X target creatures, where X is the number of nonland cards milled this way."

    keywords(Keyword.FLYING)

    val eachPlayerGetsRad = Effects.ForEachPlayer(
        Player.Each,
        Effects.AddCounters(CounterType.RAD, 1, EffectTarget.Controller)
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = eachPlayerGetsRad
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = eachPlayerGetsRad
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.mills(GameObjectFilter.Nonland, batch = true)
        targets(
            TargetFilter.Creature,
            optional = true,
            dynamicMaxCount = DynamicAmounts.distinctEntitiesIn(CollectionSlot.TriggerCaptured)
        )
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "4"
        artist = "Sergei Leoluch Panin"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d6d6944-a364-41c2-b824-7a1bf6ad0d1e.jpg?1783912422"
        ruling(
            "2024-03-08",
            "If more than one player is instructed to mill at the same time (by the activated ability of Raul, Trouble Shooter, for example), as long as at least one nonland card is milled, The Wise Mothman's last ability will trigger just once."
        )
        ruling(
            "2024-03-08",
            "The cards are milled all at once, which means abilities that trigger \"whenever one or more nonland cards are milled\" will trigger exactly once as long as at least one nonland card was milled."
        )
    }
}
