package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cavalier of Flame — Core Set 2020 #125
 * {2}{R}{R}{R} · Creature — Elemental Knight 6/5
 *
 * {1}{R}: Creatures you control get +1/+0 and gain haste until end of turn.
 * When this creature enters, discard any number of cards, then draw that many cards.
 * When this creature dies, it deals X damage to each opponent and each planeswalker they control,
 * where X is the number of land cards in your graveyard.
 *
 * The enters trigger is the Into the Night rail: the discarded set is stored and the draw reads its
 * size, so discarding zero draws zero. X is counted as the dies trigger resolves.
 */
val CavalierOfFlame = card("Cavalier of Flame") {
    manaCost = "{2}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental Knight"
    power = 6
    toughness = 5
    oracleText = "{1}{R}: Creatures you control get +1/+0 and gain haste until end of turn.\n" +
        "When this creature enters, discard any number of cards, then draw that many cards.\n" +
        "When this creature dies, it deals X damage to each opponent and each planeswalker they control, " +
        "where X is the number of land cards in your graveyard."

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Patterns.Group.pumpAndGrantToAll(
            power = 1,
            toughness = 0,
            keyword = Keyword.HASTE,
            filter = GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val discarded = runStoringCollection { Patterns.Hand.discardAnyNumber(storeAs = it) }
            run(Effects.DrawCards(discarded.count))
        }
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val lands = DynamicAmounts.count(Player.You, Zone.GRAVEYARD, Filters.Land)
        effect = Effects.DealDamage(lands, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Patterns.Group.dealDamageToAll(lands, GroupFilter(GameObjectFilter.Planeswalker.opponentControls()))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "125"
        artist = "Wesley Burt"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c18c541-758b-4237-9961-9637e996e8a5.jpg?1783932984"
        ruling(
            "2019-07-12",
            "As Cavalier of Flame's enters-the-battlefield ability resolves, you may choose to discard zero " +
                "cards. You just won't draw any cards."
        )
    }
}
