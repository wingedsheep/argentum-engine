package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ao, the Dawn Sky — Kamigawa: Neon Dynasty #2 (canonical printing)
 * {3}{W}{W} · Legendary Creature — Dragon Spirit · 5/4
 *
 * Flying, vigilance
 * When Ao dies, choose one —
 * • Look at the top seven cards of your library. Put any number of nonland permanent cards with
 *   total mana value 4 or less from among them onto the battlefield. Put the rest on the bottom of
 *   your library in a random order.
 * • Put two +1/+1 counters on each permanent you control that's a creature or Vehicle.
 *
 * Atsushi, the Blazing Sky's modal dies trigger (mode locked in as it goes on the stack). Mode one
 * is Michelangelo's Technique's look → choose-split → battlefield / random-bottom pipeline, with
 * "any number" ([chooseAnyNumberSplit]) capped by [SelectionRestriction.TotalManaValueAtMost].
 * Mode two is Rescue Retriever's [Effects.ForEachInGroup] counter placement over
 * [GameObjectFilter.CreatureOrVehicle], so an uncrewed Vehicle gets the counters too.
 */
val AoTheDawnSky = card("Ao, the Dawn Sky") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Dragon Spirit"
    power = 5
    toughness = 4
    oracleText = "Flying, vigilance\n" +
        "When Ao dies, choose one —\n" +
        "• Look at the top seven cards of your library. Put any number of nonland permanent cards with " +
        "total mana value 4 or less from among them onto the battlefield. Put the rest on the bottom of " +
        "your library in a random order.\n" +
        "• Put two +1/+1 counters on each permanent you control that's a creature or Vehicle."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.Pipeline {
                    val looked = gather(CardSource.TopOfLibrary(count = 7, player = Player.You))
                    val (toBattlefield, toBottom) = chooseAnyNumberSplit(
                        from = looked,
                        filter = GameObjectFilter.NonlandPermanent,
                        restrictions = listOf(SelectionRestriction.TotalManaValueAtMost(4)),
                        showAllCards = true,
                        prompt = "Put any number of nonland permanent cards with total mana value 4 or less " +
                            "onto the battlefield",
                        selectedLabel = "Put onto the battlefield",
                        remainderLabel = "Put on the bottom of your library"
                    )
                    move(toBattlefield, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
                    toLibraryBottom(toBottom, order = CardOrder.Random)
                },
                "Look at the top seven cards of your library. Put any number of nonland permanent cards " +
                    "with total mana value 4 or less from among them onto the battlefield. Put the rest " +
                    "on the bottom of your library in a random order.",
            ),
            Mode.noTarget(
                Effects.ForEachInGroup(
                    filter = GroupFilter(baseFilter = GameObjectFilter.CreatureOrVehicle.youControl()),
                    effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.IterationEntity)
                ),
                "Put two +1/+1 counters on each permanent you control that's a creature or Vehicle.",
            ),
        )
        description = "When Ao dies, choose one — • Look at the top seven cards of your library. Put any " +
            "number of nonland permanent cards with total mana value 4 or less from among them onto the " +
            "battlefield. Put the rest on the bottom of your library in a random order. • Put two +1/+1 " +
            "counters on each permanent you control that's a creature or Vehicle."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "2"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d2c4063-7322-4041-a870-b95598a03e29.jpg?1783923928"
    }
}
