package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Specter of Mortality
 * {3}{B}{B}
 * Creature — Specter
 * 3/3
 *
 * Flying
 * When this creature enters, you may exile one or more creature cards from your graveyard. When you
 * do, each other creature gets -X/-X until end of turn, where X is the number of cards exiled this
 * way.
 *
 * A Gather → Select → Move pipeline: gather the creature cards in your graveyard, let the controller
 * take any number of them (0 is the "may" decline — a `chooseAnyNumber` with an empty selection),
 * exile the picks, then shrink the board by however many actually moved.
 *
 * Two details the pipeline shape buys:
 *  - `moveTracked` is what X reads, not the selection. "The number of cards exiled this way" counts
 *    cards that really left the graveyard, so a card that somehow moved away between selection and
 *    resolution doesn't inflate the debuff.
 *  - The `ifNotEmpty` gate is the "When you do" clause. Declining exiles nothing and must apply no
 *    -0/-0 modification at all, rather than stacking an inert floating effect on every creature.
 *
 * `excludeSelf` on the [GroupFilter] is the printed "each **other** creature": the Specter that just
 * entered spares itself, and everything else — including your own creatures — shrinks.
 */
val SpecterOfMortality = card("Specter of Mortality") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Specter"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, you may exile one or more creature cards from your graveyard. " +
        "When you do, each other creature gets -X/-X until end of turn, where X is the number of " +
        "cards exiled this way."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Pipeline(
            descriptionOverride = "You may exile one or more creature cards from your graveyard. " +
                "When you do, each other creature gets -X/-X until end of turn, where X is the " +
                "number of cards exiled this way."
        ) {
            val graveyardCreatures = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.You,
                    filter = GameObjectFilter.Creature
                )
            )
            val chosen = chooseAnyNumber(
                from = graveyardCreatures,
                prompt = "Exile any number of creature cards from your graveyard",
                selectedLabel = "Exile",
                remainderLabel = "Leave in graveyard"
            )
            val exiled = moveTracked(
                from = chosen,
                destination = CardDestination.ToZone(Zone.EXILE),
                name = "exiledCreatures"
            )
            ifNotEmpty(exiled) {
                val exiledCount = DynamicAmount.VariableReference("exiledCreatures_count")
                run(
                    Patterns.Group.modifyStatsForAll(
                        power = DynamicAmount.Multiply(exiledCount, -1),
                        toughness = DynamicAmount.Multiply(exiledCount, -1),
                        filter = GroupFilter(GameObjectFilter.Creature, excludeSelf = true)
                    )
                )
            }
        }
        description = "When this creature enters, you may exile one or more creature cards from " +
            "your graveyard. When you do, each other creature gets -X/-X until end of turn, where " +
            "X is the number of cards exiled this way."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "107"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e4c00b5-f6d6-4fbd-828f-ad30321f2cd9.jpg?1783915102"
    }
}
