package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sutina, Speaker of the Tajuru — Foundations Jumpstart #56 (canonical printing)
 * {2}{G} · Legendary Creature — Elf Scout · 2/2
 *
 * When Sutina enters, search your library for a basic land card, put it onto the battlefield
 * tapped, then shuffle.
 * Whenever Sutina attacks, you may return a land you control to its owner's hand. When you do,
 * put a +1/+1 counter on target creature.
 *
 * The attack trigger doesn't target; the land is chosen as it resolves (Pyroclastic Hellion's
 * gather → choose → toHand shape). Returning it is the optional action of a reflexive trigger, so
 * the counter ability goes on the stack — and chooses its target, any creature — only once a land
 * actually went back. With no land to return, the action is infeasible and nothing triggers.
 */
val SutinaSpeakerOfTheTajuru = card("Sutina, Speaker of the Tajuru") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elf Scout"
    power = 2
    toughness = 2
    oracleText = "When Sutina enters, search your library for a basic land card, put it onto the " +
        "battlefield tapped, then shuffle.\n" +
        "Whenever Sutina attacks, you may return a land you control to its owner's hand. When you do, " +
        "put a +1/+1 counter on target creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ReflexiveTrigger(
            action = Effects.Pipeline {
                val lands = gather(CardSource.BattlefieldMatching(filter = GameObjectFilter.Land, player = Player.You))
                val returned = chooseExactly(
                    1,
                    from = lands,
                    prompt = "Return a land you control to its owner's hand",
                    useTargetingUI = true
                )
                toHand(returned)
            },
            optional = true,
            descriptionOverride = "You may return a land you control to its owner's hand. When you do, " +
                "put a +1/+1 counter on target creature."
        ) {
            val creature = target(TargetFilter.Creature)
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "56"
        artist = "Canata Katana"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b794170-4ffd-4628-ba5a-b9293bb364dc.jpg?1783908852"
        ruling(
            "2024-11-08",
            "You don't choose a target for Sutina's last ability at the time it triggers. Rather, a second " +
                "\"reflexive\" ability triggers when you return a land you control to its owner's hand this " +
                "way. You choose a target for that ability as it goes on the stack. Each player may respond " +
                "to this triggered ability as normal.",
        )
    }
}
