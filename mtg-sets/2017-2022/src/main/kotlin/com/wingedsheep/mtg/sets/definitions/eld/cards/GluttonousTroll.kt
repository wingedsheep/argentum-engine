package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gluttonous Troll
 * {2}{B}{G}
 * Creature — Troll
 * 3/3
 *
 * Trample
 * When this creature enters, create a number of Food tokens equal to the number of opponents you have.
 * {1}{G}, Sacrifice another nonland permanent: This creature gets +2/+2 until end of turn.
 *
 * The opponent count is read at resolution through `PlayerCount(EachOpponent)`, which reads
 * `GameState.getOpponents` and so skips opponents who have left the game (2019-10-04 ruling).
 */
val GluttonousTroll = card("Gluttonous Troll") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Troll"
    power = 3
    toughness = 3
    oracleText = "Trample\n" +
        "When this creature enters, create a number of Food tokens equal to the number of opponents you have. " +
        "(Food tokens are artifacts with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "{1}{G}, Sacrifice another nonland permanent: This creature gets +2/+2 until end of turn."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateFood(DynamicAmounts.playerCount())
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{G}"),
            Costs.SacrificeAnother(GameObjectFilter.NonlandPermanent),
        )
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "327"
        artist = "Joe Slucher"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/867e245b-6153-479c-9e09-682afb017ff3.jpg?1783932548"
        ruling("2019-10-04", "You can sacrifice a Food token created by Gluttonous Troll's triggered ability to pay for the cost of its activated ability.")
        ruling("2019-10-04", "In a multiplayer game, opponents who have left the game aren't counted by Gluttonous Troll's triggered ability.")
    }
}
