package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * General Tazri
 * {4}{W}
 * Legendary Creature — Human Ally
 * 3/4
 *
 * When General Tazri enters, you may search your library for an Ally creature card, reveal it,
 * put it into your hand, then shuffle.
 * {W}{U}{B}{R}{G}: Ally creatures you control get +X/+X until end of turn, where X is the number
 * of colors among those creatures.
 *
 * The pump is the Jazal Goldmane shape: [Effects.ForEachInGroup] locks the affected Allies as the
 * ability resolves (later Allies don't get the bonus), and [Effects.ModifyStats] evaluates X once
 * per creature at resolution, so the bonus doesn't change afterwards. X is the generic
 * `AggregateBattlefield(DISTINCT_COLORS)` count over the same group (via
 * [DynamicAmounts.colorsAmongPermanents]), which reads projected colors; colorless isn't a color.
 * Pumping doesn't change colors, so every Ally in the group sees the same X.
 */
private val allyCreaturesYouControl = GameObjectFilter.Creature.withSubtype(Subtype.ALLY)

val GeneralTazri = card("General Tazri") {
    manaCost = "{4}{W}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Human Ally"
    power = 3
    toughness = 4
    oracleText = "When General Tazri enters, you may search your library for an Ally creature card, " +
        "reveal it, put it into your hand, then shuffle.\n" +
        "{W}{U}{B}{R}{G}: Ally creatures you control get +X/+X until end of turn, where X is the " +
        "number of colors among those creatures."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Patterns.Library.searchLibrary(
                filter = allyCreaturesYouControl,
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true
            )
        )
    }

    activatedAbility {
        cost = Costs.Mana("{W}{U}{B}{R}{G}")
        val colorsAmongAllies = DynamicAmounts.colorsAmongPermanents(Player.You, allyCreaturesYouControl)
        effect = Effects.ForEachInGroup(
            GroupFilter(allyCreaturesYouControl.youControl()),
            Effects.ModifyStats(colorsAmongAllies, colorsAmongAllies, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "19"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34e9aa86-1a31-4c0f-928d-923f066286b6.jpg?1783937927"

        ruling("2016-01-22", "The last ability checks only Ally creatures you control as the ability resolves. The value of X will be between 0 and 5. Colorless is not a color.")
        ruling("2016-01-22", "Once the last ability resolves, the bonus given doesn't change, even if the number of colors among your Allies does.")
    }
}
