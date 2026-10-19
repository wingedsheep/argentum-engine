package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Hidetsugu, Devouring Chaos
 * {3}{B}
 * Legendary Creature — Ogre Demon
 * 4/4
 * {B}, Sacrifice a creature: Scry 2.
 * {2}{R}, {T}: Exile the top card of your library. You may play that card this turn. When you
 * exile a nonland card this way, Hidetsugu deals damage equal to the exiled card's mana value to
 * any target.
 *
 * "When you exile a nonland card this way" is a CR 603.12 reflexive trigger — a separate stack
 * object whose "any target" is chosen as it is put on the stack, after the card is already
 * exiled and visible. It is gated on the exiled card being nonland with the Ogre Battlecaster
 * shape: `If(CollectionContainsMatch(exiled, Nonland), ReflexiveTrigger(Nothing, …))`, so a land
 * (or an empty library) exiles/plays normally but never triggers. The amount reads the exiled
 * card's printed mana value off the pipeline collection the reflexive trigger carries forward
 * (`StoredCardManaValue`), so it is unaffected by the card being played in response.
 */
val HidetsuguDevouringChaos = card("Hidetsugu, Devouring Chaos") {
    manaCost = "{3}{B}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Ogre Demon"
    power = 4
    toughness = 4
    oracleText = "{B}, Sacrifice a creature: Scry 2.\n" +
        "{2}{R}, {T}: Exile the top card of your library. You may play that card this turn. " +
        "When you exile a nonland card this way, Hidetsugu deals damage equal to the exiled " +
        "card's mana value to any target."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.Sacrifice(GameObjectFilter.Creature))
        effect = Effects.Scry(2)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.Tap)
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(1), name = "hidetsuguExiled")
            exile(exiled)
            run(Effects.GrantMayPlayFromExile(exiled, MayPlayExpiry.EndOfTurn))
            run(
                Effects.If(
                    condition = Conditions.CollectionContainsMatch(exiled, GameObjectFilter.Nonland),
                    then = Effects.ReflexiveTrigger(
                        action = Effects.Nothing,
                        optional = false,
                        descriptionOverride = "Hidetsugu deals damage equal to the exiled card's " +
                            "mana value to any target."
                    ) {
                        val anyTarget = target(Targets.Any)
                        effect = Effects.DealDamage(DynamicAmounts.manaValueOf(exiled), anyTarget)
                    }
                )
            )
        }
        description = "Exile the top card of your library. You may play that card this turn. " +
            "When you exile a nonland card this way, Hidetsugu deals damage equal to the exiled " +
            "card's mana value to any target."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Isis"
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f4142d3-f7ca-4613-a6e5-528cf1f22383.jpg?1783923885"
        ruling("2022-02-18", "You must still pay all costs and follow all normal timing rules when playing a card this way. For example, you may play a land this way only during one of your own main phases while the stack is empty, and only if you haven't previously played a land this turn.")
    }
}
