package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lumbering Battlement (Ravnica Allegiance #15)
 * {4}{W} · Creature — Beast 4/5
 * Vigilance
 * When this creature enters, exile any number of other nontoken creatures you control until it
 * leaves the battlefield.
 * This creature gets +2/+2 for each card exiled with it.
 *
 * The enters trigger doesn't target: like Glorious Protector, it gathers your other nontoken
 * creatures on resolution and lets you pick any number. Each pick goes through
 * [Effects.ExileUntilLeaves], which links it to the Battlement (and does nothing if the Battlement
 * already left — first ruling). The bonus counts the cards in exile that are still linked to it
 * (`exiledWithSource()`), so a card that leaves exile stops counting (third ruling).
 */
val LumberingBattlement = card("Lumbering Battlement") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 5
    oracleText = "Vigilance\n" +
        "When this creature enters, exile any number of other nontoken creatures you control until it leaves the battlefield.\n" +
        "This creature gets +2/+2 for each card exiled with it."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val yours = gather(
                CardSource.ControlledPermanents(
                    player = Player.You,
                    filter = GameObjectFilter.Creature.nontoken().notSourceItself()
                )
            )
            val exiled = chooseAnyNumber(
                from = yours,
                chooser = Chooser.Controller,
                useTargetingUI = true,
                prompt = "Exile any number of other nontoken creatures you control until Lumbering Battlement leaves the battlefield"
            )
            run(Effects.ForEachInCollection(exiled, Effects.ExileUntilLeaves(EffectTarget.IterationEntity)))
        }
        description = "When this creature enters, exile any number of other nontoken creatures you " +
            "control until it leaves the battlefield."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    val bonus = DynamicAmounts.count(Player.Each, Zone.EXILE, GameObjectFilter.Any.exiledWithSource()) * 2
    staticAbility {
        ability = GrantDynamicStats(filter = GroupFilter.source(), powerBonus = bonus, toughnessBonus = bonus)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/2469bc93-57ca-4077-bda2-160b4160adad.jpg?1783933721"
        ruling(
            "2019-01-25",
            "If Lumbering Battlement leaves the battlefield before its triggered ability resolves, you can't exile any nontoken creatures."
        )
        ruling(
            "2019-01-25",
            "Auras attached to the exiled creatures will be put into their owners' graveyards. Any Equipment will become " +
                "unattached and remain on the battlefield. Any counters on the exiled creatures will cease to exist. When " +
                "the cards return to the battlefield, they will be new objects with no connection to the cards that were exiled."
        )
        ruling(
            "2019-01-25",
            "If a card you chose to exile leaves exile or somehow never moved to the exile zone in the first place, " +
                "Lumbering Battlement doesn't get +2/+2 for that card."
        )
    }
}
