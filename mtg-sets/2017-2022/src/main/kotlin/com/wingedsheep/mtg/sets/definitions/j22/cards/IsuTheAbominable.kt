package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Isu the Abominable
 * {3}{U}{U}
 * Legendary Snow Creature — Yeti
 * 5/5
 *
 * You may look at the top card of your library any time.
 * You may play snow lands and cast snow spells from the top of your library.
 * Whenever another snow permanent you control enters, you may pay {G}, {W}, or {U}. If you do,
 * put a +1/+1 counter on Isu.
 *
 * Notes:
 *  - The play permission is [PlayLandsAndCastFilteredFromTopOfLibrary] narrowed on both axes:
 *    `landFilter` to snow lands, `spellFilter` to snow spells. Like Glarb it doesn't reveal the
 *    top card; [LookAtTopOfLibrary] lets only Isu's controller see it.
 *  - "Pay {G}, {W}, or {U}" is one payment of the player's chosen colour: a choice whose options
 *    are each colour's payment then the counter, with [FeasibilityCheck.CanPayMana] hiding a colour
 *    the player can't pay (so a chosen payment always goes through), and a "don't pay" option for
 *    the "you may".
 */
val IsuTheAbominable = card("Isu the Abominable") {
    manaCost = "{3}{U}{U}"
    typeLine = "Legendary Snow Creature — Yeti"
    power = 5
    toughness = 5
    oracleText = "You may look at the top card of your library any time.\n" +
        "You may play snow lands and cast snow spells from the top of your library.\n" +
        "Whenever another snow permanent you control enters, you may pay {G}, {W}, or {U}. " +
        "If you do, put a +1/+1 counter on Isu."

    // You may look at the top card of your library any time.
    staticAbility {
        ability = LookAtTopOfLibrary
    }

    // You may play snow lands and cast snow spells from the top of your library.
    staticAbility {
        ability = PlayLandsAndCastFilteredFromTopOfLibrary(
            spellFilter = GameObjectFilter.Any.snow(),
            landFilter = GameObjectFilter.Land.snow()
        )
    }

    // Whenever another snow permanent you control enters, you may pay {G}, {W}, or {U}.
    // If you do, put a +1/+1 counter on Isu.
    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Permanent.snow().youControl()).enters()
        effect = Effects.ChooseAction(
            choices = listOf("{G}", "{W}", "{U}").map { symbol ->
                val cost = ManaCost.parse(symbol)
                EffectChoice(
                    label = "Pay $symbol: put a +1/+1 counter on Isu",
                    effect = Effects.PayMana(symbol) then
                        Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                    feasibilityCheck = FeasibilityCheck.CanPayMana(cost)
                )
            } + EffectChoice(label = "Don't pay", effect = Effects.Nothing)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "12"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e1d50c3-3219-49cb-8f63-c1faff93215c.jpg?1783919194"

        ruling("2022-12-02", "You must pay all costs and follow all timing rules for cards played from the top of your library this way. For example, you may play a snow land this way only while the stack is empty during one of your own main phases, and only if you haven't played a land yet this turn.")
        ruling("2022-12-02", "The top card of your library is still in your library and not in your hand. You can't discard cards from the top of your library or activate abilities of cards on top of your library that normally work in your hand.")
    }
}
