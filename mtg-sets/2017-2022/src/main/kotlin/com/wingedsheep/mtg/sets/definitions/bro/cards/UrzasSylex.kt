package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Urza's Sylex
 * {3}
 * Legendary Artifact
 * {2}{W}{W}, {T}, Exile Urza's Sylex: Each player chooses six lands they control. Destroy all other
 * permanents. Activate only as a sorcery.
 * When Urza's Sylex is put into exile from the battlefield, you may pay {2}. If you do, search your
 * library for a planeswalker card, reveal it, put it into your hand, then shuffle.
 *
 * The wipe is Promise of Loyalty's shape: [forEachPlayerCollecting] walks the table in turn order,
 * each player keeping exactly six of their own lands ([SelectionMode.ChooseExactly] clamps to "all of
 * them" for a player with six or fewer), and the union of everyone's keeps outlives the loop. Every
 * permanent on the battlefield is then gathered once and the ones not kept are destroyed together —
 * a single simultaneous destroy, as "Destroy all other permanents" reads. The Sylex itself is already
 * in exile as part of the cost.
 *
 * The exile trigger is [Triggers.self] leaving to exile, so it fires off the activation's own cost
 * and — per the ruling — resolves before the activated ability, letting you fetch the planeswalker
 * before anyone picks lands.
 */
val UrzasSylex = card("Urza's Sylex") {
    manaCost = "{3}"
    colorIdentity = "W"
    typeLine = "Legendary Artifact"
    oracleText = "{2}{W}{W}, {T}, Exile Urza's Sylex: Each player chooses six lands they control. " +
        "Destroy all other permanents. Activate only as a sorcery.\n" +
        "When Urza's Sylex is put into exile from the battlefield, you may pay {2}. If you do, search " +
        "your library for a planeswalker card, reveal it, put it into your hand, then shuffle."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{W}{W}"),
            Costs.Tap,
            Costs.ExileSelf
        )
        effect = Effects.Pipeline {
            val (kept) = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                val lands = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Land))
                listOf(
                    chooseExactly(
                        6,
                        from = lands,
                        selectedLabel = "Keep",
                        prompt = "Choose six lands you control. All other permanents will be destroyed.",
                        useTargetingUI = true
                    )
                )
            }
            val everything = gather(GameObjectFilter.Permanent)
            destroy(exclude(everything, kept))
        }
        timing = TimingRule.SorcerySpeed
        description = "{2}{W}{W}, {T}, Exile Urza's Sylex: Each player chooses six lands they control. " +
            "Destroy all other permanents."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves(to = Zone.EXILE)
        effect = Effects.MayPay(
            ManaCost.parse("{2}"),
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Planeswalker,
                reveal = true
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "40"
        artist = "Joseph Meehan"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/3000d1c6-dbb3-4e65-b428-dbf167bb8797.jpg?1783920116"
        ruling(
            "2022-10-14",
            "The triggered ability of Urza's Sylex will resolve before the activated ability. This means " +
                "that you choose whether to pay {2} and search your library before any player chooses " +
                "lands. Notably, this means that the other players will get to know which planeswalker " +
                "card you searched for before choosing any lands."
        )
    }
}
